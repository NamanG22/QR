package com.railway.qfrds;

import java.util.Objects;
import java.util.Optional;

/**
 * PRS thin-client envelope: {@code SOH thPRS 02 <sub> Q/S <len> STX <body> ETX}.
 * <p>
 * SOH = ASCII 1, STX (SOT) = ASCII 2, ETX = ASCII 3. Length is three characters
 * counting the body between STX and ETX (spaces for ping 110).
 * </p>
 */
public final class PrsFrame {

    public static final char SOH = 0x01;
    public static final char STX = 0x02;
    public static final char ETX = 0x03;

    public static final String THIN_CLIENT = "thPRS";
    public static final String FUNCTION = "02";

    public static final String SUB_PING = "110";
    public static final String SUB_TDRC = "111";
    public static final String SUB_QR = "112";
    public static final String SUB_PAY_OK = "113";
    public static final String SUB_PAY_FAIL = "114";

    public static final char QUERY = 'Q';
    public static final char STATUS = 'S';

    private PrsFrame() {
    }

    public static boolean isFrame(String raw) {
        return raw != null && !raw.isEmpty() && raw.charAt(0) == SOH;
    }

    public static String wrap(String subFunction, char queryOrStatus, String body) {
        Objects.requireNonNull(subFunction, "subFunction");
        String payload = body == null ? "" : body;
        String lengthText;
        if (SUB_PING.equals(subFunction) && payload.isEmpty()) {
            lengthText = "   ";
        } else {
            int len = Math.min(payload.length(), 999);
            lengthText = String.format("%03d", len);
        }
        return "" + SOH + THIN_CLIENT + FUNCTION + subFunction + queryOrStatus + lengthText + STX + payload + ETX;
    }

    public static String wrapPingQuery() {
        return wrap(SUB_PING, QUERY, "");
    }

    public static String wrapPingReply() {
        return wrap(SUB_PING, STATUS, "");
    }

    public static Optional<Unwrapped> unwrap(String raw) {
        if (raw == null || raw.length() < 17) {
            return Optional.empty();
        }
        if (raw.charAt(0) != SOH || raw.charAt(raw.length() - 1) != ETX) {
            return Optional.empty();
        }
        String thin = raw.substring(1, 6);
        String function = raw.substring(6, 8);
        String sub = raw.substring(8, 11);
        char qs = raw.charAt(11);
        if (!THIN_CLIENT.equals(thin) || !FUNCTION.equals(function)) {
            return Optional.empty();
        }
        if (raw.charAt(15) != STX) {
            return Optional.empty();
        }
        String body = raw.substring(16, raw.length() - 1);
        return Optional.of(new Unwrapped(sub, qs, body));
    }

    /** Log-friendly form with control characters named. */
    public static String toLog(String frame) {
        if (frame == null) {
            return "";
        }
        return frame
                .replace(String.valueOf(SOH), "[SOH]")
                .replace(String.valueOf(STX), "[STX]")
                .replace(String.valueOf(ETX), "[ETX]");
    }

    public record Unwrapped(String subFunction, char queryOrStatus, String body) {
    }
}
