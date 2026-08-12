package com.khanh.fooddelivery.search_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka.consumer")
public class KafkaConsumerProperties {
    private String dltTopic = "catalog.events.dlt";
    private int dltTopicPartitions = 3;
    private short dltTopicReplicationFactor = 1;
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {
        private long maxAttempts = 3;
        private long backoffMs = 1000;
    }
}
