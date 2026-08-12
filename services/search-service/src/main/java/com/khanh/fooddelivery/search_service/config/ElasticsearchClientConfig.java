package com.khanh.fooddelivery.search_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ElasticsearchClientConfig {
    @Bean
    RestClient searchElasticsearchRestClient(RestClient.Builder builder, SearchProperties properties) {
        return builder.baseUrl(properties.getElasticsearchUri()).build();
    }
}
