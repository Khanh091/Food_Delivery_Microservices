package com.khanh.fooddelivery.order_service.client.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalCartItemSnapshotResponse(
        UUID cartItemId,
        UUID catalogItemId,
        UUID branchItemId,
        int quantity,
        String note,
        List<InternalSelectedOptionSnapshotResponse> selectedOptions,
        String itemName,
        String imageUrl,
        BigDecimal baseUnitPrice,
        BigDecimal optionUnitPrice,
        BigDecimal unitPrice,
        BigDecimal originalPrice
) {
}
