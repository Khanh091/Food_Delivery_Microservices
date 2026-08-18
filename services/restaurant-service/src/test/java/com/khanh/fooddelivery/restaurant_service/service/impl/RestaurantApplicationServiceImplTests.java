package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.dto.request.ApplicationRejectionRequest;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantMember;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantPartnerApplication;
import com.khanh.fooddelivery.restaurant_service.enums.ApplicationDocumentType;
import com.khanh.fooddelivery.restaurant_service.enums.DocumentVerificationStatus;
import com.khanh.fooddelivery.restaurant_service.enums.PartnerApplicationStatus;
import com.khanh.fooddelivery.restaurant_service.identity.SystemRole;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantApplicationMapper;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantMapper;
import com.khanh.fooddelivery.restaurant_service.outbox.OutboxEventService;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantApplicationDocumentRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantMemberRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantPartnerApplicationRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantStatusHistoryRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.SystemRoleSyncService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestaurantApplicationServiceImplTests {
    @Mock private RestaurantPartnerApplicationRepository applications;
    @Mock private RestaurantApplicationDocumentRepository documents;
    @Mock private RestaurantRepository restaurants;
    @Mock private RestaurantMemberRepository members;
    @Mock private RestaurantStatusHistoryRepository histories;
    @Mock private RestaurantApplicationMapper applicationMapper;
    @Mock private RestaurantMapper restaurantMapper;
    @Mock private CurrentUserProvider currentUser;
    @Mock private OutboxEventService outbox;
    @Mock private SystemRoleSyncService systemRoleSync;

    private RestaurantApplicationServiceImpl service;
    private UUID applicationId;
    private UUID ownerId;
    private RestaurantPartnerApplication application;

    @BeforeEach
    void setUp() {
        service = new RestaurantApplicationServiceImpl(
                applications,
                documents,
                restaurants,
                members,
                histories,
                applicationMapper,
                restaurantMapper,
                currentUser,
                outbox,
                systemRoleSync);
        applicationId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        application = new RestaurantPartnerApplication();
        application.setId(applicationId);
        application.setApplicantUserId(ownerId);
        application.setStatus(PartnerApplicationStatus.UNDER_REVIEW);
        application.setBusinessName("Pho Xua");
        application.setBusinessAddress("1 Nguyen Trai");
        application.setCity("Ha Noi");
        application.setRepresentativePhone("0912345678");
        application.setRepresentativeEmail("owner@example.com");
        application.setDescription("Restaurant");
    }

    @Test
    void approveCommitsRestaurantAndEnqueuesRestaurantOwnerSystemRole() {
        when(applications.findById(applicationId)).thenReturn(Optional.of(application));
        when(restaurants.existsByPartnerApplicationId(applicationId)).thenReturn(false);
        when(documents.existsByApplicationIdAndDocumentTypeAndVerificationStatus(
                        applicationId, ApplicationDocumentType.BUSINESS_LICENSE, DocumentVerificationStatus.VERIFIED))
                .thenReturn(true);
        when(documents.existsByApplicationIdAndDocumentTypeAndVerificationStatus(
                        applicationId, ApplicationDocumentType.OWNER_ID_CARD, DocumentVerificationStatus.VERIFIED))
                .thenReturn(true);
        when(currentUser.getCurrentUserId(any())).thenReturn(UUID.randomUUID());
        when(restaurants.existsByRestaurantCode(any())).thenReturn(false);
        when(restaurants.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(members.save(any(RestaurantMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(histories.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(restaurantMapper.toResponse(any(Restaurant.class))).thenReturn(null);

        service.approve(null, applicationId);

        verify(systemRoleSync).enqueueGrant(ownerId, SystemRole.RESTAURANT_OWNER);
    }
}