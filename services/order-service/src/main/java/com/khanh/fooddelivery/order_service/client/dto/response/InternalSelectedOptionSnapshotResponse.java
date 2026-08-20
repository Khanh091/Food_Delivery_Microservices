package com.khanh.fooddelivery.order_service.client.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalSelectedOptionSnapshotResponse(
        UUID optionGroupId,
        UUID optionValueId,
        String groupName,
        String valueName,
        BigDecimal additionalPrice
) {
}
