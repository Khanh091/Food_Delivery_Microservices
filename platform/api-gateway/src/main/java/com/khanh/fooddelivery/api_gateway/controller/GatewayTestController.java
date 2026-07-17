package com.khanh.fooddelivery.api_gateway.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/gateway/test")
public class GatewayTestController {

    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of(
                "success", true,
                "message", "Public gateway endpoint is accessible"
        );
    }

    @GetMapping("/authenticated")
    public Map<String, Object> authenticatedEndpoint(
            Authentication authentication
    ) {
        JwtAuthenticationToken jwtAuthentication =
                (JwtAuthenticationToken) authentication;

        Jwt jwt = jwtAuthentication.getToken();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "JWT was verified successfully");
        response.put("subject", jwt.getSubject());
        response.put(
                "username",
                jwt.getClaimAsString("preferred_username")
        );
        response.put("email", jwt.getClaimAsString("email"));
        response.put("audience", jwt.getAudience());
        response.put(
                "authorities",
                authentication.getAuthorities()
                        .stream()
                        .map(Object::toString)
                        .toList()
        );

        return response;
    }

    @GetMapping("/customer")
    public Map<String, Object> customerEndpoint(
            Authentication authentication
    ) {
        return Map.of(
                "success", true,
                "message", "Customer authorization succeeded",
                "username", authentication.getName()
        );
    }

    @GetMapping("/admin")
    public Map<String, Object> adminEndpoint() {
        return Map.of(
                "success", true,
                "message", "Admin authorization succeeded"
        );
    }
}