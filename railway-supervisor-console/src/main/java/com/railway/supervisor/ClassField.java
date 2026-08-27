package com.railway.supervisor;

import java.util.Optional;

/** Class (command 09): {@code I} or {@code II}, packed to 2 characters. */
public final class ClassField {

    private ClassField() {
    }

    public static Optional<String> pack(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String value = raw.trim().toUpperCase();
        if (!"I".equals(value) && !"II".equals(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static Optional<String> unpack(String data) {
        if (data == null) {
            return Optional.empty();
        }
        return pack(data.trim()).map(String::trim);
    }
}
