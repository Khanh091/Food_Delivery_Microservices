package com.khanh.fooddelivery.delivery_service.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfiguration {

    @Value("${app.kafka.order-confirmed-topic:order.lifecycle.v1}")
    private String orderConfirmedTopic;

    @Value("${app.kafka.order-confirmed-dlt-topic:order.lifecycle.v1.DLT}")
    private String orderConfirmedDltTopic;

    @Value("${app.kafka.retry-backoff-ms:1000}")
    private long retryBackoffMs;

    @Value("${app.kafka.retry-max-attempts:3}")
    private long retryMaxAttempts;

    @Bean
    NewTopic orderConfirmedTopic() {
        return TopicBuilder.name(orderConfirmedTopic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderConfirmedDltTopic() {
        return TopicBuilder.name(orderConfirmedDltTopic).partitions(3).replicas(1).build();
    }

    @Bean(name = "kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(orderConfirmedErrorHandler(kafkaTemplate));
        return factory;
    }

    private CommonErrorHandler orderConfirmedErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(orderConfirmedDltTopic, record.partition())
        );
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryBackoffMs, retryMaxAttempts)
        );
    }
}
