package ru.meschanov.orderservice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.meschanov.orderservice.dto.CreateOrderRequest;
import ru.meschanov.orderservice.dto.OrderResponse;
import ru.meschanov.orderservice.enumeration.OrderStatus;
import ru.meschanov.orderservice.kafka.OrderProducer;
import ru.meschanov.orderservice.model.Order;
import ru.meschanov.orderservice.model.OrderItem;
import ru.meschanov.orderservice.repository.OrderRepository;
import ru.meschanov.orderservice.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final OrderProducer orderProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String CUSTOMER_SERVICE_URL = "http://customer-service:8082/customers";
    private final String CATALOG_SERVICE_URL = "http://catalog-service:8083/catalog/product";

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1) Есть ли такой пользователь в системе
        try {
            restTemplate.getForObject(CUSTOMER_SERVICE_URL + "/" + request.getCustomerId(), Object.class);
        } catch (RestClientException ex) {
            throw new IllegalArgumentException("Customer not found: " + request.getCustomerId());
        }

        // 2) Создание заказа из каталога
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.NEW);

        double total = 0.0;
        for (CreateOrderRequest.Item it : request.getItems()) {
            String url = CATALOG_SERVICE_URL + "/" + it.getProductId();
            ProductInfo product;
            try {
                product = restTemplate.getForObject(url, ProductInfo.class);
            } catch (RestClientException e) {
                throw new IllegalArgumentException("Product not found: " + it.getProductId());
            }
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + it.getProductId());
            }
            if (product.getQuantityStock() < it.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + it.getProductId());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(it.getProductId());
            orderItem.setQuantity(it.getQuantity());
            orderItem.setPrice(BigDecimal.valueOf(product.getPrice()));
            order.addItem(orderItem);

            total += product.getPrice() * it.getQuantity();
        }

        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);


        try {
            String json = objectMapper.writeValueAsString(new OrderCreatedEvent(savedOrder.getId(), savedOrder.getCustomerId()));
            orderProducer.publish(savedOrder.getId().toString(), json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return toResponse(savedOrder);
    }

    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getById(Long id) {
        Order o = orderRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Order not found"));
        return toResponse(o);
    }

    @Override
    public java.util.List<OrderResponse> getByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .customerId(o.getCustomerId())
                .createdAt(o.getCreatedAt())
                .status(o.getStatus().name())
                .totalAmount(o.getTotalAmount())
                .items(o.getItems().stream().map(i ->
                        OrderResponse.Item.builder()
                                .productId(i.getProductId())
                                .quantity(i.getQuantity())
                                .price(i.getPrice().doubleValue())
                                .build()).collect(Collectors.toList()))
                .build();
    }

    public static record OrderCreatedEvent(Long orderId, Long customerId) {}
    public static class ProductInfo {
        private Long id;
        private Double price;
        private Integer quantityStock;
        public Long getId(){return id;}
        public void setId(Long id){this.id=id;}
        public Double getPrice(){return price;}
        public void setPrice(Double price){this.price=price;}
        public Integer getQuantityStock(){return quantityStock;}
        public void setQuantityStock(Integer quantityStock){this.quantityStock = quantityStock;}
    }
}
