package ru.meschanov.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String TOPIC = "orders";

    public void publish(String key, String payload) {
        kafkaTemplate.send(TOPIC, key, payload);
    }
}
