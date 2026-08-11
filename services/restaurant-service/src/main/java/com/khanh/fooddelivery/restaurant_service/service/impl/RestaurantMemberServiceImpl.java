package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantMemberUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantMemberResponse;
import com.khanh.fooddelivery.restaurant_service.entity.Restaurant;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBranch;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantMember;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberStatus;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantMemberMapper;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBranchRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantMemberRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantMemberService;
import java.time.Instant;
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
public class RestaurantMemberServiceImpl implements RestaurantMemberService {
    private final RestaurantMemberRepository members;
    private final RestaurantRepository restaurants;
    private final RestaurantBranchRepository branches;
    private final RestaurantMemberMapper mapper;
    private final RestaurantAuthorizationService auth;
    private final CurrentUserProvider current;

    public RestaurantMemberResponse create(Jwt jwt, UUID rid, RestaurantMemberCreateRequest r) {
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
        RestaurantBranch branch = null;
        if (r.branchId() != null) {
            branch =
                    branches.findByIdAndRestaurantId(r.branchId(), rid)
                            .orElseThrow(() -> new AppException(ErrorCode.INVALID_MEMBER_SCOPE));
            if (members.existsByRestaurantIdAndBranchIdAndUserId(rid, r.branchId(), r.userId()))
                duplicate();
        } else if (members.existsByRestaurantIdAndUserIdAndBranchIdIsNull(rid, r.userId()))
            duplicate();
        RestaurantMember m = new RestaurantMember();
        m.setRestaurant(restaurant);
        m.setBranch(branch);
        m.setUserId(r.userId());
        m.setRole(r.role());
        m.setStatus(RestaurantMemberStatus.INVITED);
        m.setInvitedByUserId(actor);
        return mapper.toResponse(members.save(m));
    }

    @Transactional(readOnly = true)
    public Page<RestaurantMemberResponse> list(Jwt jwt, UUID rid, Pageable p) {
        auth.requireRestaurantAccess(
                rid,
                current.getCurrentUserId(jwt),
                RestaurantMemberRole.OWNER,
                RestaurantMemberRole.MANAGER);
        return members.findAllByRestaurantId(rid, p).map(mapper::toResponse);
    }

    public RestaurantMemberResponse update(
            Jwt jwt, UUID rid, UUID mid, RestaurantMemberUpdateRequest r) {
        auth.requireRestaurantAccess(
                rid,
                current.getCurrentUserId(jwt),
                RestaurantMemberRole.OWNER,
                RestaurantMemberRole.MANAGER);
        RestaurantMember m = member(rid, mid);
        if (m.getRole() == RestaurantMemberRole.OWNER)
            throw new AppException(ErrorCode.OWNER_MEMBER_CANNOT_BE_REMOVED);
        if (r.role() == RestaurantMemberRole.OWNER)
            throw new AppException(ErrorCode.INVALID_MEMBER_SCOPE);
        if (r.role() != null) m.setRole(r.role());
        if (r.status() != null) {
            m.setStatus(r.status());
            if (r.status() == RestaurantMemberStatus.ACTIVE && m.getJoinedAt() == null)
                m.setJoinedAt(Instant.now());
        }
        return mapper.toResponse(m);
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
}
