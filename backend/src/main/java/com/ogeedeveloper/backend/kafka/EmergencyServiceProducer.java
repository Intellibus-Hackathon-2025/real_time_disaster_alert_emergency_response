package com.ogeedeveloper.backend.kafka;

import com.ogeedeveloper.backend.model.*;
import com.ogeedeveloper.backend.util.GeoHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmergencyServiceProducer implements AlertProducer {
    private KafkaProducer kafkaProducerService;

    private static final String SOURCE_TYPE = "EMERGENCY_SERVICE";
    private static final String TOPIC = "emergency-alerts";

    @Override
    @Scheduled(fixedRate = 600000) // Every 10 minutes
    public void fetchAndPublishAlerts() {
        log.info("Checking emergency service reports");

        // This would normally poll emergency service APIs
        List<Alert> alerts = createMockEmergencyAlerts();

        for (Alert alert : alerts) {
            kafkaProducerService.sendAlert(TOPIC, alert);
            log.info("Published emergency service alert: {}", alert.getDetail());
        }
    }

    private List<Alert> createMockEmergencyAlerts() {
        // Road closure from emergency services
        GeoPoint roadLocation = new GeoPoint(-89.76, 30.67);
        String roadGeoHash = GeoHash.encodeHash(roadLocation.getLatitude(), roadLocation.getLongitude(), 7);

        AlertType roadClosureType = AlertType.builder()
                .id(4L)
                .name("ROAD_CLOSURE")
                .build();

        Alert roadClosureAlert = Alert.builder()
                .id(System.currentTimeMillis())
                .alertType(roadClosureType)
                .severity(SeverityType.advisory())
                .detail("Pearl River flooding has closed Highway 59 - Emergency vehicles only")
                .time(LocalDateTime.now())
                .geoHash(roadGeoHash)
                .sourceId("MS-DOT")
                .instructions(Arrays.asList(
                        "Use alternate routes",
                        "Road expected to reopen Tuesday"
                ))
                .targetUserRoles(Set.of(UserRole.ROLE_USER))
                .build();

        // Evacuation order from emergency management
        GeoPoint evacuationLocation = new GeoPoint(-88.84, 34.45);
        String evacuationGeoHash = GeoHash.encodeHash(evacuationLocation.getLatitude(), evacuationLocation.getLongitude(), 7);

        AlertType evacuationType = AlertType.builder()
                .id(5L)
                .name("EVACUATION")
                .build();

        Alert evacuationAlert = Alert.builder()
                .id(System.currentTimeMillis() + 1)
                .alertType(evacuationType)
                .severity(SeverityType.critical())
                .detail("Mandatory evacuation order for Lee County near Town Creek")
                .time(LocalDateTime.now())
                .geoHash(evacuationGeoHash)
                .sourceId("LEE-COUNTY-EMA")
                .instructions(Arrays.asList(
                        "Evacuate immediately to designated shelters",
                        "Bring essential medications and documents",
                        "Evacuees can report to Tupelo High School"
                ))
                .targetUserRoles(Set.of(UserRole.ROLE_FIRST_RESPONDERS))
                .build();

        return Arrays.asList(roadClosureAlert, evacuationAlert);
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }
}
