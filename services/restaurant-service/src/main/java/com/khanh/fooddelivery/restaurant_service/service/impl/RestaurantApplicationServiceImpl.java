package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationInformationRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationRejectionRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantApplicationUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantApplicationSummaryResponse;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantMember;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantPartnerApplication;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantStatusHistory;
import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType;
import com.khanh.fooddelivery.restaurant_service.enums.DocumentVerificationStatus;
import com.khanh.fooddelivery.restaurant_service.enums.PartnerApplicationStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantVerificationStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantApplicationMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantMapper;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantApplicationDocumentRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantMemberRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantPartnerApplicationRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantStatusHistoryRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantApplicationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantApplicationServiceImpl implements RestaurantApplicationService {
    private static final Set<ApplicationDocumentType> REQUIRED =
            EnumSet.of(
                    ApplicationDocumentType.BUSINESS_LICENSE,
                    ApplicationDocumentType.OWNER_ID_CARD);
    private final RestaurantPartnerApplicationRepository applications;
    private final RestaurantApplicationDocumentRepository documents;
    private final RestaurantRepository restaurants;
    private final RestaurantMemberRepository members;
    private final RestaurantStatusHistoryRepository histories;
    private final RestaurantApplicationMapper applicationMapper;
    private final RestaurantMapper restaurantMapper;
    private final CurrentUserProvider currentUser;

    public RestaurantApplicationResponse create(Jwt jwt, RestaurantApplicationCreateRequest r) {
        RestaurantPartnerApplication e = applicationMapper.toEntity(r);
        e.setApplicantUserId(currentUser.getCurrentUserId(jwt));
        e.setStatus(PartnerApplicationStatus.DRAFT);
        return applicationMapper.toResponse(applications.save(e));
    }

    @Transactional(readOnly = true)
    public List<RestaurantApplicationSummaryResponse> mine(Jwt jwt) {
        return applicationMapper.toSummaries(
                applications.findAllByApplicantUserIdOrderByCreatedAtDesc(
                        currentUser.getCurrentUserId(jwt)));
    }

    @Transactional(readOnly = true)
    public RestaurantApplicationResponse get(Jwt jwt, UUID id, boolean admin) {
        RestaurantPartnerApplication e = required(id);
        if (!admin && !e.getApplicantUserId().equals(currentUser.getCurrentUserId(jwt)))
            throw new AppException(ErrorCode.RESTAURANT_APPLICATION_ACCESS_DENIED);
        return applicationMapper.toResponse(e);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantApplicationSummaryResponse> all(Pageable p) {
        return applications.findAllByOrderByCreatedAtDesc(p).map(applicationMapper::toSummary);
    }

    public RestaurantApplicationResponse update(
            Jwt jwt, UUID id, RestaurantApplicationUpdateRequest r) {
        RestaurantPartnerApplication e = owned(jwt, id);
        requireStatus(
                e, PartnerApplicationStatus.DRAFT, PartnerApplicationStatus.NEEDS_MORE_INFORMATION);
        applicationMapper.update(r, e);
        return applicationMapper.toResponse(e);
    }

    public RestaurantApplicationResponse submit(Jwt jwt, UUID id) {
        RestaurantPartnerApplication e = owned(jwt, id);
        requireStatus(
                e, PartnerApplicationStatus.DRAFT, PartnerApplicationStatus.NEEDS_MORE_INFORMATION);
        for (ApplicationDocumentType t : REQUIRED)
            if (!documents.existsByApplicationIdAndDocumentType(id, t))
                throw new AppException(
                        ErrorCode.APPLICATION_REQUIRED_DOCUMENT_MISSING,
                        "Missing required document: " + t);
        e.setStatus(PartnerApplicationStatus.SUBMITTED);
        e.setSubmittedAt(Instant.now());
        e.setRejectionReason(null);
        return applicationMapper.toResponse(e);
    }

    public RestaurantApplicationResponse cancel(Jwt jwt, UUID id) {
        RestaurantPartnerApplication e = owned(jwt, id);
        requireStatus(
                e,
                PartnerApplicationStatus.DRAFT,
                PartnerApplicationStatus.SUBMITTED,
                PartnerApplicationStatus.NEEDS_MORE_INFORMATION);
        e.setStatus(PartnerApplicationStatus.CANCELLED);
        return applicationMapper.toResponse(e);
    }

    public RestaurantApplicationResponse startReview(Jwt jwt, UUID id) {
        RestaurantPartnerApplication e = required(id);
        requireStatus(e, PartnerApplicationStatus.SUBMITTED);
        e.setStatus(PartnerApplicationStatus.UNDER_REVIEW);
        return applicationMapper.toResponse(e);
    }

    public RestaurantApplicationResponse requestInformation(
            Jwt jwt, UUID id, ApplicationInformationRequest r) {
        RestaurantPartnerApplication e = required(id);
        requireStatus(e, PartnerApplicationStatus.UNDER_REVIEW);
        reviewed(e, jwt, r.reason());
        e.setStatus(PartnerApplicationStatus.NEEDS_MORE_INFORMATION);
        return applicationMapper.toResponse(e);
    }

    public RestaurantResponse approve(Jwt jwt, UUID id) {
        RestaurantPartnerApplication a = required(id);
        requireStatus(a, PartnerApplicationStatus.UNDER_REVIEW);
        if (restaurants.existsByPartnerApplicationId(id))
            throw new AppException(ErrorCode.APPLICATION_ALREADY_APPROVED);
        for (ApplicationDocumentType t : REQUIRED)
            if (!documents.existsByApplicationIdAndDocumentTypeAndVerificationStatus(
                    id, t, DocumentVerificationStatus.VERIFIED))
                throw new AppException(
                        ErrorCode.APPLICATION_DOCUMENT_NOT_VERIFIED,
                        "Required document is not verified: " + t);
        UUID reviewer = currentUser.getCurrentUserId(jwt);
        Restaurant restaurant = new Restaurant();
        restaurant.setOwnerUserId(a.getApplicantUserId());
        restaurant.setPartnerApplication(a);
        restaurant.setRestaurantCode(generateCode());
        restaurant.setName(a.getBusinessName());
        restaurant.setLegalName(a.getBusinessName());
        restaurant.setDescription(a.getDescription());
        restaurant.setPhoneNumber(a.getRepresentativePhone());
        restaurant.setEmail(a.getRepresentativeEmail());
        restaurant.setTaxCode(a.getTaxCode());
        restaurant.setStatus(RestaurantStatus.PENDING);
        restaurant.setVerificationStatus(RestaurantVerificationStatus.VERIFIED);
        restaurant.setAverageRating(BigDecimal.ZERO);
        restaurant = restaurants.save(restaurant);
        RestaurantMember owner = new RestaurantMember();
        owner.setRestaurant(restaurant);
        owner.setUserId(a.getApplicantUserId());
        owner.setRole(RestaurantMemberRole.OWNER);
        owner.setStatus(RestaurantMemberStatus.ACTIVE);
        owner.setJoinedAt(Instant.now());
        members.save(owner);
        RestaurantStatusHistory h = new RestaurantStatusHistory();
        h.setRestaurant(restaurant);
        h.setNewStatus(RestaurantStatus.PENDING);
        h.setReason("Created from approved partner application");
        h.setChangedByUserId(reviewer);
        histories.save(h);
        a.setStatus(PartnerApplicationStatus.APPROVED);
        reviewed(a, jwt, null);
        return restaurantMapper.toResponse(restaurant);
    }

    public RestaurantApplicationResponse reject(Jwt jwt, UUID id, ApplicationRejectionRequest r) {
        RestaurantPartnerApplication e = required(id);
        requireStatus(e, PartnerApplicationStatus.UNDER_REVIEW);
        e.setStatus(PartnerApplicationStatus.REJECTED);
        reviewed(e, jwt, r.reason());
        return applicationMapper.toResponse(e);
    }

    private RestaurantPartnerApplication owned(Jwt jwt, UUID id) {
        return applications
                .findByIdAndApplicantUserId(id, currentUser.getCurrentUserId(jwt))
                .orElseThrow(
                        () -> new AppException(ErrorCode.RESTAURANT_APPLICATION_ACCESS_DENIED));
    }

    private RestaurantPartnerApplication required(UUID id) {
        return applications
                .findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_APPLICATION_NOT_FOUND));
    }

    private void requireStatus(
            RestaurantPartnerApplication e, PartnerApplicationStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(x -> x == e.getStatus()))
            throw new AppException(
                    ErrorCode.INVALID_APPLICATION_STATUS_TRANSITION,
                    "Cannot transition application from " + e.getStatus());
    }

    private void reviewed(RestaurantPartnerApplication e, Jwt jwt, String reason) {
        e.setReviewedAt(Instant.now());
        e.setReviewedByUserId(currentUser.getCurrentUserId(jwt));
        e.setRejectionReason(reason);
    }

    private String generateCode() {
        String code;
        do {
            code =
                    "RES-"
                            + UUID.randomUUID()
                                    .toString()
                                    .replace("-", "")
                                    .substring(0, 12)
                                    .toUpperCase();
        } while (restaurants.existsByRestaurantCode(code));
        return code;
    }
}
