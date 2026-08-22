package com.khanh.fooddelivery.notification_service.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.notification_service.client.ExpoPushClient;
import com.khanh.fooddelivery.notification_service.entity.NotificationDelivery;
import com.khanh.fooddelivery.notification_service.entity.NotificationDeliveryStatus;
import com.khanh.fooddelivery.notification_service.entity.PushDevice;
import com.khanh.fooddelivery.notification_service.event.DeliveryOfferCreatedEvent;
import com.khanh.fooddelivery.notification_service.repository.PushDeviceRepository;
import com.khanh.fooddelivery.notification_service.service.NotificationDeliveryStateService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverOfferNotificationServiceImplTests {

    private static final UUID EVENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OFFER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DELIVERY_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID DRIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Mock
    private NotificationDeliveryStateService states;
    @Mock
    private PushDeviceRepository devices;
    @Mock
    private ExpoPushClient expo;

    private DriverOfferNotificationServiceImpl service;
    private DeliveryOfferCreatedEvent event;

    @BeforeEach
    void setUp() {
        service = new DriverOfferNotificationServiceImpl(states, devices, expo);
        event = new DeliveryOfferCreatedEvent(
                EVENT_ID,
                DeliveryOfferCreatedEvent.EVENT_TYPE,
                Instant.parse("2026-08-23T10:15:30Z"),
                OFFER_ID,
                DeliveryOfferCreatedEvent.VERSION,
                new DeliveryOfferCreatedEvent.Payload(
                        OFFER_ID,
                        DELIVERY_ID,
                        DRIVER_ID,
                        Instant.parse("2026-08-23T10:16:15Z")
                )
        );
    }

    @Test
    void validOfferSendsPushAndMarksNotificationSent() {
        NotificationDelivery notification = notification(NotificationDeliveryStatus.PENDING);
        PushDevice device = device();
        when(states.prepare(event)).thenReturn(notification);
        when(devices.findByDriverIdAndActiveTrue(DRIVER_ID)).thenReturn(List.of(device));
        when(expo.send(eq(device.getExpoPushToken()), any(), any(), anyMap()))
                .thenReturn(ExpoPushClient.DeliveryResult.SENT);

        service.notifyDriverOffer(event);

        verify(expo).send(eq(device.getExpoPushToken()), eq("Chuyến giao mới"), any(), anyMap());
        verify(states).markSent(notification.getId());
    }

    @Test
    void duplicateResolvedEventDoesNotCallExpoAgain() {
        NotificationDelivery notification = notification(NotificationDeliveryStatus.SENT);
        when(states.prepare(event)).thenReturn(notification);

        service.notifyDriverOffer(event);

        verifyNoInteractions(devices, expo);
        verify(states).prepare(event);
    }

    @Test
    void expiredEventDoesNotCallExpo() {
        NotificationDelivery notification = notification(NotificationDeliveryStatus.SKIPPED);
        when(states.prepare(event)).thenReturn(notification);

        service.notifyDriverOffer(event);

        verifyNoInteractions(devices, expo);
    }

    @Test
    void transientExpoFailureMarksFailedAndIsRetriedByKafka() {
        NotificationDelivery notification = notification(NotificationDeliveryStatus.PENDING);
        PushDevice device = device();
        when(states.prepare(event)).thenReturn(notification);
        when(devices.findByDriverIdAndActiveTrue(DRIVER_ID)).thenReturn(List.of(device));
        when(expo.send(any(), any(), any(), anyMap()))
                .thenReturn(ExpoPushClient.DeliveryResult.TRANSIENT_FAILURE);

        assertThatThrownBy(() -> service.notifyDriverOffer(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Expo push temporarily unavailable");

        verify(states).markFailed(eq(notification.getId()), any(Throwable.class));
        verify(states, never()).markSent(notification.getId());
    }

    @Test
    void failedStateIsStillEligibleForAnotherPushAttempt() {
        NotificationDelivery notification = notification(NotificationDeliveryStatus.FAILED);
        PushDevice device = device();
        when(states.prepare(event)).thenReturn(notification);
        when(devices.findByDriverIdAndActiveTrue(DRIVER_ID)).thenReturn(List.of(device));
        when(expo.send(any(), any(), any(), anyMap()))
                .thenReturn(ExpoPushClient.DeliveryResult.SENT);

        service.notifyDriverOffer(event);

        verify(expo).send(eq(device.getExpoPushToken()), any(), any(), anyMap());
        verify(states).markSent(notification.getId());
    }

    @Test
    void missingPushDeviceIsAnIntentionalSkip() {
        NotificationDelivery notification = notification(NotificationDeliveryStatus.PENDING);
        when(states.prepare(event)).thenReturn(notification);
        when(devices.findByDriverIdAndActiveTrue(DRIVER_ID)).thenReturn(List.of());

        service.notifyDriverOffer(event);

        verify(states).markSkipped(notification.getId(), "No active push device");
        verifyNoInteractions(expo);
    }

    @Test
    void invalidTokenIsDeactivatedAndDoesNotRetryForever() {
        NotificationDelivery notification = notification(NotificationDeliveryStatus.PENDING);
        PushDevice device = device();
        when(states.prepare(event)).thenReturn(notification);
        when(devices.findByDriverIdAndActiveTrue(DRIVER_ID)).thenReturn(List.of(device));
        when(expo.send(any(), any(), any(), anyMap()))
                .thenReturn(ExpoPushClient.DeliveryResult.INVALID_TOKEN);

        service.notifyDriverOffer(event);

        verify(devices).save(device);
        verify(states).markSkipped(notification.getId(), "No valid active push device");
    }

    private NotificationDelivery notification(NotificationDeliveryStatus status) {
        NotificationDelivery notification = new NotificationDelivery();
        notification.setId(UUID.randomUUID());
        notification.setSourceEventId(EVENT_ID);
        notification.setOfferId(OFFER_ID);
        notification.setDeliveryId(DELIVERY_ID);
        notification.setDriverId(DRIVER_ID);
        notification.setExpiresAt(event.payload().expiresAt());
        notification.setStatus(status);
        return notification;
    }

    private PushDevice device() {
        PushDevice device = new PushDevice();
        device.setId(UUID.randomUUID());
        device.setDriverId(DRIVER_ID);
        device.setUserId(DRIVER_ID);
        device.setExpoPushToken("ExponentPushToken[driver]");
        device.setActive(true);
        return device;
    }
}
