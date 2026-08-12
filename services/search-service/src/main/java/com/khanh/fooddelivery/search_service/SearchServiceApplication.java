package com.khanh.fooddelivery.search_service;

import com.khanh.fooddelivery.search_service.config.KafkaConsumerProperties;
import com.khanh.fooddelivery.search_service.config.SearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties({SearchProperties.class, KafkaConsumerProperties.class})
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }

}
