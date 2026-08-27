package com.railway.supervisor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transaction type (command 12). Wire data is a TRANSACTION CODE (e.g. {@code PLAT}).
 */
public final class TxnTypeField {

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

    public static List<String> comboLabels() {
        List<String> labels = new ArrayList<>();
        TYPES.forEach((code, type) -> labels.add(code + " — " + type));
        return labels;
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
}
