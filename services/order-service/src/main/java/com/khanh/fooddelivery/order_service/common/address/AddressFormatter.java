package com.khanh.fooddelivery.order_service.common.address;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AddressFormatter {
    private AddressFormatter() {
    }

    public static String format(String addressLine, String ward, String district, String city) {
        return format(addressLine, ward, district, city, null, null, null);
    }

    public static String format(
            String addressLine,
            String ward,
            String district,
            String city,
            String buildingName,
            String floor,
            String entrance
    ) {
        return Arrays.stream(new String[]{buildingName, addressLine, floor, entrance, ward, district, city})
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
