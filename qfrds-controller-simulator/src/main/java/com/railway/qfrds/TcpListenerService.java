package com.railway.qfrds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TCP mock link for the controller: listens for one supervisor client and reads UTF-8 lines.
 * Use {@code QFRDS_TRANSPORT=tcp} on both apps (same packet format as RS232).
 */
public final class TcpListenerService implements LineInputService {

    private static final Logger LOG = Logger.getLogger(TcpListenerService.class.getName());
    private static final long RECONNECT_MS = 2_000;

    private final int port;
    private final Consumer<String> lineConsumer;
    private final Consumer<String> logSink;
    private final Runnable heartbeatCallback;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile boolean mockMode = true;
    private volatile int reconnectAttempts;
    private volatile ServerSocket serverSocket;
    private Thread worker;

    public TcpListenerService(
            int port,
            Consumer<String> lineConsumer,
            Consumer<String> logSink,
            Runnable heartbeatCallback
    ) {
        this.port = port;
        this.lineConsumer = Objects.requireNonNull(lineConsumer, "lineConsumer");
        this.logSink = Objects.requireNonNull(logSink, "logSink");
        this.heartbeatCallback = heartbeatCallback;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        worker = new Thread(this::runLoop, "qfrds-tcp-listener");
        worker.setDaemon(true);
        worker.start();
        logTs("TCP listener thread started — port " + port + " (QFRDS_TRANSPORT=tcp).");
    }

    @Override
    public void stop() {
        running.set(false);
        if (worker != null) {
            worker.interrupt();
        }
        closeServerQuietly();
        logTs("TCP listener stopped.");
    }

    @Override
    public boolean isMockMode() {
        return mockMode;
    }

    @Override
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    @Override
    public String linkLabel() {
        return "TCP:" + port;
    }

    private void runLoop() {
        while (running.get()) {
            try (ServerSocket server = new ServerSocket()) {
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress("0.0.0.0", port));
                serverSocket = server;
                mockMode = false;
                reconnectAttempts = 0;
                logTs("TCP listener active on 0.0.0.0:" + port + " — waiting for supervisor.");

                while (running.get()) {
                    Socket client = server.accept();
                    logTs("Supervisor connected from " + client.getRemoteSocketAddress() + ".");
                    handleClient(client);
                }
            } catch (IOException ex) {
                if (!running.get()) {
                    break;
                }
                mockMode = true;
                reconnectAttempts++;
                LOG.log(Level.INFO, "TCP listen error", ex);
                logTs("TCP unavailable — mock mode (reconnect attempt " + reconnectAttempts + "): "
                        + ex.getMessage());
                sleepInterruptible(RECONNECT_MS);
            } finally {
                mockMode = true;
                closeServerQuietly();
            }
        }
    }

    private void handleClient(Socket client) {
        try (Socket c = client;
             InputStreamReader isr = new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {
            String line;
            while (running.get() && !c.isClosed()) {
                line = reader.readLine();
                if (line == null) {
                    logTs("Supervisor disconnected — waiting for reconnect.");
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
            if (running.get()) {
                LOG.log(Level.INFO, "TCP client read error", ex);
                logTs("TCP read error: " + ex.getMessage());
            }
        }
    }

    private void closeServerQuietly() {
        ServerSocket s = serverSocket;
        serverSocket = null;
        if (s != null && !s.isClosed()) {
            try {
                s.close();
            } catch (IOException ex) {
                LOG.log(Level.FINE, "close server", ex);
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
        logSink.accept(LogFormatter.ts(message));
    }
}
