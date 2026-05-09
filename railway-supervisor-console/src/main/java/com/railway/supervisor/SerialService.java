package com.railway.supervisor;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps jSerialComm access for the supervisor console.
 * <p>
 * Serial layer responsibilities:
 * </p>
 * <ul>
 *   <li>Open the configured port (default {@code COM3}) at 9600 8N1 — pair with controller on the other com0com end (e.g. COM4).</li>
 *   <li>Transmit UTF-8 text terminated by a newline so downstream parsers can frame records.</li>
 *   <li>Fall back to {@linkplain #isMockMode() mock mode} when the port is missing or busy.</li>
 * </ul>
 */
public final class SerialService {

    private static final Logger LOG = Logger.getLogger(SerialService.class.getName());

    /** TX side of virtual pair with QFRDS controller (com0com: pair with e.g. COM4). */
    public static final String DEFAULT_PORT_NAME = "COM3";
    private static final int BAUD = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;

    private final Consumer<String> logSink;
    private SerialPort port;
    private boolean mockMode;

    /**
     * @param logSink receives human-readable lines for the UI log {@link javafx.scene.control.TextArea}
     */
    public SerialService(Consumer<String> logSink) {
        this.logSink = Objects.requireNonNull(logSink, "logSink");
    }

    public boolean isMockMode() {
        return mockMode;
    }

    private void log(String line) {
        logSink.accept(line);
    }

    /**
     * Attempts to open {@link #DEFAULT_PORT_NAME}. On failure the service stays in mock mode
     * so the demo UI remains usable without hardware.
     */
    public void connect() {
        disconnectQuietly();

        final SerialPort candidate;
        try {
            candidate = SerialPort.getCommPort(DEFAULT_PORT_NAME);
        } catch (SerialPortInvalidPortException ex) {
            mockMode = true;
            LOG.log(Level.FINE, "Unknown serial port", ex);
            log("Serial connection failed: port not available (" + DEFAULT_PORT_NAME + ").");
            log("Running in mock mode — packets will be logged only.");
            return;
        }

        candidate.setBaudRate(BAUD);
        candidate.setNumDataBits(DATA_BITS);
        candidate.setNumStopBits(STOP_BITS);
        candidate.setParity(PARITY);
        // Blocking write with bounded wait avoids indefinite hangs on full UART buffers.
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 2000);

        try {
            if (!candidate.openPort()) {
                mockMode = true;
                log("Serial connection failed: could not open " + DEFAULT_PORT_NAME);
                log("Running in mock mode — packets will be logged only.");
                return;
            }
            port = candidate;
            mockMode = false;
            log("Connected to " + DEFAULT_PORT_NAME + " (" + BAUD + " 8N1, UTF-8 + newline).");
        } catch (Exception ex) {
            mockMode = true;
            LOG.log(Level.WARNING, "Serial open failed", ex);
            log("Serial connection failed: " + ex.getMessage());
            log("Running in mock mode — packets will be logged only.");
        }
    }

    /**
     * Sends {@code payload} as UTF-8 bytes followed by {@code \n}. In mock mode, nothing is written
     * to hardware; callers should still log success for demo continuity.
     */
    public boolean sendLine(String payload) {
        Objects.requireNonNull(payload, "payload");
        byte[] bytes = (payload + "\n").getBytes(StandardCharsets.UTF_8);

        if (mockMode || port == null || !port.isOpen()) {
            log("[mock] Would send " + bytes.length + " bytes on serial.");
            return true;
        }

        try {
            int written = port.writeBytes(bytes, bytes.length);
            if (written != bytes.length) {
                log("Packet send incomplete: wrote " + written + "/" + bytes.length + " bytes.");
                return false;
            }
            return true;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Serial write failed", ex);
            log("Packet send failed: " + ex.getMessage());
            return false;
        }
    }

    public void disconnectQuietly() {
        if (port != null && port.isOpen()) {
            try {
                port.closePort();
                log("Disconnected from serial port.");
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Error closing port", ex);
            }
        }
        port = null;
    }
}
