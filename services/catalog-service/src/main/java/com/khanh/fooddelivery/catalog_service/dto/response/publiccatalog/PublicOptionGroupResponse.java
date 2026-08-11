package com.khanh.fooddelivery.catalog_service.dto.response.publiccatalog;

import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import java.util.List;
import java.util.UUID;

public record PublicOptionGroupResponse(
        UUID id,
        String name,
        OptionSelectionType selectionType,
        Integer minimumSelections,
        Integer maximumSelections,
        Boolean required,
        Integer sortOrder,
        List<PublicOptionValueResponse> values) {}
