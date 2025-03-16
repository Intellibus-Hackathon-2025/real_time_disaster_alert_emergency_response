package com.ogeedeveloper.backend.kafka;

import com.ogeedeveloper.backend.model.Alert;
import com.ogeedeveloper.backend.model.AlertType;
import com.ogeedeveloper.backend.model.GeoPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class SocialMediaProduce implements AlertProducer{
    private KafkaProducer kafkaProducerService;

    private static final String SOURCE_TYPE = "SOCIAL_MEDIA";
    private static final String TOPIC = "social-media-alerts";
    private final Random random = new Random();

    @Override
    @Scheduled(fixedRate = 180000) // Every 3 minutes
    public void fetchAndPublishAlerts() {
        log.info("Analyzing social media streams for emergency patterns");

        // Simulate random social media trend detection
        if (random.nextInt(10) < 2) { // 20% chance of detecting relevant trend
            List<Alert> alerts = createMockSocialMediaAlerts();

            for (Alert alert : alerts) {
                kafkaProducerService.sendAlert(TOPIC, alert);
                log.info("Published social media based alert: {}", alert.getDetail());
            }
        }
    }

    private List<Alert> createMockSocialMediaAlerts() {
        // Simulate alerts derived from social media trend analysis

        // Traffic incident
        GeoPoint trafficLocation = new GeoPoint(-88.51, 33.52);
        String trafficGeoHash = GeoHash.encodeHash(trafficLocation.getLatitude(), trafficLocation.getLongitude(), 7);

        AlertType trafficType = AlertType.builder()
                .id(3L)
                .name("TRAFFIC_INCIDENT")
                .build();

        Alert trafficAlert = Alert.builder()
                .id(System.currentTimeMillis())
                .alertType(trafficType)
                .severity(SeverityType.advisory())
                .detail("Multiple social media reports of flooding on Highway 6 near Tupelo")
                .time(LocalDateTime.now())
                .geoHash(trafficGeoHash)
                .sourceId("SOCIAL-TREND-ANALYSIS")
                .instructions(Arrays.asList(
                        "Avoid Highway 6 if possible",
                        "Seek alternate routes"
                ))
                .targetUserTypes(Arrays.asList("CITIZEN"))
                .build();

        return Arrays.asList(trafficAlert);
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }
}
