package com.ogeedeveloper.backend.controller;

import com.ogeedeveloper.backend.model.AlertNotification;
import com.ogeedeveloper.backend.security.AlertNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AlertWebSocketController {
    private SimpMessagingTemplate messagingTemplate;
    private final AlertNotificationService alertNotificationService;
    // Track connected users
    private final Map<String, String> connectedUsers = new HashMap<>();

    @MessageMapping("/alert.connect")
    public void connect(SimpMessageHeaderAccessor headerAccessor) {
        String userId = Objects.requireNonNull(headerAccessor.getUser()).getName();
        connectedUsers.put(headerAccessor.getSessionId(), userId);
        log.info("User connected: {}", userId);
    }

    @MessageMapping("/alert.acknowledge")
    public void acknowledgeAlert(@Payload AlertNotification notification, Principal principal) {
        String userId = principal.getName();
        log.info("User {} acknowledged alert: {}", userId, notification.getAlert().getDetail());

        // Update the notification status in the database
        alertNotificationService.updateNotificationStatus(notification.getId(), true);

        // Send confirmation back to the user
        Map<String, Object> response = new HashMap<>();
        response.put("status", "acknowledged");
        response.put("notificationId", notification.getId());

        messagingTemplate.convertAndSendToUser(userId, "/alert/confirmation", response);
    }

    @MessageMapping("/alert.dismiss")
    public void dismissAlert(@Payload AlertNotification notification, Principal principal) {
        String userId = principal.getName();
        log.info("User {} dismissed alert: {}", userId, notification.getAlert().getDetail());

        // Update the notification status in the database
        alertNotificationService.updateNotificationStatus(notification.getId(), false);

        // Send confirmation back to the user
        Map<String, Object> response = new HashMap<>();
        response.put("status", "dismissed");
        response.put("notificationId", notification.getId());

        messagingTemplate.convertAndSendToUser(userId, "/alert/confirmation", response);
    }

    @MessageMapping("/alert.status")
    public void getAlertStatus(Principal principal) {
        String userId = principal.getName();
        List<AlertNotification> notifications = alertNotificationService.getNotificationsByUserId(userId);

        // Send the notification statuses back to the user
        messagingTemplate.convertAndSendToUser(userId, "/alert/status", notifications);
    }

}
