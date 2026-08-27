package com.railway.qfrds;

import java.util.Optional;

/** Two-digit numeric payload (day 01–31, month 01–12). */
public final class TwoDigitField {

    private TwoDigitField() {
    }

    public static Optional<String> pack(String raw, int minInclusive, int maxInclusive) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            int n = Integer.parseInt(raw.trim());
            if (n < minInclusive || n > maxInclusive) {
                return Optional.empty();
            }
            return Optional.of(String.format("%02d", n));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<String> unpack(String data, int minInclusive, int maxInclusive) {
        if (data == null || data.length() != 2) {
            return Optional.empty();
        }
        return pack(data, minInclusive, maxInclusive);
    }
}
