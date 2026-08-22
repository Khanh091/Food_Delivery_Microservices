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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final KeycloakRealmRoleConverter roleConverter;

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "Idempotency-Key"
        ));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

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
                .cors(Customizer.withDefaults())
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
                                "/api/v1/public/restaurants/**"
                        )
                        .permitAll()

                        .pathMatchers(
                                HttpMethod.GET,
                                "/api/v1/restaurant-branches/*/operating-status"
                        )
                        .permitAll()

                        .pathMatchers(HttpMethod.GET, "/api/v1/public/catalog/**")
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
