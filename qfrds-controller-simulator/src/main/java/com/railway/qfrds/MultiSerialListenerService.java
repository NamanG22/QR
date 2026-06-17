package com.railway.qfrds;

import com.fazecast.jSerialComm.SerialPort;

import java.io.ByteArrayOutputStream;
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
 * Stable RS232 listener — one port by default ({@code COM1}), blocking reads, no false reconnects
 * on idle timeout.
 */
public final class MultiSerialListenerService implements LineInputService {

    private static final Logger LOG = Logger.getLogger(MultiSerialListenerService.class.getName());

    private static final int BAUD = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;
    private static final long RECONNECT_MS = 3_000;
    private static final int READ_BUF_SIZE = 512;

    private final Consumer<String> lineConsumer;
    private final Consumer<String> logSink;
    private final Runnable heartbeatCallback;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<PortSession> openSessions = new CopyOnWriteArrayList<>();
    private final List<Thread> workers = new ArrayList<>();
    private final List<String> targetPorts = new ArrayList<>();

    private volatile int reconnectAttempts;
    private volatile boolean everHadSession;
    private volatile long lastRxNanos;

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

        for (String portName : SerialPortConfig.portsToListen()) {
            targetPorts.add(portName);
        }
        logTs("Serial listener — port(s): " + String.join(", ", targetPorts));

        for (String portName : targetPorts) {
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
        return !isLinkLive() && !isReconnecting();
    }

    /** True while at least one COM port is open and reading. */
    public boolean isLinkLive() {
        return !openSessions.isEmpty();
    }

    /** True after first successful open — avoids footer flicker during short reconnect gaps. */
    public boolean isReconnecting() {
        return everHadSession && openSessions.isEmpty() && running.get();
    }

    @Override
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    @Override
    public String linkLabel() {
        if (!openSessions.isEmpty()) {
            return openSessions.stream()
                    .map(PortSession::portName)
                    .collect(Collectors.joining(", "));
        }
        if (!targetPorts.isEmpty()) {
            return String.join(", ", targetPorts);
        }
        return SerialPortConfig.DEFAULT_PORT_NAME;
    }

    public String statusHint() {
        if (isLinkLive()) {
            return packetsReceivedRecently()
                    ? "receiving on " + linkLabel()
                    : "listening on " + linkLabel();
        }
        if (isReconnecting()) {
            return "reconnecting to " + linkLabel() + "…";
        }
        String found = SerialPortConfig.describeAvailablePorts();
        if (found.startsWith("(none")) {
            return "no COM in Device Manager";
        }
        return "waiting for " + linkLabel();
    }

    private boolean packetsReceivedRecently() {
        if (lastRxNanos == 0) {
            return false;
        }
        return System.nanoTime() - lastRxNanos < 30_000_000_000L;
    }

    private void runPortLoop(String portName) {
        while (running.get()) {
            PortSession session = openSession(portName);
            if (session == null) {
                reconnectAttempts++;
                if (reconnectAttempts == 1 || reconnectAttempts % 15 == 0) {
                    logTs(portName + " unavailable (attempt " + reconnectAttempts + ")");
                }
                sleepInterruptible(RECONNECT_MS);
                continue;
            }

            openSessions.add(session);
            everHadSession = true;
            reconnectAttempts = 0;
            logTs("RS232 listener active on " + portName + " @ " + BAUD + " 8N1 UTF-8.");
            SerialDiagLog.write("Listening on " + portName);

            try {
                readLines(session);
            } catch (Exception ex) {
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

    /**
     * Blocking byte read + manual newline framing — avoids BufferedReader returning null on
     * semi-blocking timeouts (which caused false disconnects and missed packets).
     */
    private void readLines(PortSession session) throws Exception {
        SerialPort port = session.port;
        byte[] buf = new byte[READ_BUF_SIZE];
        ByteArrayOutputStream lineBuf = new ByteArrayOutputStream(256);

        while (running.get() && port.isOpen()) {
            int n = port.readBytes(buf, buf.length);
            if (n < 0) {
                logTs(session.portName + " stream ended — will reconnect.");
                break;
            }
            if (n == 0) {
                continue;
            }

            for (int i = 0; i < n; i++) {
                byte b = buf[i];
                if (b == '\n') {
                    dispatchLine(session.portName, lineBuf);
                    lineBuf.reset();
                } else if (b != '\r') {
                    lineBuf.write(b);
                }
            }
        }
    }

    private void dispatchLine(String portName, ByteArrayOutputStream lineBuf) {
        if (lineBuf.size() == 0) {
            return;
        }
        String line = lineBuf.toString(StandardCharsets.UTF_8).trim();
        if (line.isEmpty()) {
            return;
        }
        lastRxNanos = System.nanoTime();
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
        // Block until a byte arrives — do not use read timeout (idle port is normal).
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

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
