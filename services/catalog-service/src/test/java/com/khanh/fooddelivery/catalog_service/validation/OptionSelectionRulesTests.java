package com.khanh.fooddelivery.catalog_service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khanh.fooddelivery.catalog_service.enums.OptionSelectionType;
import com.khanh.fooddelivery.catalog_service.exception.AppException;
import org.junit.jupiter.api.Test;

class OptionSelectionRulesTests {
    @Test
    void optionalSingleDerivesRequiredFromMinimum() {
        OptionSelectionRules.Normalized normalized =
                OptionSelectionRules.normalize(OptionSelectionType.SINGLE, 0, 1);

        assertThat(normalized.required()).isFalse();
    }

    @Test
    void requiredMultipleAllowsMinimumAndMaximumRange() {
        OptionSelectionRules.Normalized normalized =
                OptionSelectionRules.normalize(OptionSelectionType.MULTIPLE, 2, 4);

        assertThat(normalized.required()).isTrue();
        assertThat(normalized.minimumSelections()).isEqualTo(2);
        assertThat(normalized.maximumSelections()).isEqualTo(4);
    }

    @Test
    void singleRejectsMaximumOtherThanOne() {
        assertThatThrownBy(() -> OptionSelectionRules.normalize(OptionSelectionType.SINGLE, 0, 2))
                .isInstanceOf(AppException.class);
    }

    @Test
    void multipleRejectsMinimumAboveMaximum() {
        assertThatThrownBy(() -> OptionSelectionRules.normalize(OptionSelectionType.MULTIPLE, 3, 2))
                .isInstanceOf(AppException.class);
    }
}
