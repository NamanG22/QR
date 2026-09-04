package com.railway.qfrds;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

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
 * Event-driven RS232 listener (jSerialComm data listener). Keeps COM1 open continuously —
 * no read-timeout reconnects that drop packets mid-flight.
 */
public final class MultiSerialListenerService implements LineInputService {

    private static final Logger LOG = Logger.getLogger(MultiSerialListenerService.class.getName());

    private static final int BAUD = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = SerialPort.ONE_STOP_BIT;
    private static final int PARITY = SerialPort.NO_PARITY;
    private static final long RECONNECT_MS = 500;

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
        SerialDiagLog.write("Available ports: " + SerialPortConfig.describeAvailablePorts());

        for (String portName : SerialPortConfig.portsToListen()) {
            targetPorts.add(portName);
        }
        logTs("Serial listener — port(s): " + String.join(", ", targetPorts));

        for (String portName : targetPorts) {
            Thread worker = new Thread(() -> maintainPort(portName), "qfrds-serial-" + portName);
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

    public boolean isLinkLive() {
        return !openSessions.isEmpty();
    }

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
        return "waiting for " + linkLabel();
    }

    private boolean packetsReceivedRecently() {
        if (lastRxNanos == 0) {
            return false;
        }
        return System.nanoTime() - lastRxNanos < 30_000_000_000L;
    }

    private void maintainPort(String portName) {
        while (running.get()) {
            PortSession session = openSession(portName);
            if (session == null) {
                reconnectAttempts++;
                sleepInterruptible(RECONNECT_MS);
                continue;
            }

            openSessions.add(session);
            everHadSession = true;
            reconnectAttempts = 0;
            logTs("RS232 listener active on " + portName + " @ " + BAUD + " 8N1 (event-driven).");
            SerialDiagLog.write("Listening on " + portName);

            while (running.get() && session.port.isOpen() && !session.disconnected) {
                sleepInterruptible(1_000);
            }

            session.detachListener();
            openSessions.remove(session);
            session.closeQuietly();
            if (running.get()) {
                logTs(portName + " link down — reopening in " + RECONNECT_MS + "ms.");
                sleepInterruptible(RECONNECT_MS);
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
        candidate.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

        if (!candidate.openPort()) {
            SerialDiagLog.write("Could not open " + portName);
            return null;
        }
        candidate.setDTR();
        candidate.setRTS();
        drainStaleInput(candidate);

        PortSession session = new PortSession(portName, candidate);
        session.attachListener(this::onBytesReceived);
        return session;
    }

    private void onBytesReceived(String portName, byte[] data, int length) {
        SerialLineFramer framer = openSessions.stream()
                .filter(s -> s.portName.equals(portName))
                .findFirst()
                .map(s -> s.framer)
                .orElse(null);
        if (framer == null) {
            return;
        }
        for (String line : framer.takeLinesFromChunk(data, length)) {
            dispatchLine(portName, line);
        }
    }

    private void dispatchLine(String portName, String line) {
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

    private static void drainStaleInput(SerialPort port) {
        try {
            while (port.bytesAvailable() > 0) {
                int n = port.bytesAvailable();
                port.readBytes(new byte[n], n);
            }
        } catch (Exception ex) {
            LOG.log(Level.FINE, "drain input", ex);
        }
    }

    /**
     * Writes a reply on every open listen port (PRS ping 110 {@code Q}→{@code S}).
     */
    public boolean sendReply(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        boolean any = false;
        for (PortSession session : openSessions) {
            if (session.port == null || !session.port.isOpen()) {
                continue;
            }
            int written = session.port.writeBytes(bytes, bytes.length);
            if (written > 0) {
                any = true;
                try {
                    session.port.flushIOBuffers();
                } catch (Exception ex) {
                    LOG.log(Level.FINE, "flush reply", ex);
                }
            }
        }
        return any;
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

    private final class PortSession {
        private final String portName;
        private final SerialPort port;
        private final SerialLineFramer framer = new SerialLineFramer();
        private volatile boolean disconnected;
        private SerialPortDataListener listener;

        private PortSession(String portName, SerialPort port) {
            this.portName = portName;
            this.port = port;
        }

        private String portName() {
            return portName;
        }

        private void attachListener(ReceiveCallback callback) {
            listener = new SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE
                            | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
                }

                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                        disconnected = true;
                        return;
                    }
                    int available = port.bytesAvailable();
                    if (available <= 0) {
                        return;
                    }
                    byte[] buf = new byte[available];
                    int read = port.readBytes(buf, buf.length);
                    if (read > 0) {
                        callback.onReceive(portName, buf, read);
                    }
                }
            };
            port.addDataListener(listener);
        }

        private void detachListener() {
            if (listener != null) {
                try {
                    port.removeDataListener();
                } catch (Exception ex) {
                    LOG.log(Level.FINE, "remove listener", ex);
                }
                listener = null;
            }
        }

        private void closeQuietly() {
            detachListener();
            if (port.isOpen()) {
                try {
                    port.closePort();
                } catch (Exception ex) {
                    LOG.log(Level.FINE, "closePort " + portName, ex);
                }
            }
        }
    }

    @FunctionalInterface
    private interface ReceiveCallback {
        void onReceive(String portName, byte[] data, int length);
    }
}
