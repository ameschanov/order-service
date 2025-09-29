package ru.meschanov.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.meschanov.orderservice.dto.OrderCreatedEvent;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {
    Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private static final String TOPIC = "orders";

    public void publish(OrderCreatedEvent event) {
        log.info("Sending event to Kafka topic '{}': {}", TOPIC, event);
        kafkaTemplate.send(TOPIC, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send event to Kafka", ex);
                    } else {
                        log.info("Event sent successfully, offset = {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
