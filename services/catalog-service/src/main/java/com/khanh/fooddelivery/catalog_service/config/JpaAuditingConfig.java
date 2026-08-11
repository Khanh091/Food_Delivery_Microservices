package com.khanh.fooddelivery.catalog_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "securityAuditorAware")
public class JpaAuditingConfig {}
