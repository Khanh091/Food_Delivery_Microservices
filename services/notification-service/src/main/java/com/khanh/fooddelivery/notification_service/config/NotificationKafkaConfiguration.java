package com.khanh.fooddelivery.notification_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class NotificationKafkaConfiguration {

    @Value("${app.kafka.delivery-lifecycle-topic:delivery.lifecycle.v1}")
    private String deliveryLifecycleTopic;

    @Value("${app.kafka.delivery-lifecycle-dlt-topic:delivery.lifecycle.v1.DLT}")
    private String deliveryLifecycleDltTopic;

    @Value("${app.kafka.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    @Value("${app.kafka.retry-max-attempts:3}")
    private long retryMaxAttempts;

    @Bean
    NewTopic deliveryLifecycleTopic() {
        return TopicBuilder.name(deliveryLifecycleTopic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic deliveryLifecycleDltTopic() {
        return TopicBuilder.name(deliveryLifecycleDltTopic).partitions(3).replicas(1).build();
    }

    @Bean(name = "deliveryLifecycleKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> deliveryLifecycleKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(deliveryLifecycleErrorHandler(kafkaTemplate));
        return factory;
    }

    private CommonErrorHandler deliveryLifecycleErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(deliveryLifecycleDltTopic, record.partition())
        );
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryBackoffMs, retryMaxAttempts)
        );
    }
}
