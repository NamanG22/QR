package com.railway.qfrds;

import java.util.Optional;
import java.util.regex.Pattern;

/** Variable-length fare payload: 1–5 numeric digits. */
public final class FareField {

    private static final Pattern DIGITS = Pattern.compile("\\d{1,5}");

    private FareField() {
    }

    public static Optional<String> pack(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (!DIGITS.matcher(trimmed).matches()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    public static Optional<String> unpack(String data) {
        return pack(data);
    }
}
