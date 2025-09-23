package ru.meschanov.orderservice.service;

import ru.meschanov.orderservice.dto.CreateOrderRequest;
import ru.meschanov.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getById(Long id);
    List<OrderResponse> getByCustomerId(Long customerId);
}
