package com.ogeedeveloper.backend.service;

import com.ogeedeveloper.backend.model.*;
import com.ogeedeveloper.backend.util.GeoHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {
    // In a real application, these would be stored in a database
    private final Map<Long, Alert> activeAlerts = new HashMap<>();
    private final Map<String, UserLocation> userLocations = new HashMap<>();

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

            if (location != null) {
                userLocation.setGeoHash(GeoHash.encodeHash(location.getLatitude(), location.getLongitude(), 7));
            }
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
        Set<UserRole> targetUserRoles = alert.getTargetUserRoles();

        // Use the first 4 characters of the geohash as a prefix for a wider area match
        int prefixLength = Math.min(4, alertGeoHash.length());
        String geoHashPrefix = alertGeoHash.substring(0, prefixLength);

        return userLocations.values().stream()
                .filter(user -> {
                    try {
                        // Convert the userType string to a UserRole enum before checking
                        return targetUserRoles.contains(UserRole.valueOf(user.getUserType()));
                    } catch (IllegalArgumentException e) {
                        // If conversion fails, exclude the user
                        return false;
                    }
                })
                .filter(user -> user.getGeoHash() != null && user.getGeoHash().startsWith(geoHashPrefix))
                .collect(Collectors.toList());
    }

    private List<Alert> findRelevantAlerts(UserLocation userLocation) {
        String userGeoHash = userLocation.getGeoHash();

        if (userGeoHash == null) {
            return List.of(); // Can't match without geohash
        }

        UserRole userRole;
        try {
            userRole = UserRole.valueOf(userLocation.getUserType());
        } catch (IllegalArgumentException e) {
            // If conversion fails, no alerts are relevant
            return List.of();
        }

        // Find alerts that affect this user's location and role
        return activeAlerts.values().stream()
                .filter(alert -> alert.getTargetUserRoles().contains(userRole))
                .filter(alert -> {
                    // Compare geohash prefixes to determine if the user is in the affected area
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
