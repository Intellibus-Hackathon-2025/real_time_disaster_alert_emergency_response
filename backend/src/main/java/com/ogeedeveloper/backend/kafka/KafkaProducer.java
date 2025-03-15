package com.ogeedeveloper.backend.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducer {
    private KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "test"; // To be change for the topic name

    public void sendMessage(String message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
