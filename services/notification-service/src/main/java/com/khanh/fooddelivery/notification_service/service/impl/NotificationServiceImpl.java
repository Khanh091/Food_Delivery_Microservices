package com.khanh.fooddelivery.notification_service.service.impl;

import com.khanh.fooddelivery.notification_service.dto.request.RegisterPushDeviceRequest;
import com.khanh.fooddelivery.notification_service.dto.response.PushDeviceResponse;
import com.khanh.fooddelivery.notification_service.entity.PushDevice;
import com.khanh.fooddelivery.notification_service.entity.PushPlatform;
import com.khanh.fooddelivery.notification_service.mapper.PushDeviceMapper;
import com.khanh.fooddelivery.notification_service.repository.PushDeviceRepository;
import com.khanh.fooddelivery.notification_service.service.NotificationService;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final PushDeviceRepository devices;
    private final PushDeviceMapper mapper;

    @Override
    public PushDeviceResponse registerDevice(UUID userId, RegisterPushDeviceRequest request) {
        PushDevice device = devices.findByExpoPushToken(request.expoPushToken().trim())
                .orElseGet(() -> {
                    PushDevice created = new PushDevice();
                    created.setId(UUID.randomUUID());
                    created.setExpoPushToken(request.expoPushToken().trim());
                    return created;
                });
        device.setUserId(userId);
        device.setDriverId(userId);
        device.setPlatform(platform(request.platform()));
        device.setDeviceId(normalize(request.deviceId()));
        device.setActive(true);
        return mapper.toResponse(devices.save(device));
    }

    @Override
    public void deactivateDevice(UUID userId, UUID deviceId) {
        devices.findByIdAndDriverId(deviceId, userId).ifPresent(device -> device.setActive(false));
    }

    private PushPlatform platform(String value) {
        try {
            return PushPlatform.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return PushPlatform.UNKNOWN;
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
