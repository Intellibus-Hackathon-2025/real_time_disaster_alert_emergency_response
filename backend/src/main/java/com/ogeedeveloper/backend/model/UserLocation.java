package com.ogeedeveloper.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class UserLocation {
    private String userId;
    private String userType; // CITIZEN, FIRST_RESPONDER
    private GeoPoint currentLocation;
    private GeoPoint permanentAddress;
    private boolean isOnTheMove;
    private LocalDateTime lastUpdated;
    private String geoHash;
}
