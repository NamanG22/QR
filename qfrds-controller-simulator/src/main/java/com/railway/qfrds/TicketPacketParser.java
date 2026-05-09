package com.railway.qfrds;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses pipe-delimited supervisor packets into {@link TicketData}.
 * <p>
 * Packet grammar: {@code KEY=value} segments separated by {@code |}. Unknown keys are ignored.
 * Keys are matched case-insensitively; values are trimmed. If a value contains {@code =},
 * only the first {@code =} in each segment splits key from value.
 * </p>
 */
public final class TicketPacketParser {

    private static final Set<String> KNOWN_KEYS = Set.of(
            "TYPE", "SRC", "DST", "FARE", "TXN", "TS", "PNAME"
    );

    /**
     * Result of a parse attempt — either ticket data or a human-readable failure reason for logs.
     */
    public static final class ParseResult {
        private final Optional<TicketData> data;
        private final Optional<String> errorMessage;

        private ParseResult(Optional<TicketData> data, Optional<String> errorMessage) {
            this.data = data;
            this.errorMessage = errorMessage;
        }

        public static ParseResult ok(TicketData data) {
            return new ParseResult(Optional.of(data), Optional.empty());
        }

        public static ParseResult fail(String message) {
            return new ParseResult(Optional.empty(), Optional.of(message));
        }

        public Optional<TicketData> getData() {
            return data;
        }

        public Optional<String> getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Parses one complete line (without trailing newline). Malformed or incomplete packets
     * return {@link ParseResult#fail(String)} without throwing.
     */
    public ParseResult parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return ParseResult.fail("empty line");
        }

        Map<String, String> fields = new LinkedHashMap<>();
        String[] segments = rawLine.trim().split("\\|");
        for (String segment : segments) {
            int eq = segment.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = segment.substring(0, eq).trim();
            String value = segment.substring(eq + 1).trim();
            if (key.isEmpty()) {
                continue;
            }
            String canonical = key.toUpperCase(Locale.ROOT);
            if (KNOWN_KEYS.contains(canonical)) {
                fields.put(canonical, value);
            }
            // Unknown keys: ignored safely per requirements
        }

        String typeStr = fields.get("TYPE");
        if (typeStr == null || typeStr.isBlank()) {
            return ParseResult.fail("missing TYPE field");
        }

        TicketType type;
        try {
            type = TicketType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ParseResult.fail("invalid TYPE: " + typeStr);
        }

        String src = fields.get("SRC");
        String dst = fields.get("DST");
        String fare = fields.get("FARE");
        String txn = fields.get("TXN");
        String ts = fields.get("TS");

        if (isBlank(src) || isBlank(dst) || isBlank(fare) || isBlank(txn) || isBlank(ts)) {
            return ParseResult.fail("missing required field (SRC, DST, FARE, TXN, TS)");
        }

        Optional<String> pname = Optional.ofNullable(fields.get("PNAME")).map(String::trim).filter(s -> !s.isEmpty());
        if (type == TicketType.PRS && pname.isEmpty()) {
            return ParseResult.fail("PRS ticket missing PNAME");
        }
        if (type == TicketType.UTS) {
            pname = Optional.empty();
        }

        TicketData data = new TicketData(type, src, dst, fare, txn, ts, pname);
        return ParseResult.ok(data);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
