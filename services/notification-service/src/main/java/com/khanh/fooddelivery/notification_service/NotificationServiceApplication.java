package com.khanh.fooddelivery.notification_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.khanh.fooddelivery.notification_service.config.InternalApiProperties;

@SpringBootApplication
@EnableConfigurationProperties(InternalApiProperties.class)
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
