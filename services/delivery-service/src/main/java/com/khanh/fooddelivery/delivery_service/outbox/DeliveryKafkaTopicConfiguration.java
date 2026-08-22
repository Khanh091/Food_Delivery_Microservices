package com.khanh.fooddelivery.delivery_service.outbox;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class DeliveryKafkaTopicConfiguration {

    private final DeliveryOutboxProperties properties;

    @Bean
    NewTopic deliveryLifecycleTopic() {
        return TopicBuilder.name(properties.getTopic())
                .partitions(properties.getTopicPartitions())
                .replicas(properties.getTopicReplicationFactor())
                .build();
    }
}
