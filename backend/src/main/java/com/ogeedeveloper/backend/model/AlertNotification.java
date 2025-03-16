package com.ogeedeveloper.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AlertNotification {
    private Long id;
    private Alert alert;
    private String userId;
    private String userType;
    private LocalDateTime sentTime;
    private boolean isRead;
}
