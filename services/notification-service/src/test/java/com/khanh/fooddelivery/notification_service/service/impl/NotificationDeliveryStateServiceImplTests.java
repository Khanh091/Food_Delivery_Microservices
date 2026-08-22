package com.khanh.fooddelivery.notification_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.notification_service.entity.NotificationDelivery;
import com.khanh.fooddelivery.notification_service.entity.NotificationDeliveryStatus;
import com.khanh.fooddelivery.notification_service.event.DeliveryLifecycleEvent;
import com.khanh.fooddelivery.notification_service.repository.NotificationDeliveryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryStateServiceImplTests {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID OFFER_ID = UUID.randomUUID();
    private static final UUID DELIVERY_ID = UUID.randomUUID();
    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-08-23T10:15:30Z");

    @Mock
    private NotificationDeliveryRepository notifications;

    private NotificationDeliveryStateServiceImpl states;
    private DeliveryLifecycleEvent event;

    @BeforeEach
    void setUp() {
        states = new NotificationDeliveryStateServiceImpl(
                notifications,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        event = event(NOW.plusSeconds(45));
    }

    @Test
    void firstDeliveryCreatesOnePendingRecordAndIncrementsAttempt() {
        when(notifications.findBySourceEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(notifications.save(any(NotificationDelivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDelivery prepared = states.prepare(event);

        assertThat(prepared.getSourceEventId()).isEqualTo(EVENT_ID);
        assertThat(prepared.getOfferId()).isEqualTo(OFFER_ID);
        assertThat(prepared.getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
        assertThat(prepared.getAttemptCount()).isEqualTo(1);
        verify(notifications).save(any(NotificationDelivery.class));
    }

    @Test
    void duplicateSentEventIsAReadOnlyNoOp() {
        NotificationDelivery existing = notification(NotificationDeliveryStatus.SENT, event.payload().expiresAt());
        when(notifications.findBySourceEventId(EVENT_ID)).thenReturn(Optional.of(existing));

        NotificationDelivery prepared = states.prepare(event);

        assertThat(prepared.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(prepared.getAttemptCount()).isZero();
    }

    @Test
    void expiredEventIsMarkedSkippedWithoutAttempt() {
        DeliveryLifecycleEvent expired = event(NOW);
        when(notifications.findBySourceEventId(EVENT_ID)).thenReturn(Optional.empty());
        when(notifications.save(any(NotificationDelivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationDelivery prepared = states.prepare(expired);

        assertThat(prepared.getStatus()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
        assertThat(prepared.getAttemptCount()).isZero();
    }

    @Test
    void sameEventCannotChangeItsBusinessIdentity() {
        NotificationDelivery existing = notification(NotificationDeliveryStatus.PENDING, event.payload().expiresAt());
        existing.setDriverId(UUID.randomUUID());
        when(notifications.findBySourceEventId(EVENT_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> states.prepare(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity changed");
    }

    private DeliveryLifecycleEvent event(Instant expiresAt) {
        return new DeliveryLifecycleEvent(
                EVENT_ID,
                DeliveryLifecycleEvent.DELIVERY_OFFER_CREATED,
                NOW,
                OFFER_ID,
                DeliveryLifecycleEvent.VERSION,
                new DeliveryLifecycleEvent.Payload(OFFER_ID, DELIVERY_ID, DRIVER_ID, expiresAt)
        );
    }

    private NotificationDelivery notification(NotificationDeliveryStatus status, Instant expiresAt) {
        NotificationDelivery notification = new NotificationDelivery();
        notification.setId(UUID.randomUUID());
        notification.setSourceEventId(EVENT_ID);
        notification.setOfferId(OFFER_ID);
        notification.setDeliveryId(DELIVERY_ID);
        notification.setDriverId(DRIVER_ID);
        notification.setExpiresAt(expiresAt);
        notification.setStatus(status);
        notification.setAttemptCount(0);
        return notification;
    }
}
