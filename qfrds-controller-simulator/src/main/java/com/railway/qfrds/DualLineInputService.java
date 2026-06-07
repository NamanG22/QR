package com.railway.qfrds;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs serial and TCP listeners together so the thin client accepts network packets
 * without any env vars. Same line handler receives from either path.
 */
public final class DualLineInputService implements LineInputService {

    private final List<LineInputService> delegates = new ArrayList<>();

    public DualLineInputService(
            Consumer<String> lineConsumer,
            Consumer<String> logSink,
            Runnable heartbeatCallback
    ) {
        delegates.add(new TcpListenerService(
                TransportConfig.tcpPort(),
                lineConsumer,
                logSink,
                heartbeatCallback
        ));
        delegates.add(new SerialListenerService(lineConsumer, logSink, heartbeatCallback));
    }

    @Override
    public void start() {
        for (LineInputService d : delegates) {
            d.start();
        }
    }

    @Override
    public void stop() {
        for (LineInputService d : delegates) {
            d.stop();
        }
    }

    @Override
    public boolean isMockMode() {
        return delegates.stream().noneMatch(d -> !d.isMockMode());
    }

    @Override
    public int getReconnectAttempts() {
        return delegates.stream().mapToInt(LineInputService::getReconnectAttempts).sum();
    }

    @Override
    public String linkLabel() {
        return "TCP:" + TransportConfig.tcpPort() + " + " + SerialPortConfig.portName();
    }

    /** True when the TCP server socket is listening (network path ready). */
    public boolean isTcpReady() {
        for (LineInputService d : delegates) {
            if (d instanceof TcpListenerService tcp) {
                return !tcp.isMockMode();
            }
        }
        return false;
    }
}
