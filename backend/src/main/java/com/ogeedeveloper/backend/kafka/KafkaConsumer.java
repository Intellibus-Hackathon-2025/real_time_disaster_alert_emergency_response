package com.ogeedeveloper.backend.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogeedeveloper.backend.model.Alert;
import com.ogeedeveloper.backend.model.UserLocation;
import com.ogeedeveloper.backend.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {
    private SimpMessagingTemplate messagingTemplate;
    private ObjectMapper objectMapper;
    private final AlertService alertService;

    @KafkaListener(topics = {"weather-alerts", "iot-alerts", "social-media-alerts", "emergency-alerts"},
            groupId = "alert-group")
    public void consume(String message) {
        try{
            Alert alert = objectMapper.readValue(message, Alert.class);
            log.info("Consumed message- alert: {}", alert);

//            Send the alert to the affected user
            alertService.processAndNotifyUsers(alert);
        } catch (Exception e) {
            log.error("Error consuming message: {}", message, e);
        }
    }

    @KafkaListener(topics = "user-locations", groupId = "alert-group")
    public void consumeUserLocation(String message) {
        try {
            UserLocation userLocation = objectMapper.readValue(message, UserLocation.class);
            log.info("Received user location update for user: {}", userLocation.getUserId());

            // Update user location and check for relevant alerts
            alertService.updateUserLocationAndCheckAlerts(userLocation);

        } catch (JsonProcessingException e) {
            log.error("Error deserializing user location from JSON", e);
        }
    }

}
