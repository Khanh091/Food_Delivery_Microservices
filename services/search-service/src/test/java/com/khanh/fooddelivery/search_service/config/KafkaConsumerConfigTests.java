package com.khanh.fooddelivery.search_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

class KafkaConsumerConfigTests {
    @Test
    void provisionsDeadLetterTopicWithConfiguredTopology() {
        KafkaConsumerProperties properties = new KafkaConsumerProperties();
        properties.setDltTopic("catalog.events.dlt");
        properties.setDltTopicPartitions(3);
        properties.setDltTopicReplicationFactor((short) 1);

        NewTopic topic = new KafkaConsumerConfig(properties).catalogEventsDeadLetterTopic();

        assertThat(topic.name()).isEqualTo("catalog.events.dlt");
        assertThat(topic.numPartitions()).isEqualTo(3);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }
}
