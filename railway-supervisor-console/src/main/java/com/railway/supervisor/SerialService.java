package com.railway.supervisor;

import com.fazecast.jSerialComm.SerialPort;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wraps jSerialComm access for the supervisor console — keeps the port open and writes complete
 * packets in one synchronized operation with flush.
 */
public final class SerialService implements LineOutputService {

    private static final Logger LOG = Logger.getLogger(SerialService.class.getName());

    public static final String DEFAULT_PORT_NAME = SerialPortConfig.DEFAULT_PORT_NAME;
    private static final int BAUD = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;

    private final Consumer<String> logSink;
    private final String portName;
    private final Object writeLock = new Object();
    private SerialPort port;
    private boolean mockMode;

    public SerialService(Consumer<String> logSink) {
        this(logSink, null);
    }

    public SerialService(Consumer<String> logSink, String portNameOverride) {
        this.logSink = Objects.requireNonNull(logSink, "logSink");
        this.portName = (portNameOverride != null && !portNameOverride.isBlank())
                ? portNameOverride.trim()
                : SerialPortConfig.portName();
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
        return portName;
    }

    @Override
    public void connect() {
        synchronized (writeLock) {
            if (port != null && port.isOpen() && !mockMode) {
                return;
            }
            openPortInternal();
        }
    }

    private void openPortInternal() {
        closePortOnly();

        String target = portName;
        SerialPort candidate = SerialPortConfig.findPort(target);
        if (candidate == null) {
            mockMode = true;
            log("Serial connection failed: " + target + " not found.");
            log("Windows ports: " + SerialPortConfig.describeAvailablePorts());
            return;
        }

        candidate.setBaudRate(BAUD);
        candidate.setNumDataBits(DATA_BITS);
        candidate.setNumStopBits(STOP_BITS);
        candidate.setParity(PARITY);
        candidate.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 5_000);

        try {
            if (!candidate.openPort()) {
                mockMode = true;
                log("Serial connection failed: could not open " + target);
                log("Windows ports: " + SerialPortConfig.describeAvailablePorts());
                return;
            }
            port = candidate;
            mockMode = false;
            port.setDTR();
            port.setRTS();
            log("Connected to " + target + " (" + candidate.getDescriptivePortName()
                    + ", " + BAUD + " 8N1). Port stays open for all sends.");
        } catch (Exception ex) {
            mockMode = true;
            LOG.log(Level.WARNING, "Serial open failed", ex);
            log("Serial connection failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean sendLine(String payload) {
        Objects.requireNonNull(payload, "payload");
        byte[] bytes = (payload + "\r\n").getBytes(StandardCharsets.UTF_8);

        synchronized (writeLock) {
            if (mockMode || port == null || !port.isOpen()) {
                openPortInternal();
            }
            if (mockMode || port == null || !port.isOpen()) {
                log("[mock] Would send " + bytes.length + " bytes on serial.");
                return false;
            }

            try {
                port.setDTR();
                port.setRTS();
                if (!writeAll(bytes)) {
                    log("Packet send incomplete on " + portName + ".");
                    return false;
                }
                port.flushIOBuffers();
                return true;
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Serial write failed", ex);
                log("Packet send failed: " + ex.getMessage());
                mockMode = true;
                closePortOnly();
                return false;
            }
        }
    }

    private boolean writeAll(byte[] bytes) {
        int offset = 0;
        while (offset < bytes.length) {
            int written = port.writeBytes(bytes, bytes.length - offset, offset);
            if (written <= 0) {
                return false;
            }
            offset += written;
        }
        return true;
    }

    @Override
    public void disconnectQuietly() {
        synchronized (writeLock) {
            closePortOnly();
        }
    }

    private void closePortOnly() {
        if (port != null && port.isOpen()) {
            try {
                port.closePort();
            } catch (Exception ex) {
                LOG.log(Level.FINE, "Error closing port", ex);
            }
        }
        port = null;
        mockMode = true;
    }
}
