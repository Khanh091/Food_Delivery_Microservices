package com.khanh.fooddelivery.delivery_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khanh.fooddelivery.delivery_service.config.SecurityConfig;
import com.khanh.fooddelivery.delivery_service.security.KeycloakRealmRoleConverter;
import com.khanh.fooddelivery.delivery_service.service.ReverseGeocodingService;
import com.khanh.fooddelivery.delivery_service.service.LocationSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LocationController.class)
@Import({SecurityConfig.class, KeycloakRealmRoleConverter.class})
class LocationControllerSecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReverseGeocodingService reverseGeocodingService;

    @MockitoBean
    private LocationSearchService locationSearchService;

    @Test
    void reverseGeocodeRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/delivery/locations/reverse-geocode")
                        .contentType("application/json")
                        .content("{\"latitude\":10.776889,\"longitude\":106.700806}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void locationSearchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/locations/search").param("query", "Ha Noi"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void locationPlaceRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/delivery/locations/place").param("providerRefId", "auto:test"))
                .andExpect(status().isUnauthorized());
    }
}
