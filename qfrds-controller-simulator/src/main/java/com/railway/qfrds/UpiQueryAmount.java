package com.railway.qfrds;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the ticket fare into a UPI payment URI {@code am=} parameter so a scanned
 * QR matches command 08 instead of a leftover template amount (e.g. {@code am=1}).
 */
final class UpiQueryAmount {

    private static final Pattern AM_PARAM = Pattern.compile("([?&]am=)[^&]*", Pattern.CASE_INSENSITIVE);

    private UpiQueryAmount() {
    }

    static String overlayFare(String payload, String fare) {
        if (payload == null || payload.isBlank() || fare == null || fare.isBlank()) {
            return payload == null ? "" : payload;
        }
        if (!payload.regionMatches(true, 0, "upi:", 0, 4)) {
            return payload;
        }
        String amount = formatAmount(fare);
        Matcher matcher = AM_PARAM.matcher(payload);
        if (matcher.find()) {
            StringBuffer replaced = new StringBuffer();
            matcher.appendReplacement(replaced, Matcher.quoteReplacement(matcher.group(1) + amount));
            matcher.appendTail(replaced);
            return replaced.toString();
        }
        if (payload.indexOf('?') < 0) {
            return payload + "?am=" + amount;
        }
        if (payload.endsWith("?") || payload.endsWith("&")) {
            return payload + "am=" + amount;
        }
        return payload + "&am=" + amount;
    }

    static String formatAmount(String fare) {
        String trimmed = fare.trim().replace("₹", "").trim();
        try {
            double value = Double.parseDouble(trimmed);
            if (value >= 0 && value == Math.rint(value) && value < 1_000_000d) {
                return Long.toString((long) value);
            }
            return String.format("%.2f", value);
        } catch (NumberFormatException ex) {
            return trimmed;
        }
    }
}
