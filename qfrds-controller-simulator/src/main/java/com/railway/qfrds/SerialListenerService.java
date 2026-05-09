package com.railway.qfrds;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * RS232 listener for the fare repeater controller path: opens {@code COM6}, reads UTF-8 text lines
 * terminated by newline, and dispatches each complete line to the application layer.
 * <p>
 * A dedicated daemon thread performs blocking reads. If the port drops or cannot be opened,
 * the service backs off and retries (auto-reconnect). When the port is unavailable at startup,
 * {@linkplain #isMockMode() mock mode} is active: no bytes are read, but the reconnect loop keeps
 * attempting so plugging in a device later succeeds without restart.
 * </p>
 */
public final class SerialListenerService {

    private static final Logger LOG = Logger.getLogger(SerialListenerService.class.getName());

    /** Pair with Supervisor Console Simulator wiring — COM6 on typical demo PCs. */
    public static final String DEFAULT_PORT_NAME = "COM6";
    private static final int BAUD = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;

    /** Pause between reconnect attempts when the link is down (ms). */
    private static final long RECONNECT_MS = 2_000;

    private final Consumer<String> lineConsumer;
    private final Consumer<String> logSink;
    private final Runnable heartbeatCallback;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile SerialPort port;
    private volatile boolean mockMode = true;
    private volatile int reconnectAttempts;

    private Thread worker;

    /**
     * @param lineConsumer invoked for each complete UTF-8 line (no trailing newline), not necessarily on FX thread
     * @param logSink      timestamped status lines for the controller dashboard
     * @param heartbeatCallback run after each successful read (e.g. pulse COM RX LED); may be null
     */
    public SerialListenerService(
            Consumer<String> lineConsumer,
            Consumer<String> logSink,
            Runnable heartbeatCallback
    ) {
        this.lineConsumer = Objects.requireNonNull(lineConsumer, "lineConsumer");
        this.logSink = Objects.requireNonNull(logSink, "logSink");
        this.heartbeatCallback = heartbeatCallback;
    }

    public boolean isMockMode() {
        return mockMode;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    private void log(String line) {
        logSink.accept(line);
    }

    /**
     * Starts the listener loop if not already running. Safe to call once at application bootstrap.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        worker = new Thread(this::runLoop, "qfrds-serial-listener");
        worker.setDaemon(true);
        worker.start();
        logTs("Serial listener thread started.");
    }

    /**
     * Stops the listener and closes the port.
     */
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
        closePortQuietly();
        logTs("Serial listener stopped.");
    }

    private void runLoop() {
        while (running.get()) {
            if (!openPortIfPossible()) {
                reconnectAttempts++;
                logTs("COM unavailable — mock listening mode (reconnect attempt " + reconnectAttempts + ").");
                sleepInterruptible(RECONNECT_MS);
                continue;
            }

            mockMode = false;
            logTs("RS232 listener active on " + DEFAULT_PORT_NAME + " @ " + BAUD + " 8N1 UTF-8.");

            try (InputStreamReader isr = new InputStreamReader(
                    Objects.requireNonNull(port).getInputStream(), StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {

                String line;
                while (running.get() && port != null && port.isOpen()) {
                    line = reader.readLine();
                    if (line == null) {
                        logTs("Serial stream closed — will reconnect.");
                        break;
                    }
                    if (heartbeatCallback != null) {
                        try {
                            heartbeatCallback.run();
                        } catch (Exception ex) {
                            LOG.log(Level.FINE, "heartbeat callback failed", ex);
                        }
                    }
                    lineConsumer.accept(line);
                }
            } catch (IOException ex) {
                LOG.log(Level.INFO, "Serial read error", ex);
                logTs("Serial read error: " + ex.getMessage() + " — reconnecting.");
            } finally {
                mockMode = true;
                closePortQuietly();
                if (running.get()) {
                    sleepInterruptible(RECONNECT_MS);
                }
            }
        }
    }

    private boolean openPortIfPossible() {
        closePortQuietly();

        final SerialPort candidate;
        try {
            candidate = SerialPort.getCommPort(DEFAULT_PORT_NAME);
        } catch (SerialPortInvalidPortException ex) {
            LOG.log(Level.FINE, "Port not found", ex);
            return false;
        }

        candidate.setBaudRate(BAUD);
        candidate.setNumDataBits(DATA_BITS);
        candidate.setNumStopBits(STOP_BITS);
        candidate.setParity(PARITY);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);

        if (!candidate.openPort()) {
            return false;
        }
        port = candidate;
        reconnectAttempts = 0;
        return true;
    }

    private void closePortQuietly() {
        SerialPort p = port;
        port = null;
        if (p != null && p.isOpen()) {
            try {
                p.closePort();
            } catch (Exception ex) {
                LOG.log(Level.FINE, "closePort", ex);
            }
        }
    }

    private static void sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void logTs(String message) {
        log(LogFormatter.ts(message));
    }
}
