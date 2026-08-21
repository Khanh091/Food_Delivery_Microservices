package com.khanh.fooddelivery.notification_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.notification_service.client.ExpoPushClient;
import com.khanh.fooddelivery.notification_service.dto.request.DriverOfferNotificationRequest;
import com.khanh.fooddelivery.notification_service.dto.request.RegisterPushDeviceRequest;
import com.khanh.fooddelivery.notification_service.entity.PushDevice;
import com.khanh.fooddelivery.notification_service.mapper.PushDeviceMapper;
import com.khanh.fooddelivery.notification_service.repository.PushDeviceRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTests {

    private static final UUID DRIVER_A = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID DRIVER_B = UUID.fromString("00000000-0000-0000-0000-0000000000bb");

    @Mock
    private PushDeviceRepository devices;
    @Mock
    private ExpoPushClient expo;

    private final PushDeviceMapper mapper = Mappers.getMapper(PushDeviceMapper.class);
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(devices, mapper, expo);
    }

    @Test
    void registeringTheSameTokenReassignsItToTheCurrentDriver() {
        PushDevice device = device(DRIVER_A, true);
        when(devices.findByExpoPushToken("ExponentPushToken[test]"))
                .thenReturn(Optional.of(device));
        when(devices.save(device)).thenReturn(device);

        var response = service.registerDevice(
                DRIVER_B,
                new RegisterPushDeviceRequest("ExponentPushToken[test]", "android", null)
        );

        assertThat(device.getDriverId()).isEqualTo(DRIVER_B);
        assertThat(device.isActive()).isTrue();
        assertThat(response.id()).isEqualTo(device.getId());
        verify(devices).save(device);
    }

    @Test
    void offerNotificationTargetsOnlyTheIntendedDriverDevices() {
        PushDevice target = device(DRIVER_A, true);
        when(devices.findByDriverIdAndActiveTrue(DRIVER_A)).thenReturn(List.of(target));
        when(expo.send(eq("ExponentPushToken[target]"), any(), any(), any()))
                .thenReturn(ExpoPushClient.DeliveryResult.SENT);

        service.notifyDriverOffer(new DriverOfferNotificationRequest(
                DRIVER_A,
                UUID.randomUUID(),
                UUID.randomUUID()
        ));

        verify(expo).send(eq("ExponentPushToken[target]"), eq("Chuyến giao mới"), any(), any());
        verify(devices, never()).findByDriverIdAndActiveTrue(DRIVER_B);
    }

    @Test
    void invalidExpoTokenIsDeactivatedWithoutASecondRetryLoop() {
        PushDevice target = device(DRIVER_A, true);
        when(devices.findByDriverIdAndActiveTrue(DRIVER_A)).thenReturn(List.of(target));
        when(expo.send(any(), any(), any(), any()))
                .thenReturn(ExpoPushClient.DeliveryResult.INVALID_TOKEN);

        service.notifyDriverOffer(new DriverOfferNotificationRequest(
                DRIVER_A,
                UUID.randomUUID(),
                UUID.randomUUID()
        ));

        assertThat(target.isActive()).isFalse();
    }

    private PushDevice device(UUID driverId, boolean active) {
        PushDevice device = new PushDevice();
        device.setId(UUID.randomUUID());
        device.setDriverId(driverId);
        device.setUserId(driverId);
        device.setExpoPushToken(driverId.equals(DRIVER_A)
                ? "ExponentPushToken[target]"
                : "ExponentPushToken[other]");
        device.setActive(active);
        return device;
    }
}
