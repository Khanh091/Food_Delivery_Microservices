package com.khanh.fooddelivery.restaurant_service.storage.exception;

import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;

public class FileStorageException extends AppException {

    public FileStorageException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileStorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
