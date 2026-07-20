package com.khanh.fooddelivery.restaurant_service.service;

import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationDocumentUploadRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationDocumentVerificationRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.ApplicationDocumentResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

public interface ApplicationDocumentService {

    ApplicationDocumentResponse upload(
            Jwt jwt,
            UUID applicationId,
            MultipartFile file,
            ApplicationDocumentUploadRequest request);

    List<ApplicationDocumentResponse> findAll(Jwt jwt, UUID applicationId, boolean administrator);

    void delete(Jwt jwt, UUID applicationId, UUID documentId);

    ApplicationDocumentResponse verify(Jwt jwt, UUID applicationId, UUID documentId);

    ApplicationDocumentResponse reject(
            Jwt jwt,
            UUID applicationId,
            UUID documentId,
            ApplicationDocumentVerificationRequest request);
}
