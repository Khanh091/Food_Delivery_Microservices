package com.khanh.fooddelivery.order_service.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
public class PaymentLifecycleKafkaConfiguration {

    @Value("${app.kafka.payment-lifecycle-topic:payment.lifecycle.v1}")
    private String paymentLifecycleTopic;

    @Value("${app.kafka.payment-lifecycle-dlt-topic:payment.lifecycle.v1.DLT}")
    private String paymentLifecycleDltTopic;

    @Value("${app.kafka.payment-lifecycle-retry-backoff-ms:1000}")
    private long retryBackoffMs;

    @Value("${app.kafka.payment-lifecycle-retry-max-attempts:3}")
    private long retryMaxAttempts;

    @Bean
    NewTopic paymentLifecycleTopic() {
        return TopicBuilder.name(paymentLifecycleTopic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentLifecycleDltTopic() {
        return TopicBuilder.name(paymentLifecycleDltTopic).partitions(3).replicas(1).build();
    }

    @Bean(name = "paymentLifecycleKafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> paymentLifecycleKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(paymentLifecycleErrorHandler(kafkaTemplate));
        return factory;
    }

    private CommonErrorHandler paymentLifecycleErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (ConsumerRecord<?, ?> record, Exception exception) ->
                        new TopicPartition(paymentLifecycleDltTopic, record.partition())
        );
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryBackoffMs, retryMaxAttempts)
        );
    }
}
