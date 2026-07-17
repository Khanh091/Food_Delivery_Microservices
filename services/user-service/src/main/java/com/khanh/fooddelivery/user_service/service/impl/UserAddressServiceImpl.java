package com.khanh.fooddelivery.user_service.service.impl;

import com.khanh.fooddelivery.user_service.dto.request.UserAddressCreateRequest;
import com.khanh.fooddelivery.user_service.dto.request.UserAddressUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.user_service.dto.response.UserAddressResponse;
import com.khanh.fooddelivery.user_service.entity.User;
import com.khanh.fooddelivery.user_service.entity.UserAddress;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.exception.ErrorCode;
import com.khanh.fooddelivery.user_service.mapper.UserAddressMapper;
import com.khanh.fooddelivery.user_service.repository.UserAddressRepository;
import com.khanh.fooddelivery.user_service.repository.UserRepository;
import com.khanh.fooddelivery.user_service.service.UserAddressService;
import com.khanh.fooddelivery.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAddressServiceImpl implements UserAddressService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final UserAddressMapper addressMapper;

    @Override
    public List<UserAddressResponse> getMyAddresses(Jwt jwt) {
        UUID userId = currentUserId(jwt);
        return addressMapper.toResponseList(
                addressRepository
                        .findAllByUserIdOrderByIsDefaultDescCreatedAtDesc(
                                userId
                        )
        );
    }

    @Override
    public UserAddressResponse createAddress(
            Jwt jwt,
            UserAddressCreateRequest request
    ) {
        User user = lockCurrentUser(jwt);
        boolean firstAddress =
                !addressRepository.existsByUserId(user.getId());
        boolean makeDefault =
                firstAddress || Boolean.TRUE.equals(request.isDefault());

        if (makeDefault) {
            clearDefaultAddress(user.getId());
        }

        UserAddress address = addressMapper.toEntity(request);
        address.setUser(user);
        address.setIsDefault(makeDefault);
        normalizeCreateRequest(request, address);

        UserAddress saved = addressRepository.saveAndFlush(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    public UserAddressResponse updateAddress(
            Jwt jwt,
            UUID addressId,
            UserAddressUpdateRequest request
    ) {
        UUID userId = currentUserId(jwt);
        UserAddress address = ownedAddress(addressId, userId);

        validateRequiredUpdates(request);
        addressMapper.updateEntity(request, address);
        normalizeUpdateRequest(request, address);
        UserAddress saved = addressRepository.saveAndFlush(address);
        return addressMapper.toResponse(saved);
    }

    @Override
    public void deleteAddress(Jwt jwt, UUID addressId) {
        User user = lockCurrentUser(jwt);
        UserAddress address = ownedAddress(addressId, user.getId());
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            addressRepository
                    .findFirstByUserIdOrderByCreatedAtDesc(user.getId())
                    .ifPresent(replacement -> {
                        replacement.setIsDefault(true);
                        addressRepository.save(replacement);
                    });
        }
    }

    @Override
    public UserAddressResponse setDefaultAddress(
            Jwt jwt,
            UUID addressId
    ) {
        User user = lockCurrentUser(jwt);
        UserAddress address = ownedAddress(addressId, user.getId());

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            return addressMapper.toResponse(address);
        }

        clearDefaultAddress(user.getId());
        address.setIsDefault(true);
        UserAddress saved = addressRepository.saveAndFlush(address);
        return addressMapper.toResponse(saved);
    }

    private UUID currentUserId(Jwt jwt) {
        CurrentUserResponse currentUser =
                userService.getCurrentUser(jwt);
        return currentUser.id();
    }

    private User lockCurrentUser(Jwt jwt) {
        UUID userId = currentUserId(jwt);
        return userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND
                ));
    }

    private UserAddress ownedAddress(UUID addressId, UUID userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.ADDRESS_NOT_FOUND
                ));
    }

    private void clearDefaultAddress(UUID userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(currentDefault -> {
                    currentDefault.setIsDefault(false);
                    addressRepository.saveAndFlush(currentDefault);
                });
    }

    private void validateRequiredUpdates(
            UserAddressUpdateRequest request
    ) {
        requireNotBlankWhenPresent(
                request.recipientName(),
                "recipientName"
        );
        requireNotBlankWhenPresent(
                request.recipientPhone(),
                "recipientPhone"
        );
        requireNotBlankWhenPresent(
                request.addressLine(),
                "addressLine"
        );
        requireNotBlankWhenPresent(request.city(), "city");
    }

    private void requireNotBlankWhenPresent(
            String value,
            String fieldName
    ) {
        if (value != null && value.isBlank()) {
            throw new AppException(
                    ErrorCode.INVALID_REQUEST,
                    fieldName + " must not be blank"
            );
        }
    }

    private void normalizeCreateRequest(
            UserAddressCreateRequest request,
            UserAddress address
    ) {
        address.setLabel(trimToNull(request.label()));
        address.setRecipientName(request.recipientName().trim());
        address.setRecipientPhone(request.recipientPhone().trim());
        address.setAddressLine(request.addressLine().trim());
        address.setWard(trimToNull(request.ward()));
        address.setDistrict(trimToNull(request.district()));
        address.setCity(request.city().trim());
        address.setDeliveryNote(trimToNull(request.deliveryNote()));
    }

    private void normalizeUpdateRequest(
            UserAddressUpdateRequest request,
            UserAddress address
    ) {
        if (request.label() != null) {
            address.setLabel(trimToNull(request.label()));
        }
        if (request.recipientName() != null) {
            address.setRecipientName(request.recipientName().trim());
        }
        if (request.recipientPhone() != null) {
            address.setRecipientPhone(request.recipientPhone().trim());
        }
        if (request.addressLine() != null) {
            address.setAddressLine(request.addressLine().trim());
        }
        if (request.ward() != null) {
            address.setWard(trimToNull(request.ward()));
        }
        if (request.district() != null) {
            address.setDistrict(trimToNull(request.district()));
        }
        if (request.city() != null) {
            address.setCity(request.city().trim());
        }
        if (request.deliveryNote() != null) {
            address.setDeliveryNote(trimToNull(request.deliveryNote()));
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
