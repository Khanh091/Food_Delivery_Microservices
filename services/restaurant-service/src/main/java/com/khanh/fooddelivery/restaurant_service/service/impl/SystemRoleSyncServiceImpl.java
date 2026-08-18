package com.khanh.fooddelivery.restaurant_service.service.impl;

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
import com.khanh.fooddelivery.restaurant_service.service.SystemRoleSyncService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemRoleSyncServiceImpl implements SystemRoleSyncService {
    private static final int BATCH_SIZE = 50;

    private final SystemRoleSyncRequestRepository requests;
    private final RestaurantRepository restaurants;
    private final SystemRoleClient systemRoleClient;
    private final InternalApiProperties internalApi;
    private final SystemRoleSyncProperties properties;

    @Override
    @Transactional
    public void enqueueGrant(UUID userId, SystemRole role) {
        enqueue(userId, role, SystemRoleSyncOperation.GRANT);
    }

    @Override
    @Transactional
    public void enqueueRevoke(UUID userId, SystemRole role) {
        enqueue(userId, role, SystemRoleSyncOperation.REVOKE);
    }

    @Override
    @Scheduled(fixedDelayString = "${app.system-role-sync.fixed-delay-ms:5000}")
    public void processDue() {
        if (!properties.isEnabled()) return;
        List<SystemRoleSyncRequest> due =
                requests.findByStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        SystemRoleSyncStatus.PENDING,
                        Instant.now(),
                        PageRequest.of(0, BATCH_SIZE));
        due.forEach(this::process);
    }

    @Override
    @Transactional
    public int reconcileRestaurantOwners() {
        List<UUID> owners =
                restaurants.findOwnerUserIdsByStatusIn(
                        List.of(RestaurantStatus.ACTIVE, RestaurantStatus.PENDING));
        int enqueued = 0;
        for (UUID owner : owners) {
            if (enqueue(owner, SystemRole.RESTAURANT_OWNER, SystemRoleSyncOperation.GRANT)) {
                enqueued++;
            }
        }
        return enqueued;
    }

    @Transactional
    protected boolean enqueue(UUID userId, SystemRole role, SystemRoleSyncOperation operation) {
        if (requests
                .findTopByUserIdAndSystemRoleAndOperationAndStatusOrderByCreatedAtDesc(
                        userId, role.name(), operation, SystemRoleSyncStatus.PENDING)
                .isPresent()) {
            return false;
        }
        SystemRoleSyncRequest request = new SystemRoleSyncRequest();
        request.setUserId(userId);
        request.setSystemRole(role.name());
        request.setOperation(operation);
        request.setStatus(SystemRoleSyncStatus.PENDING);
        request.setRetryCount(0);
        request.setNextRetryAt(Instant.now());
        requests.save(request);
        return true;
    }

    @Transactional
    protected void process(SystemRoleSyncRequest request) {
        try {
            switch (request.getOperation()) {
                case GRANT -> systemRoleClient.grantRole(
                        request.getUserId(), request.getSystemRole(), internalApi.getKey());
                case REVOKE -> systemRoleClient.revokeRole(
                        request.getUserId(), request.getSystemRole(), internalApi.getKey());
            }
            request.setStatus(SystemRoleSyncStatus.COMPLETED);
            request.setProcessedAt(Instant.now());
            request.setLastError(null);
            request.setNextRetryAt(null);
            requests.save(request);
        } catch (Exception exception) {
            markRetryableFailure(request, exception);
        }
    }

    private void markRetryableFailure(SystemRoleSyncRequest request, Exception exception) {
        int retries = request.getRetryCount() + 1;
        request.setRetryCount(retries);
        request.setLastError(errorText(exception));
        if (retries >= properties.getMaxRetries()) {
            request.setStatus(SystemRoleSyncStatus.FAILED);
            request.setNextRetryAt(null);
        } else {
            request.setStatus(SystemRoleSyncStatus.PENDING);
            long delay = Math.min(
                    properties.getInitialRetryDelayMs() * (1L << Math.min(retries - 1, 20)),
                    properties.getMaxRetryDelayMs());
            request.setNextRetryAt(Instant.now().plusMillis(delay));
        }
        requests.save(request);
        log.warn(
                "System-role sync failed: requestId={}, userId={}, role={}, retry={}, maxRetries={}",
                request.getId(),
                request.getUserId(),
                request.getSystemRole(),
                retries,
                properties.getMaxRetries(),
                exception);
    }

    private String errorText(Exception exception) {
        String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return message == null || message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}