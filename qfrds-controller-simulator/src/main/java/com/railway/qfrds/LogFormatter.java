package com.railway.qfrds;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Shared timestamp prefix for controller dashboard logs.
 */
final class LogFormatter {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    static String ts(String message) {
        return "[" + TS.format(Instant.now()) + "] " + message;
    }

    private LogFormatter() {
    }
}
