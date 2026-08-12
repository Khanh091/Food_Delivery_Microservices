package com.khanh.fooddelivery.restaurant_service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfiguration {
    private final OutboxProperties properties;
    @Bean NewTopic restaurantEventsTopic() {
        return TopicBuilder.name(properties.getTopic()).partitions(properties.getTopicPartitions())
                .replicas(properties.getTopicReplicationFactor()).build();
    }
}
