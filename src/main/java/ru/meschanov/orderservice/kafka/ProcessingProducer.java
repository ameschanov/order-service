package ru.meschanov.orderservice.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessingProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "order-processing";

    public void publish(String key, String payload) {
        kafkaTemplate.send(TOPIC, key, payload);
    }
}
