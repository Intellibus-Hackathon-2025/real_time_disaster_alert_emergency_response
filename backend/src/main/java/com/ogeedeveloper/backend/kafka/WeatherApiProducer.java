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
public class WeatherApiProducer implements AlertProducer{
    private final KafkaProducer kafkaProducerService;

    private static final String SOURCE_TYPE = "WEATHER_API";
    private static final String TOPIC = "weather-alerts";

    @Override
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void fetchAndPublishAlerts() {
        log.info("Fetching weather alerts from Weather API");

        // TODO: Replace with actual Weather API call
        List<Alert> alerts = mockWeatherApiCall();

        for (Alert alert : alerts) {
            kafkaProducerService.sendAlert(TOPIC, alert);
            log.info("Published weather alert: {}", alert.getDetail());
        }
    }

    private List<Alert> mockWeatherApiCall() {
        // This would be replaced with actual API call
        GeoPoint location = new GeoPoint(-91.7, 31.01);
        String geoHash = GeoHash.encodeHash(location.getLatitude(), location.getLongitude(), 7);

        AlertType floodAlertType = AlertType.builder()
                .id(1L)
                .name("FLOOD")
                .build();

        Alert floodAlert = Alert.builder()
                .id(1L)
                .alertType(floodAlertType)
                .severity(SeverityType.warning())
                .detail("Flooding caused by excessive rainfall continues on the Mississippi River")
                .time(LocalDateTime.now())
                .geoHash(geoHash)
                .sourceId("NOAA-Weather-Service")
                .instructions(Arrays.asList(
                        "Turn around, don't drown when encountering flooded roads",
                        "Most flood deaths occur in vehicles"
                ))
                .targetUserRoles(Set.of(UserRole.ROLE_USER))
                .build();

        return Arrays.asList(floodAlert);
    }

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }
}
