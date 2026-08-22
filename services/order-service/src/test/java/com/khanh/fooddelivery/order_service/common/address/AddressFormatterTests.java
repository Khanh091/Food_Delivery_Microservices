package com.khanh.fooddelivery.order_service.common.address;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AddressFormatterTests {
    @Test
    void keeps_a_readable_snapshot_and_omits_blank_parts() {
        assertThat(AddressFormatter.format("97 Man Thien", "", "Thu Duc", "Ho Chi Minh City"))
                .isEqualTo("97 Man Thien, Thu Duc, Ho Chi Minh City");
    }
}
