package com.khanh.fooddelivery.driver_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.driver_service.client.UserSystemRoleClient;
import com.khanh.fooddelivery.driver_service.dto.request.DriverAvailabilityRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverRegistrationRequest;
import com.khanh.fooddelivery.driver_service.dto.request.DriverStatusUpdateRequest;
import com.khanh.fooddelivery.driver_service.entity.DriverAvailability;
import com.khanh.fooddelivery.driver_service.entity.DriverProfile;
import com.khanh.fooddelivery.driver_service.mapper.DriverMapper;
import com.khanh.fooddelivery.driver_service.model.DriverStatus;
import com.khanh.fooddelivery.driver_service.model.VehicleType;
import com.khanh.fooddelivery.driver_service.repository.DriverAvailabilityRepository;
import com.khanh.fooddelivery.driver_service.repository.DriverProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DriverServiceImplTests {

    private static final UUID DRIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID OTHER_DRIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final UUID DELIVERY_ID = UUID.fromString("00000000-0000-0000-0000-000000000203");
    private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000204");
    private static final UUID KEYCLOAK_SUB = UUID.fromString("00000000-0000-0000-0000-000000000205");

    @Mock
    private DriverAvailabilityRepository availability;
    @Mock
    private DriverProfileRepository profiles;
    @Mock
    private UserSystemRoleClient systemRoles;

    private DriverServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DriverServiceImpl(
                availability,
                profiles,
                systemRoles,
                Mappers.getMapper(DriverMapper.class)
        );
        ReflectionTestUtils.setField(service, "internalApiKey", "test-key");
    }

    @Test
    void available_returns_only_active_profiles_from_the_free_online_repository_query() {
        DriverAvailability inactive = row(OTHER_DRIVER_ID, true, null, null);
        DriverAvailability active = row(DRIVER_ID, true, null, null);
        when(availability
                .findByAvailableTrueAndActiveDeliveryIdIsNullAndPendingOfferDeliveryIdIsNullOrderByUpdatedAtAsc())
                .thenReturn(List.of(inactive, active));
        when(profiles.findByUserId(OTHER_DRIVER_ID)).thenReturn(Optional.of(profile(OTHER_DRIVER_ID, DriverStatus.PENDING)));
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.of(profile(DRIVER_ID, DriverStatus.ACTIVE)));

        assertThat(service.available()).containsExactly(DRIVER_ID);
        verify(availability)
                .findByAvailableTrueAndActiveDeliveryIdIsNullAndPendingOfferDeliveryIdIsNullOrderByUpdatedAtAsc();
    }

    @Test
    void inactive_driver_cannot_go_online() {
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.of(profile(DRIVER_ID, DriverStatus.PENDING)));

        assertThatThrownBy(() -> service.setAvailability(DRIVER_ID, new DriverAvailabilityRequest(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver is not active");
        verify(availability, never()).findByUserIdForUpdate(DRIVER_ID);
    }

    @Test
    void profile_lookup_returns_current_driver_profile() {
        DriverProfile profile = profile(DRIVER_ID, DriverStatus.ACTIVE);
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.of(profile));

        assertThat(service.profile(DRIVER_ID)).get()
                .extracting(response -> response.userId(), response -> response.status())
                .containsExactly(DRIVER_ID, DriverStatus.ACTIVE);
    }

    @Test
    void registration_persists_the_selected_vehicle_enum() {
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.empty());
        when(profiles.save(any(DriverProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(
                DRIVER_ID,
                new DriverRegistrationRequest(VehicleType.ELECTRIC_BICYCLE, "29C127836")
        );

        assertThat(response.vehicleType()).isEqualTo(VehicleType.ELECTRIC_BICYCLE);
        assertThat(response.vehiclePlate()).isEqualTo("29C127836");

        ArgumentCaptor<DriverProfile> saved = ArgumentCaptor.forClass(DriverProfile.class);
        verify(profiles).save(saved.capture());
        assertThat(saved.getValue().getUserId())
                .isEqualTo(DRIVER_ID)
                .isNotEqualTo(KEYCLOAK_SUB);
    }

    @Test
    void status_update_accepts_profile_id_and_grants_role_to_canonical_user() {
        DriverProfile profile = profile(DRIVER_ID, DriverStatus.PENDING);
        profile.setId(PROFILE_ID);
        when(profiles.findById(PROFILE_ID)).thenReturn(Optional.of(profile));

        service.setStatus(PROFILE_ID, new DriverStatusUpdateRequest(DriverStatus.ACTIVE));

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.ACTIVE);
        verify(systemRoles).grantDriverRole(DRIVER_ID, "test-key");
    }

    @Test
    void status_update_keeps_user_id_compatibility() {
        DriverProfile profile = profile(DRIVER_ID, DriverStatus.PENDING);
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.of(profile));

        service.setStatus(DRIVER_ID, new DriverStatusUpdateRequest(DriverStatus.ACTIVE));

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.ACTIVE);
        verify(systemRoles).grantDriverRole(DRIVER_ID, "test-key");
    }

    @Test
    void status_update_keeps_profile_pending_when_role_grant_fails() {
        DriverProfile profile = profile(DRIVER_ID, DriverStatus.PENDING);
        profile.setId(PROFILE_ID);
        when(profiles.findById(PROFILE_ID)).thenReturn(Optional.of(profile));
        doThrow(new IllegalStateException("role grant failed"))
                .when(systemRoles)
                .grantDriverRole(DRIVER_ID, "test-key");

        assertThatThrownBy(() -> service.setStatus(
                PROFILE_ID,
                new DriverStatusUpdateRequest(DriverStatus.ACTIVE)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("role grant failed");

        assertThat(profile.getStatus()).isEqualTo(DriverStatus.PENDING);
    }

    @Test
    void availability_lookup_returns_current_reservation_state() {
        DriverAvailability row = row(DRIVER_ID, false, DELIVERY_ID, null);
        when(availability.findByUserId(DRIVER_ID)).thenReturn(Optional.of(row));

        assertThat(service.availability(DRIVER_ID)).get()
                .extracting(response -> response.userId(), response -> response.activeDeliveryId())
                .containsExactly(DRIVER_ID, DELIVERY_ID);
    }

    @Test
    void active_driver_can_go_online() {
        DriverAvailability row = row(DRIVER_ID, false, null, null);
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.of(profile(DRIVER_ID, DriverStatus.ACTIVE)));
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(row));
        when(availability.save(row)).thenReturn(row);

        var response = service.setAvailability(DRIVER_ID, new DriverAvailabilityRequest(true));

        assertThat(row.isAvailable()).isTrue();
        assertThat(response.available()).isTrue();
    }

    @Test
    void cannot_go_online_with_active_delivery_or_pending_offer() {
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.of(profile(DRIVER_ID, DriverStatus.ACTIVE)));

        DriverAvailability busy = row(DRIVER_ID, false, UUID.randomUUID(), null);
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(busy));
        assertThatThrownBy(() -> service.setAvailability(DRIVER_ID, new DriverAvailabilityRequest(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver has an active delivery or pending offer");

        DriverAvailability reserved = row(DRIVER_ID, true, null, UUID.randomUUID());
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(reserved));
        assertThatThrownBy(() -> service.setAvailability(DRIVER_ID, new DriverAvailabilityRequest(true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver has an active delivery or pending offer");
    }

    @Test
    void reserve_offer_sets_pending_reservation() {
        DriverAvailability row = row(DRIVER_ID, true, null, null);
        activeProfile();
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(row));

        service.reserveOffer(DRIVER_ID, DELIVERY_ID);

        assertThat(row.getPendingOfferDeliveryId()).isEqualTo(DELIVERY_ID);
        assertThat(row.getActiveDeliveryId()).isNull();
    }

    @Test
    void offline_driver_cannot_reserve_offer() {
        DriverAvailability row = row(DRIVER_ID, false, null, null);
        activeProfile();
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.reserveOffer(DRIVER_ID, DELIVERY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver unavailable");
    }

    @Test
    void busy_or_reserved_driver_cannot_reserve_another_offer() {
        activeProfile();
        DriverAvailability busy = row(DRIVER_ID, true, UUID.randomUUID(), null);
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(busy));
        assertThatThrownBy(() -> service.reserveOffer(DRIVER_ID, DELIVERY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver unavailable");

        DriverAvailability reserved = row(DRIVER_ID, true, null, UUID.randomUUID());
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(reserved));
        assertThatThrownBy(() -> service.reserveOffer(DRIVER_ID, DELIVERY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver unavailable");
    }

    @Test
    void accept_offer_converts_pending_reservation_to_active_delivery() {
        DriverAvailability row = row(DRIVER_ID, true, null, DELIVERY_ID);
        activeProfile();
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(row));

        service.acceptOffer(DRIVER_ID, DELIVERY_ID);

        assertThat(row.getPendingOfferDeliveryId()).isNull();
        assertThat(row.getActiveDeliveryId()).isEqualTo(DELIVERY_ID);
        assertThat(row.isAvailable()).isFalse();
    }

    @Test
    void accept_offer_cannot_replace_an_existing_active_delivery() {
        DriverAvailability row = row(DRIVER_ID, true, OTHER_DRIVER_ID, DELIVERY_ID);
        activeProfile();
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.acceptOffer(DRIVER_ID, DELIVERY_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Driver offer is no longer active");

        assertThat(row.getActiveDeliveryId()).isEqualTo(OTHER_DRIVER_ID);
        assertThat(row.getPendingOfferDeliveryId()).isEqualTo(DELIVERY_ID);
    }

    @Test
    void release_pending_offer_clears_only_matching_reservation() {
        DriverAvailability row = row(DRIVER_ID, true, null, DELIVERY_ID);
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(row));

        service.releaseOffer(DRIVER_ID, DELIVERY_ID);

        assertThat(row.getPendingOfferDeliveryId()).isNull();
    }

    @Test
    void release_delivery_clears_active_delivery_and_restores_active_driver_online() {
        DriverAvailability row = row(DRIVER_ID, false, DELIVERY_ID, null);
        activeProfile();
        when(availability.findByUserIdForUpdate(DRIVER_ID)).thenReturn(Optional.of(row));

        service.releaseDelivery(DRIVER_ID, DELIVERY_ID);

        assertThat(row.getActiveDeliveryId()).isNull();
        assertThat(row.isAvailable()).isTrue();
    }

    private void activeProfile() {
        when(profiles.findByUserId(DRIVER_ID)).thenReturn(Optional.of(profile(DRIVER_ID, DriverStatus.ACTIVE)));
    }

    private DriverProfile profile(UUID userId, DriverStatus status) {
        DriverProfile profile = new DriverProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(userId);
        profile.setStatus(status);
        return profile;
    }

    private DriverAvailability row(
            UUID userId,
            boolean online,
            UUID activeDeliveryId,
            UUID pendingOfferDeliveryId
    ) {
        DriverAvailability row = new DriverAvailability();
        row.setId(UUID.randomUUID());
        row.setUserId(userId);
        row.setAvailable(online);
        row.setActiveDeliveryId(activeDeliveryId);
        row.setPendingOfferDeliveryId(pendingOfferDeliveryId);
        return row;
    }
}
