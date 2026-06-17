package com.railway.qfrds;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Best-effort file log for RS232 diagnostics (passenger UI has no engineering console). */
final class SerialDiagLog {

    private SerialDiagLog() {
    }

    static void write(String message) {
        String line = Instant.now() + " " + message;
        System.err.println(line);
        Path logDir = resolveLogDir();
        try {
            Files.createDirectories(logDir);
            Files.writeString(
                    logDir.resolve("serial.log"),
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Best-effort only.
        }
    }

    private static Path resolveLogDir() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "QFRDS");
        }
        return Path.of(System.getProperty("user.home"), ".qfrds", "logs");
    }
}
