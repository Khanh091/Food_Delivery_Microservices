package com.khanh.fooddelivery.catalog_service;

import com.khanh.fooddelivery.catalog_service.outbox.OutboxProperties;
import com.khanh.fooddelivery.catalog_service.service.CatalogSearchReindexProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties({OutboxProperties.class, CatalogSearchReindexProperties.class})
@SpringBootApplication
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
