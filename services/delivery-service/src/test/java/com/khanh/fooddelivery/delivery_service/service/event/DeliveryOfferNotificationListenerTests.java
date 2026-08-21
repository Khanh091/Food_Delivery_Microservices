package com.khanh.fooddelivery.delivery_service.service.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;

import com.khanh.fooddelivery.delivery_service.client.NotificationServiceClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class DeliveryOfferNotificationListenerTests {

    @Test
    void notificationFailureDoesNotEscapeAfterOfferCommit() {
        NotificationServiceClient client = Mockito.mock(NotificationServiceClient.class);
        doThrow(new IllegalStateException("push unavailable"))
                .when(client)
                .notifyDriverOffer(Mockito.anyString(), Mockito.any());
        DeliveryOfferNotificationListener listener = new DeliveryOfferNotificationListener(client);
        ReflectionTestUtils.setField(listener, "internalApiKey", "internal-test-key");

        assertThatCode(() -> listener.notifyDriver(new DeliveryOfferCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        ))).doesNotThrowAnyException();
    }
}
