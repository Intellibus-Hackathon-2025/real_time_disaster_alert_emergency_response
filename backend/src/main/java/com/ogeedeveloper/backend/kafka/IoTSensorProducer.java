package com.ogeedeveloper.backend.kafka;

import com.ogeedeveloper.backend.model.*;
import com.ogeedeveloper.backend.util.GeoHash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class IoTSensorProducer implements AlertProducer{
    private KafkaProducer kafkaProducerService;

    private static final String SOURCE_TYPE = "IOT_SENSOR";
    private static final String TOPIC = "iot-alerts";
    private final Random random = new Random();

    @Override
    @Scheduled(fixedRate = 60000) // Every minute
    public void fetchAndPublishAlerts() {
        log.info("Checking IoT sensors for alerts");

        // Simulate random sensor triggering
        if (random.nextInt(10) < 3) { // 30% chance of sensor alert
            List<Alert> alerts = createMockSensorAlerts();

            for (Alert alert : alerts) {
                kafkaProducerService.sendAlert(TOPIC, alert);
                log.info("Published IoT sensor alert: {}", alert.getDetail());
            }
        }
    }

    private List<Alert> createMockSensorAlerts() {
        // Simulate different types of sensor alerts

        // River level sensor
        GeoPoint riverLocation = new GeoPoint(-91.14, 31.87);
        String riverGeoHash = GeoHash.encodeHash(riverLocation.getLatitude(), riverLocation.getLongitude(), 7);

        AlertType waterLevelType = AlertType.builder()
                .id(2L)
                .name("WATER_LEVEL")
                .build();

        Alert waterLevelAlert = Alert.builder()
                .id(System.currentTimeMillis())
                .alertType(waterLevelType)
                .severity(SeverityType.warning())
                .detail("River level sensors detecting rapid rise at Thornburg Lake Road")
                .time(LocalDateTime.now())
                .geoHash(riverGeoHash)
                .sourceId("SENSOR-HYDRO-1234")
                .instructions(Arrays.asList(
                        "Be prepared for possible evacuation",
                        "Move valuables to higher ground"
                ))
                .targetUserRoles(Set.of(UserRole.ROLE_FIRST_RESPONDERS, UserRole.ROLE_USER))
                .build();

        return Arrays.asList(waterLevelAlert);
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }
}
