package com.railway.supervisor;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCP mock link to the controller. Set {@code QFRDS_TRANSPORT=tcp} and optionally
 * {@code QFRDS_TCP_HOST} / {@code QFRDS_TCP_PORT} (default {@code 127.0.0.1:9000}).
 */
public final class TcpOutputService implements LineOutputService {

    private static final Logger LOG = Logger.getLogger(TcpOutputService.class.getName());

    private final Consumer<String> logSink;
    private final String host;
    private final int port;
    private Socket socket;
    private OutputStream out;
    private boolean mockMode = true;

    public TcpOutputService(Consumer<String> logSink) {
        this(logSink, TransportConfig.tcpHost(), TransportConfig.tcpPort());
    }

    TcpOutputService(Consumer<String> logSink, String host, int port) {
        this.logSink = Objects.requireNonNull(logSink, "logSink");
        this.host = host;
        this.port = port;
    }

    @Override
    public boolean isMockMode() {
        return mockMode;
    }

    @Override
    public String linkLabel() {
        return "TCP:" + host + ":" + port;
    }

    private void log(String line) {
        logSink.accept(line);
    }

    @Override
    public void connect() {
        disconnectQuietly();
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), 5_000);
            socket = s;
            out = s.getOutputStream();
            mockMode = false;
            log("Connected to controller at " + host + ":" + port + " (TCP mock link).");
        } catch (IOException ex) {
            mockMode = true;
            LOG.log(Level.INFO, "TCP connect failed", ex);
            log("TCP connection failed: " + ex.getMessage());
            log("Start the controller first with QFRDS_TRANSPORT=tcp, then retry Generate.");
            log("Running in mock mode — packets will be logged only.");
        }
    }

    @Override
    public boolean sendLine(String payload) {
        Objects.requireNonNull(payload, "payload");
        byte[] bytes = (payload + "\n").getBytes(StandardCharsets.UTF_8);

        if (mockMode || out == null) {
            log("[mock] Would send " + bytes.length + " bytes over TCP.");
            return true;
        }

        try {
            out.write(bytes);
            out.flush();
            return true;
        } catch (IOException ex) {
            mockMode = true;
            LOG.log(Level.WARNING, "TCP write failed", ex);
            log("Packet send failed: " + ex.getMessage() + " — reconnecting.");
            disconnectQuietly();
            connect();
            if (!mockMode && out != null) {
                try {
                    out.write(bytes);
                    out.flush();
                    return true;
                } catch (IOException retryEx) {
                    log("Packet send failed after reconnect: " + retryEx.getMessage());
                    mockMode = true;
                    return false;
                }
            }
            return false;
        }
    }

    @Override
    public void disconnectQuietly() {
        if (socket != null) {
            try {
                socket.close();
                log("Disconnected from TCP link.");
            } catch (IOException ex) {
                LOG.log(Level.FINE, "TCP close", ex);
            }
        }
        socket = null;
        out = null;
    }
}
