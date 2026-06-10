package com.railway.qfrds;

import javafx.application.Application;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * jpackage entry point. Keeps {@link MainApp} as an {@link Application} subclass while giving
 * packaged builds a stable main class and a crash log when the GUI fails to start.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> logFatal(
                "Uncaught on " + thread.getName(), error));
        try {
            Application.launch(MainApp.class, args);
        } catch (Throwable error) {
            logFatal("Application.launch failed", error);
            System.exit(1);
        }
    }

    static void logFatal(String message, Throwable error) {
        String line = Instant.now() + " " + message;
        if (error != null) {
            StringWriter stack = new StringWriter();
            error.printStackTrace(new PrintWriter(stack));
            line = line + System.lineSeparator() + stack;
        }
        System.err.println(line);
        writeStartupLog(line);
    }

    private static void writeStartupLog(String text) {
        Path logDir = resolveLogDir();
        try {
            Files.createDirectories(logDir);
            Files.writeString(
                    logDir.resolve("startup.log"),
                    text + System.lineSeparator() + System.lineSeparator(),
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
