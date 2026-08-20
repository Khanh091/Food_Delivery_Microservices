package com.khanh.fooddelivery.delivery_service.client;

import com.khanh.fooddelivery.delivery_service.client.dto.response.NearestDriverResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "tracking-service")
public interface TrackingServiceClient {

    @GetMapping("/internal/v1/tracking/drivers/nearest")
    List<NearestDriverResponse> nearest(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam double radiusMeters,
            @RequestParam long limit
    );
}
