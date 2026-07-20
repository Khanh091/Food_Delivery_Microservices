package com.khanh.fooddelivery.restaurant_service.config;

import com.khanh.fooddelivery.restaurant_service.security.KeycloakRealmRoleConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final KeycloakRealmRoleConverter roles;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter c = new JwtAuthenticationConverter();
        c.setJwtGrantedAuthoritiesConverter(roles);
        return http.csrf(x -> x.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                HttpMethod.GET,
                                                "/actuator/health",
                                                "/actuator/info")
                                        .permitAll()
                                        .requestMatchers("/api/v1/restaurant-applications/**")
                                        .authenticated()
                                        .requestMatchers(
                                                "/api/v1/restaurants/**",
                                                "/api/v1/restaurant-branches/**")
                                        .hasAnyRole("RESTAURANT_OWNER", "RESTAURANT_STAFF", "ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(c)))
                .build();
    }
}
