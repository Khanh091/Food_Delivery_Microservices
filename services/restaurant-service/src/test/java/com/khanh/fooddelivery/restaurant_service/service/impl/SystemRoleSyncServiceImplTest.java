package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.client.SystemRoleClient;
import com.khanh.fooddelivery.restaurant_service.config.InternalApiProperties;
import com.khanh.fooddelivery.restaurant_service.config.SystemRoleSyncProperties;
import com.khanh.fooddelivery.restaurant_service.entity.SystemRoleSyncRequest;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.enums.SystemRoleSyncOperation;
import com.khanh.fooddelivery.restaurant_service.enums.SystemRoleSyncStatus;
import com.khanh.fooddelivery.restaurant_service.identity.SystemRole;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.repository.SystemRoleSyncRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SystemRoleSyncServiceImplTest {
    @Mock private SystemRoleSyncRequestRepository requests;
    @Mock private RestaurantRepository restaurants;
    @Mock private SystemRoleClient systemRoleClient;

    private InternalApiProperties internalApi;
    private SystemRoleSyncProperties properties;
    private SystemRoleSyncServiceImpl service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        internalApi = new InternalApiProperties();
        internalApi.setKey("internal-test-key");
        properties = new SystemRoleSyncProperties();
        service = new SystemRoleSyncServiceImpl(requests, restaurants, systemRoleClient, internalApi, properties);
        userId = UUID.randomUUID();
    }

    @Test
    void approvalEnqueuesOwnerGrantAndRepeatedEnqueueIsIdempotent() {
        when(requests.findTopByUserIdAndSystemRoleAndOperationAndStatusOrderByCreatedAtDesc(
                        eq(userId),
                        eq(SystemRole.RESTAURANT_OWNER.name()),
                        eq(SystemRoleSyncOperation.GRANT),
                        eq(SystemRoleSyncStatus.PENDING)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new SystemRoleSyncRequest()));
        when(requests.save(any(SystemRoleSyncRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueueGrant(userId, SystemRole.RESTAURANT_OWNER);
        service.enqueueGrant(userId, SystemRole.RESTAURANT_OWNER);

        ArgumentCaptor<SystemRoleSyncRequest> captor = ArgumentCaptor.forClass(SystemRoleSyncRequest.class);
        verify(requests, org.mockito.Mockito.times(1)).save(captor.capture());
        assertThat(captor.getValue().getSystemRole()).isEqualTo(SystemRole.RESTAURANT_OWNER.name());
        assertThat(captor.getValue().getOperation()).isEqualTo(SystemRoleSyncOperation.GRANT);
        assertThat(captor.getValue().getStatus()).isEqualTo(SystemRoleSyncStatus.PENDING);
    }

    @Test
    void existingActiveOwnersAreReconciledAsGrantRequests() {
        when(restaurants.findOwnerUserIdsByStatusIn(
                        eq(List.of(RestaurantStatus.ACTIVE, RestaurantStatus.PENDING))))
                .thenReturn(List.of(userId));
        when(requests.findTopByUserIdAndSystemRoleAndOperationAndStatusOrderByCreatedAtDesc(
                        eq(userId),
                        eq(SystemRole.RESTAURANT_OWNER.name()),
                        eq(SystemRoleSyncOperation.GRANT),
                        eq(SystemRoleSyncStatus.PENDING)))
                .thenReturn(Optional.empty());
        when(requests.save(any(SystemRoleSyncRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.reconcileRestaurantOwners()).isEqualTo(1);
    }

    @Test
    void successfulRetryCallsInternalRoleApiAndCompletesWithoutTouchingRestaurant() {
        SystemRoleSyncRequest request = request(SystemRoleSyncStatus.PENDING);
        when(requests.findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        eq(SystemRoleSyncStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                .thenReturn(List.of(request));
        doNothing().when(systemRoleClient).grantRole(userId, SystemRole.RESTAURANT_OWNER.name(), internalApi.getKey());

        service.processDue();

        assertThat(request.getStatus()).isEqualTo(SystemRoleSyncStatus.COMPLETED);
        assertThat(request.getRetryCount()).isZero();
        verify(systemRoleClient).grantRole(userId, SystemRole.RESTAURANT_OWNER.name(), internalApi.getKey());
        verify(restaurants, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void keycloakFailureIsRetryableAndDoesNotFailTheApprovedRestaurantData() {
        SystemRoleSyncRequest request = request(SystemRoleSyncStatus.PENDING);
        when(requests.findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        eq(SystemRoleSyncStatus.PENDING), any(Instant.class), any(PageRequest.class)))
                .thenReturn(List.of(request));
        doThrow(new RuntimeException("Keycloak unavailable"))
                .when(systemRoleClient)
                .grantRole(userId, SystemRole.RESTAURANT_OWNER.name(), internalApi.getKey());

        service.processDue();

        assertThat(request.getStatus()).isEqualTo(SystemRoleSyncStatus.PENDING);
        assertThat(request.getRetryCount()).isEqualTo(1);
        assertThat(request.getLastError()).contains("Keycloak unavailable");
        verify(restaurants, org.mockito.Mockito.never()).save(any());
    }

    private SystemRoleSyncRequest request(SystemRoleSyncStatus status) {
        SystemRoleSyncRequest request = new SystemRoleSyncRequest();
        request.setUserId(userId);
        request.setSystemRole(SystemRole.RESTAURANT_OWNER.name());
        request.setOperation(SystemRoleSyncOperation.GRANT);
        request.setStatus(status);
        request.setRetryCount(0);
        request.setNextRetryAt(Instant.now().minusSeconds(1));
        return request;
    }
}