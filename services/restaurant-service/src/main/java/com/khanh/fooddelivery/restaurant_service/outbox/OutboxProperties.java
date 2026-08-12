package com.khanh.fooddelivery.restaurant_service.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.outbox")
public class OutboxProperties {
    private String topic = "restaurant.events";
    private int topicPartitions = 3;
    private short topicReplicationFactor = 1;
    private Publisher publisher = new Publisher();
    @Getter @Setter public static class Publisher {
        private boolean enabled = true;
        private long fixedDelayMs = 1000;
        private int batchSize = 50;
        private int maxRetries = 10;
        private long sendTimeoutMs = 5000;
        private long processingLeaseMs = 30000;
        private long initialRetryDelayMs = 5000;
        private long maxRetryDelayMs = 60000;
    }
}
