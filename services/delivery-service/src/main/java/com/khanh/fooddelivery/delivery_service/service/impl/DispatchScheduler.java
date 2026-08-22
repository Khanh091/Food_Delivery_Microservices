package com.khanh.fooddelivery.delivery_service.service.impl;

import com.khanh.fooddelivery.delivery_service.model.DeliveryStatus;
import com.khanh.fooddelivery.delivery_service.repository.DeliveryRepository;
import com.khanh.fooddelivery.delivery_service.service.DriverDispatchService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DispatchScheduler {

    private final DeliveryRepository deliveries;
    private final DriverDispatchService dispatch;

    @Scheduled(fixedDelayString = "${delivery.dispatch.scheduler-delay:5000}")
    public void dispatchDueDeliveries() {
        for (var delivery : deliveries.findDueForDispatch(DeliveryStatus.MATCHING, Instant.now())) {
            try {
                // dispatch() takes a pessimistic lock, so duplicate scheduler
                // invocations cannot create two pending offers.
                dispatch.dispatch(delivery.getId());
            } catch (RuntimeException exception) {
                log.warn("Dispatch scheduler could not process deliveryId={}", delivery.getId(), exception);
            }
        }
    }
}
