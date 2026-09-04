package com.railway.supervisor;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Production UTS envelope: {@code $}<2-digit code>{@code <Length><Data>^}.
 * <p>
 * There is no colon between code, length, and data. {@code Data} uses {@code :} as a
 * field separator and usually ends with a trailing colon (code 22 QR is the exception).
 * {@code Length} is the UTF-8 byte length of {@code Data} plus EOT (i.e. {@code Data^}).
 * PRS uses a separate SOH/STX/ETX envelope ({@link PrsFrame}).
 * </p>
 */
public final class CommandFrame {

    public static final char SOT = '$';
    public static final char EOT = '^';
    public static final char SEP = ':';

    private static final int MAX_LENGTH_DIGITS = 4;

    private CommandFrame() {
    }

    public static String wrap(String code, String data) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(data, "data");
        if (code.matches("\\d{2}")) {
            return wrapProduction(code, data);
        }
        return wrapLegacy(code, data);
    }

    /**
     * {@code $00}<len>{@code thUts:^} — length includes the trailing colon and EOT.
     */
    static String wrapProduction(String code, String data) {
        if ("13".equals(code) && (data == null || data.isBlank() || ":".equals(data))) {
            return SOT + "1303:^";
        }
        boolean qr = "22".equals(code);
        String payload = data;
        if (!qr && !payload.endsWith(String.valueOf(SEP))) {
            payload = payload + SEP;
        }
        int length = (payload + EOT).getBytes(StandardCharsets.UTF_8).length;
        String lengthText;
        if (qr) {
            lengthText = length < 1000 ? String.format("%03d", length) : Integer.toString(length);
        } else {
            lengthText = length < 100 ? String.format("%02d", length) : Integer.toString(length);
        }
        return SOT + code + lengthText + payload + EOT;
    }

    static String wrapLegacy(String code, String data) {
        if (code.isBlank() || code.indexOf(SEP) >= 0 || code.indexOf(SOT) >= 0 || code.indexOf(EOT) >= 0) {
            throw new IllegalArgumentException("invalid command code: " + code);
        }
        int length = data.getBytes(StandardCharsets.UTF_8).length;
        return SOT + code + SEP + length + SEP + data + EOT;
    }

    public static Optional<Unwrapped> unwrap(String frame) {
        if (frame == null || frame.isBlank()) {
            return Optional.empty();
        }
        String trimmed = frame.trim();
        if (trimmed.length() < 4 || trimmed.charAt(0) != SOT || trimmed.charAt(trimmed.length() - 1) != EOT) {
            return Optional.empty();
        }
        if ("$1303:^".equals(trimmed) || "$1302:^".equals(trimmed) || "$132:^".equals(trimmed)) {
            return Optional.of(new Unwrapped("13", ""));
        }
        Optional<Unwrapped> production = unwrapProduction(trimmed);
        if (production.isPresent()) {
            return production;
        }
        return unwrapLegacy(trimmed);
    }

    private static Optional<Unwrapped> unwrapProduction(String trimmed) {
        String code = trimmed.substring(1, 3);
        if (!code.matches("\\d{2}")) {
            return Optional.empty();
        }
        String rest = trimmed.substring(3);
        for (int digits = 1; digits <= MAX_LENGTH_DIGITS && digits < rest.length(); digits++) {
            String lengthText = rest.substring(0, digits);
            if (!lengthText.chars().allMatch(Character::isDigit)) {
                break;
            }
            int declared;
            try {
                declared = Integer.parseInt(lengthText);
            } catch (NumberFormatException ex) {
                break;
            }
            String suffix = rest.substring(digits);
            int actual = suffix.getBytes(StandardCharsets.UTF_8).length;
            if (declared != actual || suffix.isEmpty() || suffix.charAt(suffix.length() - 1) != EOT) {
                continue;
            }
            String data = suffix.substring(0, suffix.length() - 1);
            if (data.endsWith(String.valueOf(SEP))) {
                data = data.substring(0, data.length() - 1);
            }
            return Optional.of(new Unwrapped(code, data));
        }
        return Optional.empty();
    }

    private static Optional<Unwrapped> unwrapLegacy(String trimmed) {
        if (trimmed.length() < 5) {
            return Optional.empty();
        }
        String body = trimmed.substring(1, trimmed.length() - 1);
        int firstSep = body.indexOf(SEP);
        if (firstSep <= 0) {
            return Optional.empty();
        }
        int secondSep = body.indexOf(SEP, firstSep + 1);
        if (secondSep < 0 || secondSep == firstSep + 1) {
            return Optional.empty();
        }
        String code = body.substring(0, firstSep);
        String lengthText = body.substring(firstSep + 1, secondSep);
        String data = body.substring(secondSep + 1);
        int declared;
        try {
            declared = Integer.parseInt(lengthText);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        int actual = data.getBytes(StandardCharsets.UTF_8).length;
        if (declared != actual) {
            return Optional.empty();
        }
        return Optional.of(new Unwrapped(code, data));
    }

    public static final class Unwrapped {
        private final String code;
        private final String data;

        Unwrapped(String code, String data) {
            this.code = code;
            this.data = data;
        }

        public String code() {
            return code;
        }

        public String data() {
            return data;
        }
    }
}
