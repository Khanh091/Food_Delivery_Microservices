package com.khanh.fooddelivery.restaurant_service.controller;

import com.khanh.fooddelivery.restaurant_service.common.response.ApiResponse;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationDocumentUploadRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationDocumentVerificationRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationInformationRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationRejectionRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.ApplicationDocumentResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationSummaryResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import com.khanh.fooddelivery.restaurant_service.service.ApplicationDocumentService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/restaurant-applications")
@RequiredArgsConstructor
public class RestaurantApplicationController {
    private final RestaurantApplicationService service;
    private final ApplicationDocumentService documentService;

    @PostMapping
    public ResponseEntity<ApiResponse<RestaurantApplicationResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RestaurantApplicationCreateRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Restaurant application created successfully",
                                service.create(jwt, r)));
    }

    @GetMapping("/me")
    public ApiResponse<List<RestaurantApplicationSummaryResponse>> mine(
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(
                "Restaurant applications retrieved successfully", service.mine(jwt));
    }

    @GetMapping("/{id}")
    public ApiResponse<RestaurantApplicationResponse> get(
            @AuthenticationPrincipal Jwt jwt, Authentication a, @PathVariable UUID id) {
        return ApiResponse.success(
                "Restaurant application retrieved successfully", service.get(jwt, id, isAdmin(a)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<Page<RestaurantApplicationSummaryResponse>> all(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable p) {
        return ApiResponse.success(
                "Restaurant applications retrieved successfully", service.all(p));
    }

    @PatchMapping("/{id}")
    public ApiResponse<RestaurantApplicationResponse> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantApplicationUpdateRequest r) {
        return ApiResponse.success(
                "Restaurant application updated successfully", service.update(jwt, id, r));
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<RestaurantApplicationResponse> submit(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(
                "Restaurant application submitted successfully", service.submit(jwt, id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<RestaurantApplicationResponse> cancel(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(
                "Restaurant application cancelled successfully", service.cancel(jwt, id));
    }

    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<RestaurantApplicationResponse> review(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success("Application review started", service.startReview(jwt, id));
    }

    @PostMapping("/{id}/request-information")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<RestaurantApplicationResponse> info(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationInformationRequest r) {
        return ApiResponse.success(
                "Additional information requested", service.requestInformation(jwt, id, r));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<RestaurantResponse> approve(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(
                "Restaurant application approved successfully", service.approve(jwt, id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<RestaurantApplicationResponse> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationRejectionRequest r) {
        return ApiResponse.success("Restaurant application rejected", service.reject(jwt, id, r));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ApplicationDocumentResponse>> addDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @Valid @RequestPart("metadata") ApplicationDocumentUploadRequest metadata) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Application document uploaded successfully",
                                documentService.upload(jwt, id, file, metadata)));
    }

    @GetMapping("/{id}/documents")
    public ApiResponse<List<ApplicationDocumentResponse>> documents(
            @AuthenticationPrincipal Jwt jwt, Authentication a, @PathVariable UUID id) {
        return ApiResponse.success(
                "Application documents retrieved successfully",
                documentService.findAll(jwt, id, isAdmin(a)));
    }

    @DeleteMapping("/{id}/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID documentId) {
        documentService.delete(jwt, id, documentId);
        return ApiResponse.success("Application document deleted successfully");
    }

    @PostMapping("/{id}/documents/{documentId}/verify")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<ApplicationDocumentResponse> verify(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID documentId) {
        return ApiResponse.success(
                "Application document verified", documentService.verify(jwt, id, documentId));
    }

    @PostMapping("/{id}/documents/{documentId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT')")
    public ApiResponse<ApplicationDocumentResponse> rejectDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID documentId,
            @Valid @RequestBody ApplicationDocumentVerificationRequest r) {
        return ApiResponse.success(
                "Application document rejected", documentService.reject(jwt, id, documentId, r));
    }

    private boolean isAdmin(Authentication a) {
        return a.getAuthorities().stream()
                .anyMatch(
                        x ->
                                x.getAuthority().equals("ROLE_ADMIN")
                                        || x.getAuthority().equals("ROLE_SUPPORT"));
    }
}
