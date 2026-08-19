package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantStatusHistoryMapper;
import com.khanh.fooddelivery.restaurant_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantStatusHistoryRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTests {
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private RestaurantRepository repository;
    @Mock private RestaurantStatusHistoryRepository histories;
    @Mock private RestaurantMapper mapper;
    @Mock private RestaurantStatusHistoryMapper historyMapper;
    @Mock private RestaurantAuthorizationService authorization;
    @Mock private CurrentUserProvider currentUser;
    @Mock private OutboxEventService outbox;
    @Mock private Jwt jwt;

    private RestaurantServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RestaurantServiceImpl(
                repository, histories, mapper, historyMapper, authorization, currentUser, outbox);
    }

    @Test
    void updateRejectsUsersWithoutManagementRole() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        when(repository.findByIdForUpdate(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(currentUser.getCurrentUserId(jwt)).thenReturn(USER_ID);
        doThrow(new AppException(ErrorCode.RESTAURANT_ACCESS_DENIED))
                .when(authorization)
                .requireRestaurantAccess(
                        eq(RESTAURANT_ID),
                        eq(USER_ID),
                        any(RestaurantMemberRole[].class));

        RestaurantUpdateRequest request =
                new RestaurantUpdateRequest("Updated", null, null, null, null, null);

        assertThatThrownBy(() -> service.update(jwt, RESTAURANT_ID, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESTAURANT_ACCESS_DENIED);

        verify(mapper, never()).update(any(), any());
    }
}