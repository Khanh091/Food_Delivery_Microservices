package com.khanh.fooddelivery.user_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.keycloak-admin")
public class KeycloakAdminProperties {
    private String baseUrl = "http://localhost:8180";
    private String realm = "food-delivery";
    private String clientId = "";
    private String clientSecret = "";
}
