package com.khanh.fooddelivery.user_service.enums;

public enum SystemRole {
    CUSTOMER,
    RESTAURANT_OWNER,
    RESTAURANT_STAFF,
    DRIVER;

    public boolean isPartnerGrantable() {
        return switch (this) {
            case RESTAURANT_OWNER, RESTAURANT_STAFF, DRIVER -> true;
            case CUSTOMER -> false;
        };
    }
}
