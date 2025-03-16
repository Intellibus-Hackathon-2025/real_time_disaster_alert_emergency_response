package com.ogeedeveloper.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {
    private Long id;
    private SeverityType severity;
    private String detail;
    private LocalDateTime time;
    private String geoHash; // Geohash for location
    private AlertType alertType;
    private List<String> instructions;
    private List<String> targetUserTypes; // CITIZEN, FIRST_RESPONDER, etc.
    private String sourceId; // ID of the source system
}
