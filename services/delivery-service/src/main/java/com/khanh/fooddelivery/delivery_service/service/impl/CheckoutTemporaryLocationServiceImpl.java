package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.config.DeliveryCheckoutLocationProperties;
import com.khanh.fooddelivery.delivery_service.dto.request.ReverseGeocodeRequest;
import com.khanh.fooddelivery.delivery_service.dto.request.UpsertCheckoutTemporaryLocationRequest;
import com.khanh.fooddelivery.delivery_service.dto.response.CheckoutTemporaryLocationResponse;
import com.khanh.fooddelivery.delivery_service.dto.response.ReverseGeocodeResponse;
import com.khanh.fooddelivery.delivery_service.model.CheckoutTemporaryLocation;
import com.khanh.fooddelivery.delivery_service.repository.CheckoutTemporaryLocationRepository;
import com.khanh.fooddelivery.delivery_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.delivery_service.service.CheckoutTemporaryLocationService;
import com.khanh.fooddelivery.delivery_service.service.ReverseGeocodingService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CheckoutTemporaryLocationServiceImpl implements CheckoutTemporaryLocationService {
    private final CheckoutTemporaryLocationRepository repository;
    private final ReverseGeocodingService reverseGeocodingService;
    private final CurrentUserProvider currentUserProvider;
    private final DeliveryCheckoutLocationProperties properties;

    @Override
    public CheckoutTemporaryLocationResponse upsert(Jwt jwt, UUID branchId, UpsertCheckoutTemporaryLocationRequest request) {
        UUID ownerUserId = currentUserProvider.getCurrentUserId(jwt);
        // A browser candidate is re-resolved server-side; only this normalized result becomes quote authority.
        ReverseGeocodeResponse normalized = reverseGeocodingService.reverseGeocode(
                new ReverseGeocodeRequest(request.latitude(), request.longitude()));
        Instant now = Instant.now();
        CheckoutTemporaryLocation previous = repository.findCurrent(ownerUserId, branchId).orElse(null);
        CheckoutTemporaryLocation location = new CheckoutTemporaryLocation(
                previous == null ? UUID.randomUUID() : previous.id(), ownerUserId, branchId,
                normalized.formattedAddress(), normalized.addressLine(), normalized.ward(), normalized.district(), normalized.city(),
                normalized.latitude(), normalized.longitude(), previous == null ? now : previous.createdAt(), now,
                now.plus(properties.getTtl()));
        repository.save(location, properties.getTtl());
        return toResponse(location);
    }

    @Override
    public Optional<CheckoutTemporaryLocationResponse> getCurrent(Jwt jwt, UUID branchId) {
        return repository.findCurrent(currentUserProvider.getCurrentUserId(jwt), branchId).map(this::toResponse);
    }

    private CheckoutTemporaryLocationResponse toResponse(CheckoutTemporaryLocation location) {
        return new CheckoutTemporaryLocationResponse(location.id(), location.branchId(), location.formattedAddress(),
                location.addressLine(), location.ward(), location.district(), location.city(), location.latitude(),
                location.longitude(), location.expiresAt());
    }
}
