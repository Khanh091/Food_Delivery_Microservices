package com.khanh.fooddelivery.order_service.service;

import com.khanh.fooddelivery.order_service.dto.response.CheckoutPreviewResponse;
import com.khanh.fooddelivery.order_service.dto.response.DeliveryQuoteStatus;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CheckoutPreviewFingerprint {
    public String of(
            UUID ownerUserId,
            long cartVersion,
            CheckoutPreviewResponse.CheckoutAddressSnapshot address,
            CheckoutPreviewResponse.CheckoutRestaurantSnapshot restaurant,
            CheckoutPreviewResponse.CheckoutBranchSnapshot branch,
            List<CheckoutPreviewResponse.CheckoutItemResponse> items,
            String currency,
            BigDecimal itemsSubtotal,
            BigDecimal discountAmount,
            DeliveryQuoteStatus deliveryQuoteStatus,
            UUID deliveryQuoteId,
            BigDecimal deliveryFee,
            java.time.Instant deliveryQuoteExpiresAt,
            String deliveryPricingPolicyVersion) {
        String canonical = String.join("|",
                ownerUserId.toString(),
                Long.toString(cartVersion),
                text(address.targetType()), address.addressId() == null ? "" : address.addressId().toString(),
                address.temporaryLocationId() == null ? "" : address.temporaryLocationId().toString(),
                String.valueOf(address.version()),
                text(address.recipientName()), text(address.recipientPhone()), text(address.addressLine()),
                text(address.ward()), text(address.district()), text(address.city()),
                money(address.latitude()), money(address.longitude()), text(address.buildingName()),
                text(address.floor()), text(address.entrance()), text(address.deliveryNote()),
                restaurant.restaurantId().toString(), branch.branchId().toString(), text(currency),
                money(itemsSubtotal), money(discountAmount), deliveryQuoteStatus.name(),
                deliveryQuoteId == null ? "" : deliveryQuoteId.toString(), money(deliveryFee),
                deliveryQuoteExpiresAt == null ? "" : deliveryQuoteExpiresAt.toString(), text(deliveryPricingPolicyVersion),
                items.stream().sorted(Comparator.comparing(CheckoutPreviewResponse.CheckoutItemResponse::cartItemId))
                        .map(this::line).reduce((left, right) -> left + "|" + right).orElse(""));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String line(CheckoutPreviewResponse.CheckoutItemResponse item) {
        return String.join("~", item.cartItemId().toString(), item.catalogItemId().toString(),
                item.branchItemId().toString(), Integer.toString(item.quantity()), text(item.note()),
                money(item.unitPrice()), money(item.lineTotal()),
                item.selectedOptions().stream().sorted(Comparator.comparing(CheckoutPreviewResponse.SelectedOptionResponse::optionValueId))
                        .map(option -> option.optionValueId() + ":" + money(option.additionalPrice()))
                        .reduce((left, right) -> left + "," + right).orElse(""));
    }

    private String text(String value) { return value == null ? "" : value; }
    private String money(BigDecimal value) { return value == null ? "null" : value.stripTrailingZeros().toPlainString(); }
}
