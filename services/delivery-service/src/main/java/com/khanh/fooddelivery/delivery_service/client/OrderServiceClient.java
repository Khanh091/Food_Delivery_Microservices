package com.khanh.fooddelivery.delivery_service.client;
import org.springframework.cloud.openfeign.FeignClient; import org.springframework.web.bind.annotation.*; import org.springframework.http.HttpHeaders; import java.util.UUID;
@FeignClient(name="order-service") public interface OrderServiceClient {
 @PostMapping("/internal/v1/orders/{id}/driver-assigned") void assigned(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,@PathVariable UUID id);
 @PostMapping("/internal/v1/orders/{id}/picked-up") void pickedUp(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,@PathVariable UUID id);
 @PostMapping("/internal/v1/orders/{id}/delivered") void delivered(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,@PathVariable UUID id);
 @PostMapping("/internal/v1/orders/{id}/matching-failed") void matchingFailed(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,@PathVariable UUID id);
}
