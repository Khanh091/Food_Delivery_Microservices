package com.khanh.fooddelivery.restaurant_service.dto.response; import com.khanh.fooddelivery.restaurant_service.enums.PartnerApplicationStatus; import java.time.Instant; import java.util.UUID;
public record RestaurantApplicationSummaryResponse(UUID id,String businessName,PartnerApplicationStatus status,String city,Instant submittedAt,Instant createdAt){}
