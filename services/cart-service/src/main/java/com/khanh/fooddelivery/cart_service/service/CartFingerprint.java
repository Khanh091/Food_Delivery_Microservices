package com.khanh.fooddelivery.cart_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public final class CartFingerprint {
    private CartFingerprint() {}

    public static String of(UUID catalogItemId, List<UUID> selectedOptionValueIds, String note) {
        String options =
                selectedOptionValueIds.stream()
                        .distinct()
                        .sorted(Comparator.comparing(UUID::toString))
                        .map(UUID::toString)
                        .reduce((left, right) -> left + "," + right)
                        .orElse("");
        String canonical = catalogItemId + "|" + options + "|" + (note == null ? "" : note);
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
