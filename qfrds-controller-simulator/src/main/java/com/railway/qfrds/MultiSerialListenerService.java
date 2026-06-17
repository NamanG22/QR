package com.railway.qfrds;

import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Listens on one or more COM ports. When {@code QFRDS_CONTROLLER_PORT} is unset, opens
 * <strong>every</strong> port Windows reports so lab wiring does not depend on guessing COM10.
 */
public final class MultiSerialListenerService implements LineInputService {

    private static final Logger LOG = Logger.getLogger(MultiSerialListenerService.class.getName());

    private static final int BAUD = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;
    private static final long RECONNECT_MS = 2_000;

    private final Consumer<String> lineConsumer;
    private final Consumer<String> logSink;
    private final Runnable heartbeatCallback;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<PortSession> openSessions = new CopyOnWriteArrayList<>();
    private final List<Thread> workers = new ArrayList<>();
    private volatile int reconnectAttempts;

    public MultiSerialListenerService(
            Consumer<String> lineConsumer,
            Consumer<String> logSink,
            Runnable heartbeatCallback
    ) {
        this.lineConsumer = Objects.requireNonNull(lineConsumer, "lineConsumer");
        this.logSink = Objects.requireNonNull(logSink, "logSink");
        this.heartbeatCallback = heartbeatCallback;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        String discovered = SerialPortConfig.describeAvailablePorts();
        SerialDiagLog.write("Available ports: " + discovered);

        if (SerialPortConfig.useExplicitPort()) {
            logTs("Serial listener — explicit port " + SerialPortConfig.explicitPortName());
        } else {
            logTs("Serial listener — auto mode on all ports: " + discovered);
        }

        for (String portName : SerialPortConfig.portsToListen()) {
            Thread worker = new Thread(() -> runPortLoop(portName), "qfrds-serial-" + portName);
            worker.setDaemon(true);
            workers.add(worker);
            worker.start();
        }
    }

    @Override
    public void stop() {
        running.set(false);
        for (Thread worker : workers) {
            worker.interrupt();
        }
        for (PortSession session : openSessions) {
            session.closeQuietly();
        }
        openSessions.clear();
        logTs("Serial listener stopped.");
    }

    @Override
    public boolean isMockMode() {
        return openSessions.isEmpty();
    }

    @Override
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    @Override
    public String linkLabel() {
        if (openSessions.isEmpty()) {
            return SerialPortConfig.useExplicitPort()
                    ? SerialPortConfig.explicitPortName()
                    : "AUTO";
        }
        return openSessions.stream()
                .map(PortSession::portName)
                .collect(Collectors.joining(", "));
    }

    /** Footer hint text for the passenger display. */
    public String statusHint() {
        if (!openSessions.isEmpty()) {
            return "listening on " + linkLabel();
        }
        String found = SerialPortConfig.describeAvailablePorts();
        if (found.startsWith("(none")) {
            return "no COM in Device Manager — plug RS232/USB adapter into thin client";
        }
        if (SerialPortConfig.useExplicitPort()) {
            return "cannot open " + SerialPortConfig.explicitPortName() + " — found: " + found;
        }
        return "cannot open any port — found: " + found;
    }

    private void runPortLoop(String portName) {
        while (running.get()) {
            PortSession session = openSession(portName);
            if (session == null) {
                reconnectAttempts++;
                if (reconnectAttempts == 1 || reconnectAttempts % 10 == 0) {
                    logTs(portName + " unavailable — " + statusHint());
                }
                sleepInterruptible(RECONNECT_MS);
                continue;
            }

            openSessions.add(session);
            reconnectAttempts = 0;
            logTs("RS232 listener active on " + portName + " @ " + BAUD + " 8N1 UTF-8.");
            SerialDiagLog.write("Listening on " + portName);

            try (InputStreamReader isr = new InputStreamReader(session.port.getInputStream(), StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {

                String line;
                while (running.get() && session.port.isOpen()) {
                    line = reader.readLine();
                    if (line == null) {
                        logTs(portName + " stream closed — will reconnect.");
                        break;
                    }
                    SerialDiagLog.write(portName + " RX " + line.length() + " chars: " + truncate(line, 200));
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
                LOG.log(Level.INFO, "Serial read error on " + portName, ex);
                logTs(portName + " read error: " + ex.getMessage() + " — reconnecting.");
            } finally {
                openSessions.remove(session);
                session.closeQuietly();
                if (running.get()) {
                    sleepInterruptible(RECONNECT_MS);
                }
            }
        }
    }

    private PortSession openSession(String portName) {
        SerialPort candidate = SerialPortConfig.findPort(portName);
        if (candidate == null) {
            return null;
        }

        candidate.setBaudRate(BAUD);
        candidate.setNumDataBits(DATA_BITS);
        candidate.setNumStopBits(STOP_BITS);
        candidate.setParity(PARITY);
        candidate.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 500, 0);

        if (!candidate.openPort()) {
            SerialDiagLog.write("Could not open " + portName + " (" + candidate.getDescriptivePortName() + ")");
            return null;
        }
        candidate.setDTR();
        candidate.setRTS();
        return new PortSession(portName, candidate);
    }

    private void logTs(String message) {
        logSink.accept(LogFormatter.ts(message));
    }

    private static void sleepInterruptible(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static final class PortSession {
        private final String portName;
        private final SerialPort port;

        private PortSession(String portName, SerialPort port) {
            this.portName = portName;
            this.port = port;
        }

        private String portName() {
            return portName;
        }

        private void closeQuietly() {
            if (port.isOpen()) {
                try {
                    port.closePort();
                } catch (Exception ex) {
                    LOG.log(Level.FINE, "closePort " + portName, ex);
                }
            }
        }
    }
}
