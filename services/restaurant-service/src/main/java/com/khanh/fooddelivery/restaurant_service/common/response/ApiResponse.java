package com.khanh.fooddelivery.restaurant_service.common.response;
import java.time.Instant;
public record ApiResponse<T>(boolean success,String code,String message,T data,Instant timestamp) {
    public static <T> ApiResponse<T> success(String message,T data){return new ApiResponse<>(true,"SUCCESS",message,data,Instant.now());}
    public static ApiResponse<Void> success(String message){return success(message,null);}
}
