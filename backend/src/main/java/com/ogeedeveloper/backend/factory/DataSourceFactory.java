package com.ogeedeveloper.backend.factory;

import com.ogeedeveloper.backend.kafka.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DataSourceFactory {
    private WeatherApiProducer weatherApiProducer;
    private IoTSensorProducer ioTSensorProducer;
    private SocialMediaProduce socialMediaProducer;
    private EmergencyServiceProducer emergencyServiceProducer;

    public AlertProducer getProducer(String sourceType) {
        return switch (sourceType) {
            case "WEATHER_API" -> weatherApiProducer;
            case "IOT_SENSOR" -> ioTSensorProducer;
            case "SOCIAL_MEDIA" -> socialMediaProducer;
            case "EMERGENCY_SERVICE" -> emergencyServiceProducer;
            default -> throw new IllegalArgumentException("Unknown source type: " + sourceType);
        };
    }
}
