package com.khanh.fooddelivery.payment_service.outbox;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PaymentOutboxProperties.class)
public class PaymentOutboxConfiguration {
}
