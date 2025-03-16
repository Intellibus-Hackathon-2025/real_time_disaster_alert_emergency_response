package com.ogeedeveloper.backend.security;

import com.ogeedeveloper.backend.model.AlertNotification;
import com.ogeedeveloper.backend.repository.AlertNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertNotificationService {
    private final AlertNotificationRepository alertNotificationRepository;

    public List<AlertNotification> getNotificationsByUserId(String userId) {
        return alertNotificationRepository.findByUserId(userId);
    }

    public void updateNotificationStatus(Long notificationId, boolean isRead) {
        AlertNotification notification = alertNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(isRead);
        alertNotificationRepository.save(notification);
    }
}