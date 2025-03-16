package com.ogeedeveloper.backend.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogeedeveloper.backend.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaProducer {
    private KafkaTemplate<String, String> kafkaTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

//    Send out alert
    public void sendAlert(String topic, Alert alert) {
        try {
            String alertJson = objectMapper.writeValueAsString(alert);
            kafkaTemplate.send(topic, String.valueOf(alert.getId()), alertJson);
            log.info("Alert sent to topic {}: {}", topic, alert.getDetail());
        } catch (JsonProcessingException e) {
            log.error("Error serializing alert to JSON", e);

        }
    }

//    Send message
public void sendMessage(String topic, String key, String message) {
    kafkaTemplate.send(topic, key, message);
    log.info("Message has been sent to topic {}", topic);
}
}
