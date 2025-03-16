package com.ogeedeveloper.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {
    private Double longitude;
    private Double latitude;

    // Helper method to convert to geohash
    public String toGeoHash() {
        // Implementation of geohash encoding would go here
        // For simplicity, we'll use a placeholder
        return "TODO: Implement proper geohash encoding";
    }
}
