package com.ogeedeveloper.backend.kafka;

public interface AlertProducer {
    void fetchAndPublishAlerts();
    String getSourceType();
}
