package com.khanh.fooddelivery.user_service.service.impl;

import com.khanh.fooddelivery.user_service.dto.request.UserAddressCreateRequest;
import com.khanh.fooddelivery.user_service.dto.request.UserAddressUpdateRequest;
import com.khanh.fooddelivery.user_service.dto.response.CurrentUserResponse;
import com.khanh.fooddelivery.user_service.entity.User;
import com.khanh.fooddelivery.user_service.entity.UserAddress;
import com.khanh.fooddelivery.user_service.enums.AddressLabelType;
import com.khanh.fooddelivery.user_service.exception.AppException;
import com.khanh.fooddelivery.user_service.mapper.UserAddressMapperImpl;
import com.khanh.fooddelivery.user_service.repository.UserAddressRepository;
import com.khanh.fooddelivery.user_service.repository.UserRepository;
import com.khanh.fooddelivery.user_service.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAddressServiceImplTest {

    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAddressRepository addressRepository;

    private UserAddressServiceImpl service;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserAddressServiceImpl(
                userService,
                userRepository,
                addressRepository,
                new UserAddressMapperImpl()
        );
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        when(userService.getCurrentUser(any())).thenReturn(
                new CurrentUserResponse(
                        userId, "keycloak-id", "user", "user@example.com",
                        null, null, null, null, null, null
                )
        );
    }

    @Test
    void firstAddressBecomesDefaultAndHomeCustomLabelIsIgnored() {
        when(userRepository.findByIdForUpdate(userId))
                .thenReturn(Optional.of(user));
        when(addressRepository.existsByUserId(userId)).thenReturn(false);
        when(addressRepository.saveAndFlush(any(UserAddress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createAddress(
                null,
                createRequest(AddressLabelType.HOME, "My old label", false)
        );

        assertThat(response.isDefault()).isTrue();
        assertThat(response.labelType()).isEqualTo(AddressLabelType.HOME);
        assertThat(response.customLabel()).isNull();
        assertThat(response.displayLabel()).isEqualTo("Nh\u00e0");
    }

    @Test
    void patchChangesOnlyFieldsSupplied() {
        UserAddress address = address(AddressLabelType.WORK);
        address.setRecipientName("Nguyen Khanh");
        address.setBuildingName("VNPT IT");
        address.setDeliveryNote("Old note");
        when(addressRepository.findByIdAndUserId(address.getId(), userId))
                .thenReturn(Optional.of(address));
        when(addressRepository.saveAndFlush(address)).thenReturn(address);

        service.updateAddress(
                null,
                address.getId(),
                new UserAddressUpdateRequest(
                        null, null, null, null, null, null, null, null,
                        null, null, null, null, null, "  Leave with reception  "
                )
        );

        assertThat(address.getRecipientName()).isEqualTo("Nguyen Khanh");
        assertThat(address.getBuildingName()).isEqualTo("VNPT IT");
        assertThat(address.getDeliveryNote()).isEqualTo("Leave with reception");
    }

    @Test
    void otherLabelRequiresCustomLabel() {
        when(userRepository.findByIdForUpdate(userId))
                .thenReturn(Optional.of(user));
        when(addressRepository.existsByUserId(userId)).thenReturn(false);

        assertThatThrownBy(() -> service.createAddress(
                null,
                createRequest(AddressLabelType.OTHER, "   ", false)
        )).isInstanceOf(AppException.class);
    }

    @Test
    void deletingDefaultPromotesOldestRemainingAddress() {
        when(userRepository.findByIdForUpdate(userId))
                .thenReturn(Optional.of(user));
        UserAddress defaultAddress = address(AddressLabelType.HOME);
        defaultAddress.setIsDefault(true);
        UserAddress oldestRemaining = address(AddressLabelType.WORK);
        oldestRemaining.setIsDefault(false);
        when(addressRepository.findByIdAndUserId(defaultAddress.getId(), userId))
                .thenReturn(Optional.of(defaultAddress));
        when(addressRepository.findFirstByUserIdOrderByCreatedAtAsc(userId))
                .thenReturn(Optional.of(oldestRemaining));

        service.deleteAddress(null, defaultAddress.getId());

        assertThat(oldestRemaining.getIsDefault()).isTrue();
        verify(addressRepository).findFirstByUserIdOrderByCreatedAtAsc(userId);
        verify(addressRepository).save(oldestRemaining);
    }

    @Test
    void settingDefaultClearsThePreviousDefaultInTheSameUserLock() {
        when(userRepository.findByIdForUpdate(userId))
                .thenReturn(Optional.of(user));
        UserAddress currentDefault = address(AddressLabelType.HOME);
        currentDefault.setIsDefault(true);
        UserAddress target = address(AddressLabelType.WORK);
        target.setIsDefault(false);
        when(addressRepository.findByIdAndUserId(target.getId(), userId))
                .thenReturn(Optional.of(target));
        when(addressRepository.findByUserIdAndIsDefaultTrue(userId))
                .thenReturn(Optional.of(currentDefault));
        when(addressRepository.saveAndFlush(currentDefault))
                .thenReturn(currentDefault);
        when(addressRepository.saveAndFlush(target)).thenReturn(target);

        var response = service.setDefaultAddress(null, target.getId());

        assertThat(currentDefault.getIsDefault()).isFalse();
        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).saveAndFlush(currentDefault);
        verify(addressRepository).saveAndFlush(target);
    }

    @Test
    void addressOfAnotherUserIsNotExposed() {
        UUID addressId = UUID.randomUUID();
        when(addressRepository.findByIdAndUserId(addressId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAddress(
                null, addressId,
                new UserAddressUpdateRequest(
                        null, null, null, null, null, null, null, null,
                        null, null, null, null, null, "note"
                )
        )).isInstanceOf(AppException.class);
    }

    private UserAddressCreateRequest createRequest(
            AddressLabelType labelType,
            String customLabel,
            boolean isDefault
    ) {
        return new UserAddressCreateRequest(
                labelType, customLabel, "Nguyen Khanh", "0912345678",
                "25 Nguyen Trai", null, null, "Ha Noi",
                new BigDecimal("20.995"), new BigDecimal("105.810"),
                null, null, null, "Call first", isDefault
        );
    }

    private UserAddress address(AddressLabelType labelType) {
        UserAddress address = new UserAddress();
        address.setId(UUID.randomUUID());
        address.setUser(user);
        address.setLabelType(labelType);
        address.setRecipientPhone("0912345678");
        address.setAddressLine("25 Nguyen Trai");
        address.setCity("Ha Noi");
        return address;
    }
}
