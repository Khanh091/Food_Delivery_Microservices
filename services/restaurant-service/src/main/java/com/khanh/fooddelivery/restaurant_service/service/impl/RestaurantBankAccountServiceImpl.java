package com.khanh.fooddelivery.restaurant_service.service.impl;

import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBankAccountCreateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.request.RestaurantBankAccountUpdateRequest;
import com.khanh.fooddelivery.restaurant_service.dto.response.RestaurantBankAccountResponse;
import com.khanh.fooddelivery.restaurant_service.entity.RestaurantBankAccount;
import com.khanh.fooddelivery.restaurant_service.enums.BankAccountVerificationStatus;
import com.khanh.fooddelivery.restaurant_service.enums.RestaurantMemberRole;
import com.khanh.fooddelivery.restaurant_service.exception.AppException;
import com.khanh.fooddelivery.restaurant_service.exception.ErrorCode;
import com.khanh.fooddelivery.restaurant_service.mapper.RestaurantBankAccountMapper;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantBankAccountRepository;
import com.khanh.fooddelivery.restaurant_service.repository.RestaurantRepository;
import com.khanh.fooddelivery.restaurant_service.security.CurrentUserProvider;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantAuthorizationService;
import com.khanh.fooddelivery.restaurant_service.service.RestaurantBankAccountService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantBankAccountServiceImpl implements RestaurantBankAccountService {
    private final RestaurantBankAccountRepository accounts;
    private final RestaurantRepository restaurants;
    private final RestaurantBankAccountMapper mapper;
    private final RestaurantAuthorizationService auth;
    private final CurrentUserProvider current;

    public RestaurantBankAccountResponse create(
            Jwt jwt, UUID rid, RestaurantBankAccountCreateRequest r) {
        access(jwt, rid);
        if (accounts.existsByRestaurantIdAndBankCodeAndAccountNumber(
                rid, r.bankCode(), r.accountNumber()))
            throw new AppException(ErrorCode.BANK_ACCOUNT_ALREADY_EXISTS);
        if (Boolean.TRUE.equals(r.isDefault()))
            throw new AppException(
                    ErrorCode.BANK_ACCOUNT_NOT_VERIFIED, "A pending account cannot be default");
        RestaurantBankAccount a = new RestaurantBankAccount();
        a.setRestaurant(
                restaurants
                        .findById(rid)
                        .orElseThrow(() -> new AppException(ErrorCode.RESTAURANT_NOT_FOUND)));
        a.setBankCode(r.bankCode());
        a.setBankName(r.bankName());
        a.setAccountNumber(r.accountNumber());
        a.setAccountHolderName(r.accountHolderName());
        a.setVerificationStatus(BankAccountVerificationStatus.PENDING);
        return mapper.toResponse(accounts.save(a));
    }

    @Transactional(readOnly = true)
    public List<RestaurantBankAccountResponse> list(Jwt jwt, UUID rid) {
        access(jwt, rid);
        return accounts.findAllByRestaurantIdOrderByCreatedAtAsc(rid).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public RestaurantBankAccountResponse update(
            Jwt jwt, UUID rid, UUID id, RestaurantBankAccountUpdateRequest r) {
        access(jwt, rid);
        RestaurantBankAccount a = account(rid, id);
        if (r.bankName() != null) a.setBankName(r.bankName());
        if (r.accountHolderName() != null) a.setAccountHolderName(r.accountHolderName());
        return mapper.toResponse(a);
    }

    public void delete(Jwt jwt, UUID rid, UUID id) {
        access(jwt, rid);
        accounts.delete(account(rid, id));
    }

    public RestaurantBankAccountResponse setDefault(Jwt jwt, UUID rid, UUID id) {
        access(jwt, rid);
        RestaurantBankAccount a = account(rid, id);
        if (a.getVerificationStatus() != BankAccountVerificationStatus.VERIFIED)
            throw new AppException(ErrorCode.BANK_ACCOUNT_NOT_VERIFIED);
        accounts.clearDefault(rid);
        accounts.flush();
        a.setDefaultAccount(true);
        return mapper.toResponse(a);
    }

    private void access(Jwt jwt, UUID id) {
        auth.requireRestaurantAccess(
                id,
                current.getCurrentUserId(jwt),
                RestaurantMemberRole.OWNER,
                RestaurantMemberRole.ACCOUNTANT);
    }

    private RestaurantBankAccount account(UUID rid, UUID id) {
        return accounts.findByIdAndRestaurantId(id, rid)
                .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
    }
}
