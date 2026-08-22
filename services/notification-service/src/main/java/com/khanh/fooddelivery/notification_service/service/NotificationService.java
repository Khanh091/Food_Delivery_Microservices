package com.khanh.fooddelivery.notification_service.service;

import com.khanh.fooddelivery.notification_service.dto.request.RegisterPushDeviceRequest;
import com.khanh.fooddelivery.notification_service.dto.response.PushDeviceResponse;
import java.util.UUID;

public interface NotificationService {

    PushDeviceResponse registerDevice(UUID userId, RegisterPushDeviceRequest request);

    void deactivateDevice(UUID userId, UUID deviceId);
}
