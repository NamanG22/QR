package com.railway.qfrds;

import com.fazecast.jSerialComm.SerialPort;

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
 * RS232 listener for the fare repeater controller: opens the configured port (default {@code COM10}),
 * reads UTF-8 text lines terminated by newline, and dispatches each complete line to the application layer.
 * <p>
 * Production: direct RS232 from the CRIS/supervisor terminal. Lab: USB-serial on the console PC wired
 * to the controller RS232 input (null-modem or straight-through per your cable).
 * </p>
 */
public final class SerialListenerService implements LineInputService {

    private static final Logger LOG = Logger.getLogger(SerialListenerService.class.getName());

    /** Controller RS232 RX port (default COM10). Override: QFRDS_CONTROLLER_PORT. */
    public static final String DEFAULT_PORT_NAME = SerialPortConfig.DEFAULT_PORT_NAME;

    private volatile SerialPort pairHoldPort;
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

    @Override
    public boolean isMockMode() {
        return mockMode;
    }

    @Override
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    private void log(String line) {
        logSink.accept(line);
    }

    @Override
    public String linkLabel() {
        return SerialPortConfig.portName();
    }

    /**
     * Starts the listener loop if not already running. Safe to call once at application bootstrap.
     */
    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        worker = new Thread(this::runLoop, "qfrds-serial-listener");
        worker.setDaemon(true);
        worker.start();
        logTs("Serial listener thread started — target " + SerialPortConfig.portName()
                + " (set QFRDS_CONTROLLER_PORT to override).");
    }

    /**
     * Stops the listener and closes the port.
     */
    @Override
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
        closePortQuietly();
        closePairHoldQuietly();
        logTs("Serial listener stopped.");
    }

    private void runLoop() {
        while (running.get()) {
            if (!openPortIfPossible()) {
                reconnectAttempts++;
                if (reconnectAttempts == 1 || reconnectAttempts % 10 == 0) {
                    logPortDiagnostics();
                }
                logTs("COM unavailable — mock listening mode (reconnect attempt " + reconnectAttempts + ").");
                sleepInterruptible(RECONNECT_MS);
                continue;
            }

            mockMode = false;
            logTs("RS232 listener active on " + SerialPortConfig.portName() + " @ " + BAUD + " 8N1 UTF-8.");

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

        String target = SerialPortConfig.portName();
        openPairHoldIfNeeded(target);

        SerialPort candidate = SerialPortConfig.findPort(target);
        if (candidate == null) {
            return false;
        }

        candidate.setBaudRate(BAUD);
        candidate.setNumDataBits(DATA_BITS);
        candidate.setNumStopBits(STOP_BITS);
        candidate.setParity(PARITY);
        candidate.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);

        if (!candidate.openPort()) {
            lastOpenFailure = "openPort returned false";
            return false;
        }
        lastOpenFailure = "";
        port = candidate;
        reconnectAttempts = 0;
        return true;
    }

    /** Some com0com CNCB ports stay busy until the paired CNCA port is opened first. */
    private void openPairHoldIfNeeded(String primaryPort) {
        String pair = SerialPortConfig.pairPortName(primaryPort);
        if (pair == null || pair.equalsIgnoreCase(primaryPort)) {
            return;
        }
        if (pairHoldPort != null && pairHoldPort.isOpen()) {
            return;
        }
        closePairHoldQuietly();
        SerialPort partner = SerialPortConfig.findPort(pair);
        if (partner == null) {
            return;
        }
        partner.setBaudRate(BAUD);
        partner.setNumDataBits(DATA_BITS);
        partner.setNumStopBits(STOP_BITS);
        partner.setParity(PARITY);
        partner.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        partner.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);
        if (partner.openPort()) {
            pairHoldPort = partner;
            logTs("Opened pair partner " + pair + " (com0com hold) before " + primaryPort + ".");
        }
    }

    private void closePairHoldQuietly() {
        SerialPort p = pairHoldPort;
        pairHoldPort = null;
        if (p != null && p.isOpen()) {
            try {
                p.closePort();
            } catch (Exception ex) {
                LOG.log(Level.FINE, "close pair hold", ex);
            }
        }
    }

    private volatile String lastOpenFailure = "";

    private void logPortDiagnostics() {
        String target = SerialPortConfig.portName();
        SerialPort candidate = SerialPortConfig.findPort(target);
        if (candidate == null) {
            logTs(target + " not found. Windows ports: " + SerialPortConfig.describeAvailablePorts());
            return;
        }
        String detail = lastOpenFailure.isEmpty()
                ? SerialPortConfig.openFailureDetail(candidate)
                : lastOpenFailure;
        logTs(target + " found as "
                + candidate.getDescriptivePortName()
                + " but could not open"
                + (detail.isEmpty() ? " (port busy or access denied — close other apps, kill java.exe)."
                : ": " + detail));
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
