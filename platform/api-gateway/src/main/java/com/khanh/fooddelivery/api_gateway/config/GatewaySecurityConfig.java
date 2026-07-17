package com.khanh.fooddelivery.api_gateway.config;

import com.khanh.fooddelivery.api_gateway.security.KeycloakRealmRoleConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final KeycloakRealmRoleConverter roleConverter;

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {
        JwtAuthenticationConverter jwtConverter =
                new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(
                roleConverter
        );

        ReactiveJwtAuthenticationConverterAdapter reactiveConverter =
                new ReactiveJwtAuthenticationConverterAdapter(
                        jwtConverter
                );

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                .authorizeExchange(exchange -> exchange

                        // Endpoint test trực tiếp trên Gateway
                        .pathMatchers("/gateway/test/public")
                        .permitAll()

                        .pathMatchers("/gateway/test/authenticated")
                        .authenticated()

                        .pathMatchers("/gateway/test/customer")
                        .hasRole("CUSTOMER")

                        .pathMatchers("/gateway/test/admin")
                        .hasRole("ADMIN")

                        // Actuator
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        )
                        .permitAll()

                        // Public downstream APIs
                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/search/**",
                                "/api/v1/restaurants/**",
                                "/api/v1/catalog/**"
                        )
                        .permitAll()

                        .pathMatchers("/api/v1/carts/**")
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .pathMatchers(
                                HttpMethod.POST,
                                "/api/v1/orders/**"
                        )
                        .hasAnyRole("CUSTOMER", "ADMIN")

                        .anyExchange()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        reactiveConverter
                                )
                        )
                )

                .build();
    }
}
