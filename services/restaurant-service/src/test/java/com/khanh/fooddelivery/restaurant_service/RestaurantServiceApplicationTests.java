package com.khanh.fooddelivery.restaurant_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khanh.fooddelivery.restaurant_service.dto.response.BranchOperatingStatusResponse;
import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.BranchSpecialHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantApplicationDocumentRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBankAccountRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantMemberRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantPartnerApplicationRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantStatusHistoryRepository;
import com.khanh.fooddelivery.restaurant_service.service.BranchOperatingStatusService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@SpringBootTest(
        properties = {
            "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "eureka.client.enabled=false"
        })
class RestaurantServiceApplicationTests {
    private final MockMvc mockMvc;

    @Autowired
    RestaurantServiceApplicationTests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @MockBean RestaurantPartnerApplicationRepository applications;
    @MockBean RestaurantApplicationDocumentRepository documents;
    @MockBean RestaurantRepository restaurants;
    @MockBean RestaurantBranchRepository branches;
    @MockBean RestaurantMemberRepository members;
    @MockBean BranchBusinessHourRepository businessHours;
    @MockBean BranchSpecialHourRepository specialHours;
    @MockBean RestaurantBankAccountRepository bankAccounts;
    @MockBean RestaurantStatusHistoryRepository histories;
    @MockBean JwtDecoder jwtDecoder;
    @MockBean RestaurantService restaurantService;
    @MockBean BranchOperatingStatusService branchOperatingStatusService;

    @MockBean(name = "jpaMappingContext")
    JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void contextLoads() {}

    @Test
    void operatingStatusIsPublic() throws Exception {
        when(branchOperatingStatusService.getOperatingStatus(any(), any()))
                .thenReturn(new BranchOperatingStatusResponse(false, "NOT_ACCEPTING_ORDERS"));

        mockMvc.perform(get("/api/v1/restaurant-branches/{id}/operating-status", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousRestaurantWriteIsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/restaurants/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void supportCanReachSuspendAndRestoreOnly() throws Exception {
        SimpleGrantedAuthority support = new SimpleGrantedAuthority("ROLE_SUPPORT");

        mockMvc.perform(
                        post("/api/v1/restaurants/{id}/suspend", UUID.randomUUID())
                                .with(jwt().authorities(support)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/restaurants/{id}/restore", UUID.randomUUID())
                                .with(jwt().authorities(support)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/v1/restaurants/{id}", UUID.randomUUID())
                                .with(jwt().authorities(support)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        patch("/api/v1/restaurants/{id}", UUID.randomUUID())
                                .with(jwt().authorities(support)))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAccessRestaurantManagement() throws Exception {
        SimpleGrantedAuthority customer = new SimpleGrantedAuthority("ROLE_CUSTOMER");

        mockMvc.perform(
                        patch("/api/v1/restaurants/{id}", UUID.randomUUID())
                                .with(jwt().authorities(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReachSuspendAndRestore() throws Exception {
        SimpleGrantedAuthority admin = new SimpleGrantedAuthority("ROLE_ADMIN");

        mockMvc.perform(
                        post("/api/v1/restaurants/{id}/suspend", UUID.randomUUID())
                                .with(jwt().authorities(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/v1/restaurants/{id}/restore", UUID.randomUUID())
                                .with(jwt().authorities(admin)))
                .andExpect(status().isOk());
    }
}
