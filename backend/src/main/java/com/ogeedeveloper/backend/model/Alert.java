package com.ogeedeveloper.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity(name = "alerts")
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private SeverityType severity;

    private String detail;
    private LocalDateTime time;
    private String geoHash; // Geohash for location

    @ManyToOne(optional = false)
    private AlertType alertType;

    @ElementCollection
    private List<String> instructions;

    @ElementCollection(targetClass = UserRole.class)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> targetUserRoles; // Using UserRole enum for user types

    private String sourceId; // ID of the source system
}
