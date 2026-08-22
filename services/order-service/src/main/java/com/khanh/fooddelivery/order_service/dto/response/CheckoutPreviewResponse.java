package com.khanh.fooddelivery.order_service.dto.response;

import com.khanh.fooddelivery.order_service.common.address.AddressFormatter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CheckoutPreviewResponse(
        long cartVersion,
        CheckoutAddressSnapshot address,
        CheckoutRestaurantSnapshot restaurant,
        CheckoutBranchSnapshot branch,
        List<CheckoutItemResponse> items,
        String currency,
        BigDecimal itemsSubtotal,
        BigDecimal discountAmount,
        DeliveryQuoteStatus deliveryQuoteStatus,
        UUID deliveryQuoteId,
        Instant deliveryQuoteExpiresAt,
        String deliveryPricingPolicyVersion,
        BigDecimal deliveryFee,
        BigDecimal totalAmount,
        List<PriceChangeResponse> priceChanges,
        String previewFingerprint,
        Instant calculatedAt,
        boolean canPlaceOrder) {

    public record CheckoutAddressSnapshot(
            String targetType, UUID addressId, UUID temporaryLocationId, String labelType, String customLabel, String displayLabel,
            String recipientName, String recipientPhone, String addressLine, String ward, String district,
            String city, BigDecimal latitude, BigDecimal longitude, String buildingName, String floor,
            String entrance, String deliveryNote, Long version, String formattedAddress) {
        public CheckoutAddressSnapshot(
                String targetType, UUID addressId, UUID temporaryLocationId, String labelType, String customLabel,
                String displayLabel, String recipientName, String recipientPhone, String addressLine, String ward,
                String district, String city, BigDecimal latitude, BigDecimal longitude, String buildingName,
                String floor, String entrance, String deliveryNote, Long version) {
            this(targetType, addressId, temporaryLocationId, labelType, customLabel, displayLabel, recipientName,
                    recipientPhone, addressLine, ward, district, city, latitude, longitude, buildingName, floor,
                    entrance, deliveryNote, version, AddressFormatter.format(
                            addressLine, ward, district, city, buildingName, floor, entrance));
        }
    }

    public record CheckoutRestaurantSnapshot(UUID restaurantId, String restaurantName) {}

    public record CheckoutBranchSnapshot(UUID branchId, String branchName) {}

    public record CheckoutItemResponse(
            UUID cartItemId, UUID catalogItemId, UUID branchItemId, String name, String imageUrl,
            int quantity, String note, List<SelectedOptionResponse> selectedOptions,
            BigDecimal baseUnitPrice, BigDecimal optionUnitPrice, BigDecimal unitPrice,
            BigDecimal originalPrice, BigDecimal lineTotal) {}

    public record SelectedOptionResponse(
            UUID optionGroupId, UUID optionValueId, String groupName, String valueName,
            BigDecimal additionalPrice) {}

    public record PriceChangeResponse(
            UUID cartItemId, UUID catalogItemId, String itemName,
            BigDecimal previousUnitPrice, BigDecimal currentUnitPrice) {}
}
