package com.ogeedeveloper.backend.service;

import com.ogeedeveloper.backend.model.Alert;
import com.ogeedeveloper.backend.model.AlertNotification;
import com.ogeedeveloper.backend.model.GeoPoint;
import com.ogeedeveloper.backend.model.UserLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlertService {
    // In a real application, these would be stored in a database
    private final Map<Long, Alert> activeAlerts = new HashMap<>();
    private final Map<String, UserLocation> userLocations = new HashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void processAndNotifyUsers(Alert alert) {
        // Store alert
        activeAlerts.put(alert.getId(), alert);

        // Find affected users
        List<UserLocation> affectedUsers = findAffectedUsers(alert);

        // Notify each affected user
        for (UserLocation user : affectedUsers) {
            notifyUser(user, alert);
        }
    }

    public void updateUserLocationAndCheckAlerts(UserLocation userLocation) {
        // Calculate geohash if not provided
        if (userLocation.getGeoHash() == null) {
            GeoPoint location = userLocation.isOnTheMove() ?
                    userLocation.getCurrentLocation() : userLocation.getPermanentAddress();
            userLocation.setGeoHash(GeoHash.encodeHash(location.getLatitude(), location.getLongitude(), 7));
        }

        // Update stored user location
        userLocations.put(userLocation.getUserId(), userLocation);

        // Check if user is in any active alert areas
        List<Alert> relevantAlerts = findRelevantAlerts(userLocation);

        // Notify user of any relevant alerts
        for (Alert alert : relevantAlerts) {
            notifyUser(userLocation, alert);
        }
    }

    private List<UserLocation> findAffectedUsers(Alert alert) {
        String alertGeoHash = alert.getGeoHash();
        List<String> targetUserTypes = alert.getTargetUserTypes();

        // Find matching users based on geohash prefix (affected area)
        // Usually we'd want to check if user's geohash starts with the alert's geohash prefix
        // or use a more sophisticated spatial matching algorithm
        int prefixLength = Math.min(4, alertGeoHash.length()); // Use first 4 chars as prefix
        String geoHashPrefix = alertGeoHash.substring(0, prefixLength);

        return userLocations.values().stream()
                .filter(user -> targetUserTypes.contains(user.getUserType()))
                .filter(user -> user.getGeoHash() != null && user.getGeoHash().startsWith(geoHashPrefix))
                .collect(Collectors.toList());
    }

    private List<Alert> findRelevantAlerts(UserLocation userLocation) {
        String userGeoHash = userLocation.getGeoHash();

        // Find alerts that affect this user's location and type
        return activeAlerts.values().stream()
                .filter(alert -> alert.getTargetUserTypes().contains(userLocation.getUserType()))
                .filter(alert -> {
                    // Compare geohash prefixes to determine if user is in affected area
                    int prefixLength = Math.min(4, alert.getGeoHash().length());
                    String alertPrefix = alert.getGeoHash().substring(0, prefixLength);
                    return userGeoHash.startsWith(alertPrefix);
                })
                .collect(Collectors.toList());
    }

    private void notifyUser(UserLocation user, Alert alert) {
        // Create notification
        AlertNotification notification = AlertNotification.builder()
                .id(System.currentTimeMillis()) // Simple ID generator
                .alert(alert)
                .userId(user.getUserId())
                .userType(user.getUserType())
                .sentTime(LocalDateTime.now())
                .isRead(false)
                .build();

        // Send notification via WebSocket
        messagingTemplate.convertAndSendToUser(user.getUserId(), "/alert", notification);

        log.info("Sent alert notification to user {}: {}", user.getUserId(), alert.getDetail());
    }
}
