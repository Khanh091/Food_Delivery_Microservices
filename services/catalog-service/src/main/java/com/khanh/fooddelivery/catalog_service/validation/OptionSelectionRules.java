package com.khanh.fooddelivery.catalog_service.validation;

import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import com.khanh.fooddelivery.catalog_service.exception.ErrorCode;

public final class OptionSelectionRules {
    private OptionSelectionRules() {}

    public static Normalized normalize(
            OptionSelectionType selectionType, Integer minimumSelections, Integer maximumSelections) {
        if (selectionType == null
                || minimumSelections == null
                || maximumSelections == null
                || minimumSelections < 0
                || maximumSelections < 1
                || minimumSelections > maximumSelections) {
            throw new AppException(ErrorCode.INVALID_OPTION_SELECTION);
        }
        if (selectionType == OptionSelectionType.SINGLE
                && (maximumSelections != 1 || minimumSelections > 1)) {
            throw new AppException(ErrorCode.INVALID_OPTION_SELECTION);
        }
        return new Normalized(minimumSelections, maximumSelections, minimumSelections > 0);
    }

    public record Normalized(int minimumSelections, int maximumSelections, boolean required) {}
}
