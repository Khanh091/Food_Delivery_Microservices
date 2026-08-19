package com.khanh.fooddelivery.restaurant_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.khanh.fooddelivery.restaurant_service.client.UserServiceClient;
import com.khanh.fooddelivery.restaurant_service.config.InternalApiProperties;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantMemberManagementResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantMember;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantMemberRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class RestaurantMemberServiceImplTests {
    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @Mock private RestaurantMemberRepository members;
    @Mock private RestaurantRepository restaurants;
    @Mock private RestaurantBranchRepository branches;
    @Mock private RestaurantAuthorizationService authorization;
    @Mock private CurrentUserProvider currentUser;
    @Mock private UserServiceClient userService;
    @Mock private Jwt jwt;

    private RestaurantMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        InternalApiProperties internalApi = new InternalApiProperties();
        internalApi.setKey("internal-key");
        service = new RestaurantMemberServiceImpl(
                members,
                restaurants,
                branches,
                authorization,
                currentUser,
                userService,
                internalApi);
        when(currentUser.getCurrentUserId(jwt)).thenReturn(ACTOR_ID);
    }

    @Test
    void listEnrichesMembersWithOneBatchIdentityCallAndBranchName() {
        Restaurant restaurant = restaurant();
        RestaurantBranch branch = new RestaurantBranch();
        branch.setId(UUID.randomUUID());
        branch.setName("Quận 1");
        RestaurantMember member = member(restaurant);
        member.setBranch(branch);
        when(members.findAllByRestaurantId(eq(RESTAURANT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 20), 1));
        when(userService.batchProfiles(eq("internal-key"), any()))
                .thenReturn(
                        apiResponse(
                                List.of(
                                        new UserServiceClient.InternalUserProfileResponse(
                                                USER_ID,
                                                USER_ID,
                                                "Nguyen Van A",
                                                "a@example.com",
                                                null,
                                                null))));

        RestaurantMemberManagementResponse response =
                service.list(jwt, RESTAURANT_ID, PageRequest.of(0, 20)).getContent().getFirst();

        assertThat(response.fullName()).isEqualTo("Nguyen Van A");
        assertThat(response.email()).isEqualTo("a@example.com");
        assertThat(response.branchName()).isEqualTo("Quận 1");
        verify(userService, times(1)).batchProfiles(eq("internal-key"), any());
    }

    @Test
    void createResolvesEmailAndCreatesAnActiveMember() {
        Restaurant restaurant = restaurant();
        when(restaurants.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant));
        when(userService.resolveByEmail(eq("internal-key"), any()))
                .thenReturn(
                        apiResponse(
                                new UserServiceClient.InternalUserProfileResponse(
                                        USER_ID,
                                        USER_ID,
                                        "Nguyen Van A",
                                        "a@example.com",
                                        null,
                                        null)));
        when(members.existsByRestaurantIdAndUserIdAndBranchIdIsNull(RESTAURANT_ID, USER_ID))
                .thenReturn(false);
        when(members.save(any(RestaurantMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantMemberManagementResponse response =
                service.create(
                        jwt,
                        RESTAURANT_ID,
                        new RestaurantMemberCreateRequest(
                                "a@example.com", null, RestaurantMemberRole.STAFF));

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.status()).isEqualTo(RestaurantMemberStatus.ACTIVE);
        ArgumentCaptor<RestaurantMember> saved = ArgumentCaptor.forClass(RestaurantMember.class);
        verify(members).save(saved.capture());
        assertThat(saved.getValue().getJoinedAt()).isNotNull();
    }

    @Test
    void ownerMembershipCannotBeChangedOrRemoved() {
        RestaurantMember owner = member(restaurant());
        owner.setRole(RestaurantMemberRole.OWNER);
        when(members.findByIdAndRestaurantId(MEMBER_ID, RESTAURANT_ID)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.remove(jwt, RESTAURANT_ID, MEMBER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", ErrorCode.OWNER_MEMBER_CANNOT_BE_REMOVED);
    }

    @Test
    void memberFromAnotherRestaurantCannotBeRemoved() {
        when(members.findByIdAndRestaurantId(MEMBER_ID, RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(jwt, RESTAURANT_ID, MEMBER_ID))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESTAURANT_MEMBER_NOT_FOUND);
    }

    private Restaurant restaurant() {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(RESTAURANT_ID);
        return restaurant;
    }

    private RestaurantMember member(Restaurant restaurant) {
        RestaurantMember member = new RestaurantMember();
        member.setId(MEMBER_ID);
        member.setRestaurant(restaurant);
        member.setUserId(USER_ID);
        member.setRole(RestaurantMemberRole.STAFF);
        member.setStatus(RestaurantMemberStatus.ACTIVE);
        return member;
    }

    private <T> UserServiceClient.ApiResponse<T> apiResponse(T data) {
        return new UserServiceClient.ApiResponse<>(true, "SUCCESS", "ok", data, Instant.now());
    }
}
