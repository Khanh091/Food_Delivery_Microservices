package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.client.UserServiceClient;
import com.khanh.fooddelivery.restaurant_service.config.InternalApiProperties;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberUpdateRequest;
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
import com.khanh.fooddelivery.restaurant_service.service.RestaurantMemberService;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import feign.FeignException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantMemberServiceImpl implements RestaurantMemberService {
    private final RestaurantMemberRepository members;
    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;
    private final RestaurantAuthorizationService auth;
    private final CurrentUserProvider current;
    private final UserServiceClient userService;
    private final InternalApiProperties internalApi;

    public RestaurantMemberManagementResponse create(Jwt jwt, UUID rid, RestaurantMemberCreateRequest r) {
        UUID actor = current.getCurrentUserId(jwt);
        auth.requireRestaurantAccess(
                rid, actor, RestaurantMemberRole.OWNER, RestaurantMemberRole.MANAGER);
        if (r.role() == RestaurantMemberRole.OWNER)
            throw new AppException(
                    ErrorCode.INVALID_MEMBER_SCOPE,
                    "OWNER is created only during application approval");
        Restaurant restaurant =
                restaurants
                        .findById(rid)
                        .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND));
        UserServiceClient.InternalUserProfileResponse user = resolveUser(r.email());
        RestaurantBranch branch = branch(rid, r.branchId());
        if (branch != null) {
            if (members.existsByRestaurantIdAndBranchIdAndUserId(rid, branch.getId(), user.userId()))
                duplicate();
        } else if (members.existsByRestaurantIdAndUserIdAndBranchIdIsNull(rid, user.userId()))
            duplicate();
        RestaurantMember m = new RestaurantMember();
        m.setRestaurant(restaurant);
        m.setBranch(branch);
        m.setUserId(user.userId());
        m.setRole(r.role());
        m.setStatus(RestaurantMemberStatus.ACTIVE);
        m.setInvitedByUserId(actor);
        m.setJoinedAt(Instant.now());
        return response(members.save(m), user);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantMemberManagementResponse> list(Jwt jwt, UUID rid, Pageable p) {
        auth.requireRestaurantAccess(
                rid,
                current.getCurrentUserId(jwt),
                RestaurantMemberRole.OWNER,
                RestaurantMemberRole.MANAGER);
        Page<RestaurantMember> page = members.findAllByRestaurantId(rid, p);
        Map<UUID, UserServiceClient.InternalUserProfileResponse> profiles =
                profiles(page.getContent());
        return page.map(member -> response(member, profiles.get(member.getUserId())));
    }

    public RestaurantMemberManagementResponse update(
            Jwt jwt, UUID rid, UUID mid, RestaurantMemberUpdateRequest r) {
        auth.requireRestaurantAccess(
                rid,
                current.getCurrentUserId(jwt),
                RestaurantMemberRole.OWNER,
                RestaurantMemberRole.MANAGER);
        RestaurantMember m = member(rid, mid);
        if (m.getRole() == RestaurantMemberRole.OWNER)
            throw new AppException(ErrorCode.OWNER_MEMBER_CANNOT_BE_CHANGED);
        if (r.role() == RestaurantMemberRole.OWNER)
            throw new AppException(ErrorCode.INVALID_MEMBER_SCOPE);
        if (r.role() != null) m.setRole(r.role());
        if (Boolean.TRUE.equals(r.updateBranchScope())) {
            RestaurantBranch updatedBranch = branch(rid, r.branchId());
            ensureScopeAvailable(m, rid, updatedBranch);
            m.setBranch(updatedBranch);
        }
        if (r.status() != null) {
            if (r.status() == RestaurantMemberStatus.REMOVED)
                throw new AppException(ErrorCode.INVALID_MEMBER_SCOPE);
            m.setStatus(r.status());
            if (r.status() == RestaurantMemberStatus.ACTIVE && m.getJoinedAt() == null)
                m.setJoinedAt(Instant.now());
        }
        return response(m, profiles(List.of(m)).get(m.getUserId()));
    }

    public void remove(Jwt jwt, UUID rid, UUID mid) {
        auth.requireRestaurantAccess(
                rid,
                current.getCurrentUserId(jwt),
                RestaurantMemberRole.OWNER,
                RestaurantMemberRole.MANAGER);
        RestaurantMember m = member(rid, mid);
        if (m.getRole() == RestaurantMemberRole.OWNER)
            throw new AppException(ErrorCode.OWNER_MEMBER_CANNOT_BE_REMOVED);
        m.setStatus(RestaurantMemberStatus.REMOVED);
    }

    private RestaurantMember member(UUID rid, UUID id) {
        return members.findByIdAndRestaurantId(id, rid)
                .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_MEMBER_NOT_FOUND));
    }

    private void duplicate() {
        throw new AppException(ErrorCode.RESTAURANT_MEMBER_ALREADY_EXISTS);
    }

    private RestaurantBranch branch(UUID restaurantId, UUID branchId) {
        if (branchId == null) return null;
        return branches
                .findByIdAndRestaurantId(branchId, restaurantId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_MEMBER_SCOPE));
    }

    private void ensureScopeAvailable(
            RestaurantMember member, UUID restaurantId, RestaurantBranch branch) {
        Optional<RestaurantMember> existing =
                branch == null
                        ? members.findByRestaurantIdAndUserIdAndBranchIdIsNull(
                                restaurantId, member.getUserId())
                        : members.findByRestaurantIdAndBranchIdAndUserId(
                                restaurantId, branch.getId(), member.getUserId());
        if (existing.filter(other -> !other.getId().equals(member.getId())).isPresent()) duplicate();
    }

    private UserServiceClient.InternalUserProfileResponse resolveUser(String email) {
        try {
            UserServiceClient.ApiResponse<UserServiceClient.InternalUserProfileResponse> response =
                    userService.resolveByEmail(
                            internalApi.getKey(), new UserServiceClient.ResolveEmailRequest(email));
            if (response.data() == null) {
                throw new AppException(ErrorCode.RESTAURANT_MEMBER_USER_NOT_FOUND);
            }
            return response.data();
        } catch (FeignException.NotFound exception) {
            throw new AppException(ErrorCode.RESTAURANT_MEMBER_USER_NOT_FOUND);
        }
    }

    private Map<UUID, UserServiceClient.InternalUserProfileResponse> profiles(
            Collection<RestaurantMember> memberPage) {
        List<UUID> userIds = memberPage.stream().map(RestaurantMember::getUserId).distinct().toList();
        if (userIds.isEmpty()) return Map.of();
        UserServiceClient.ApiResponse<List<UserServiceClient.InternalUserProfileResponse>> response =
                userService.batchProfiles(
                        internalApi.getKey(), new UserServiceClient.BatchProfileRequest(userIds));
        if (response.data() == null) return Map.of();
        return response.data().stream()
                .collect(
                        java.util.stream.Collectors.toMap(
                                UserServiceClient.InternalUserProfileResponse::lookupUserId,
                                Function.identity()));
    }

    private RestaurantMemberManagementResponse response(
            RestaurantMember member, UserServiceClient.InternalUserProfileResponse profile) {
        RestaurantBranch branch = member.getBranch();
        return new RestaurantMemberManagementResponse(
                member.getId(),
                member.getRestaurant().getId(),
                branch == null ? null : branch.getId(),
                branch == null ? "Tất cả chi nhánh" : branch.getName(),
                member.getUserId(),
                profile == null ? null : profile.fullName(),
                profile == null ? null : profile.email(),
                profile == null ? null : profile.phoneNumber(),
                profile == null ? null : profile.avatarUrl(),
                member.getRole(),
                member.getStatus(),
                member.getInvitedByUserId(),
                member.getJoinedAt(),
                member.getCreatedAt(),
                member.getVersion());
    }
}
