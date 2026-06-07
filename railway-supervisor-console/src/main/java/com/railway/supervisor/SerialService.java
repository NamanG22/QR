package com.railway.supervisor;

import com.fazecast.jSerialComm.SerialPort;

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
 *   <li>Open the configured port (default {@code COM10}) at 9600 8N1 — pair with controller on the other com0com end (e.g. COM11).</li>
 *   <li>Transmit UTF-8 text terminated by a newline so downstream parsers can frame records.</li>
 *   <li>Fall back to {@linkplain #isMockMode() mock mode} when the port is missing or busy.</li>
 * </ul>
 */
public final class SerialService implements LineOutputService {

    private static final Logger LOG = Logger.getLogger(SerialService.class.getName());

    /** TX side of com0com pair (default COM11; controller on COM10). Override: QFRDS_SUPERVISOR_PORT. */
    public static final String DEFAULT_PORT_NAME = SerialPortConfig.DEFAULT_PORT_NAME;
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

    @Override
    public boolean isMockMode() {
        return mockMode;
    }

    private void log(String line) {
        logSink.accept(line);
    }

    @Override
    public String linkLabel() {
        return SerialPortConfig.portName();
    }

    /**
     * Attempts to open {@link SerialPortConfig#portName()}. On failure the service stays in mock mode
     * so the demo UI remains usable without hardware.
     */
    @Override
    public void connect() {
        disconnectQuietly();

        String target = SerialPortConfig.portName();
        SerialPort candidate = SerialPortConfig.findPort(target);
        if (candidate == null) {
            mockMode = true;
            log("Serial connection failed: " + target + " not found.");
            log("Windows ports: " + SerialPortConfig.describeAvailablePorts());
            log("Running in mock mode — packets will be logged only.");
            return;
        }

        candidate.setBaudRate(BAUD);
        candidate.setNumDataBits(DATA_BITS);
        candidate.setNumStopBits(STOP_BITS);
        candidate.setParity(PARITY);
        candidate.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        // Blocking write with bounded wait avoids indefinite hangs on full UART buffers.
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 2000);

        try {
            if (!candidate.openPort()) {
                mockMode = true;
                String detail = SerialPortConfig.openFailureDetail(candidate);
                log("Serial connection failed: could not open " + target
                        + (detail.isEmpty() ? "" : " — " + detail));
                log("Port type: " + candidate.getDescriptivePortName());
                log("Windows ports: " + SerialPortConfig.describeAvailablePorts());
                log("Tip: use TCP on BOTH apps — set QFRDS_TRANSPORT=tcp (or mvn -Dqfrds.transport=tcp javafx:run).");
                log("Running in mock mode — packets will be logged only.");
                return;
            }
            port = candidate;
            mockMode = false;
            log("Connected to " + target + " (" + candidate.getDescriptivePortName()
                    + ", " + BAUD + " 8N1, UTF-8 + newline).");
        } catch (Exception ex) {
            mockMode = true;
            LOG.log(Level.WARNING, "Serial open failed", ex);
            log("Serial connection failed: " + ex.getMessage());
            log("Windows ports: " + SerialPortConfig.describeAvailablePorts());
            log("Running in mock mode — packets will be logged only.");
        }
    }

    /**
     * Sends {@code payload} as UTF-8 bytes followed by {@code \n}. In mock mode, nothing is written
     * to hardware; callers should still log success for demo continuity.
     */
    @Override
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

    @Override
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
