package com.ogeedeveloper.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLocation {
    private String userId;
    private String userType; // CITIZEN, FIRST_RESPONDER
    private GeoPoint currentLocation;
    private GeoPoint permanentAddress;
    private boolean isOnTheMove;
    private LocalDateTime lastUpdated;
    private String geoHash;
}
