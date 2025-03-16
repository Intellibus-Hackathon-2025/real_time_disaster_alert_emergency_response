package com.ogeedeveloper.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ogeedeveloper.backend.kafka.KafkaProducer;
import com.ogeedeveloper.backend.model.GeoPoint;
import com.ogeedeveloper.backend.model.UserLocation;
import com.ogeedeveloper.backend.util.GeoHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/location")
@Slf4j
@RequiredArgsConstructor
public class UserLocationController {
    private KafkaProducer kafkaProducerService;
    private ObjectMapper objectMapper;

    @PostMapping("/update")
    public ResponseEntity<String> updateLocation(@RequestBody UserLocation userLocation) {
        try {
            // Calculate geohash from coordinates if not provided
            if (userLocation.getGeoHash() == null) {
                GeoPoint locationPoint = userLocation.isOnTheMove() ?
                        userLocation.getCurrentLocation() : userLocation.getPermanentAddress();

                if (locationPoint != null) {
                    String geoHash = GeoHash.encodeHash(
                            locationPoint.getLatitude(),
                            locationPoint.getLongitude(),
                            7); // Precision of 7 gives ~76m precision
                    userLocation.setGeoHash(geoHash);
                }
            }

            String userLocationJson = objectMapper.writeValueAsString(userLocation);
            kafkaProducerService.sendMessage("user-locations", userLocation.getUserId(), userLocationJson);
            log.info("Location update received for user: {}", userLocation.getUserId());
            return ResponseEntity.ok("Location updated successfully");
        } catch (JsonProcessingException e) {
            log.error("Error serializing user location", e);
            return ResponseEntity.badRequest().body("Error processing location update");
        }
    }
}
