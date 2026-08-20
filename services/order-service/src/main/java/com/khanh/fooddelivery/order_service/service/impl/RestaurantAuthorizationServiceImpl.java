package com.khanh.fooddelivery.order_service.service.impl;

import com.khanh.fooddelivery.order_service.client.RestaurantServiceClient;
import com.khanh.fooddelivery.order_service.client.dto.response.OrderAuthorizationResponse;
import com.khanh.fooddelivery.order_service.common.response.ApiResponse;
import com.khanh.fooddelivery.order_service.exception.AppException;
import com.khanh.fooddelivery.order_service.exception.ErrorCode;
import com.khanh.fooddelivery.order_service.security.CurrentBearerTokenProvider;
import com.khanh.fooddelivery.order_service.service.RestaurantAuthorizationService;
import feign.FeignException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestaurantAuthorizationServiceImpl implements RestaurantAuthorizationService {
    private final RestaurantServiceClient restaurants;
    private final CurrentBearerTokenProvider bearer;

    @Override
    public void requireAccess(UUID restaurantId, UUID branchId) {
        try {
            ApiResponse<OrderAuthorizationResponse> response = restaurants.orderAuthorization(
                    bearer.getBearerToken(), restaurantId, branchId);
            if (response == null || !response.success() || response.data() == null || !response.data().authorized()) {
                throw new AppException(ErrorCode.ORDER_ACCESS_DENIED);
            }
        } catch (FeignException exception) {
            throw new AppException(ErrorCode.RESTAURANT_SERVICE_UNAVAILABLE);
        }
    }
}
