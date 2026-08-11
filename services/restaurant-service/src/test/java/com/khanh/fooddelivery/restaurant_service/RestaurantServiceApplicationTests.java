package com.khanh.fooddelivery.restaurant_service;

import com.khanh.fooddelivery.restaurant_service.repository.BranchBusinessHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.BranchSpecialHourRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantApplicationDocumentRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBankAccountRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantMemberRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantPartnerApplicationRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest(
        properties = {
            "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "eureka.client.enabled=false"
        })
class RestaurantServiceApplicationTests {
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

    @MockBean(name = "jpaMappingContext")
    JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void contextLoads() {}
}
