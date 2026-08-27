package com.railway.qfrds;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Transaction type (command 12). Wire data is a TRANSACTION CODE (e.g. {@code PLAT}).
 * Unknown codes are accepted and shown as {@code INVALID}.
 */
public final class TxnTypeField {

    public static final String INVALID = "INVALID";

    private static final Map<String, String> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("SPLC", "SPECIAL CANCEL");
        TYPES.put("PLAT", "PLATFORM");
        TYPES.put("NI", "NON-ISSUE");
        TYPES.put("CANC", "CANCELLATION");
        TYPES.put("ST", "SEASON TICKET");
        TYPES.put("BPT", "BPT TICKET");
        TYPES.put("SF", "SUPERFAST TICKET");
        TYPES.put("JRNY", "JOURNEY");
        TYPES.put("CARD", "I CARD");
        TYPES.put("MMQT", "MULTI TRT MST-QST");
        TYPES.put("RRTT", "RAIL/TOURIST");
        TYPES.put("PART", "PARTIAL CANCELLATION");
    }

    private TxnTypeField() {
    }

    public static Optional<String> pack(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String code = raw.trim().split("[\\s—-]", 2)[0].toUpperCase();
        if (!TYPES.containsKey(code)) {
            return Optional.empty();
        }
        return Optional.of(code);
    }

    public static Optional<String> unpack(String data) {
        if (data == null || data.isBlank()) {
            return Optional.empty();
        }
        String trimmed = data.trim().toUpperCase();
        if (trimmed.endsWith(":")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(trimmed);
    }

    /** Passenger-facing transaction type, or {@code INVALID} for unknown codes. */
    public static String display(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        String key = code.trim().toUpperCase();
        return TYPES.getOrDefault(key, INVALID);
    }
}
