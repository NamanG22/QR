package com.railway.qfrds;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

/**
 * Resolves the controller serial port from {@code QFRDS_CONTROLLER_PORT} or the coded default,
 * and helps log what Windows actually exposes to jSerialComm.
 */
public final class SerialPortConfig {

    public static final String DEFAULT_PORT_NAME = "COM10";
    private static final String ENV_KEY = "QFRDS_CONTROLLER_PORT";

    private SerialPortConfig() {
    }

    public static String portName() {
        String env = System.getenv(ENV_KEY);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return DEFAULT_PORT_NAME;
    }

    public static SerialPort findPort(String portName) {
        try {
            return SerialPort.getCommPort(portName);
        } catch (SerialPortInvalidPortException ignored) {
            // fall through — scan enumerated ports
        }
        for (SerialPort p : SerialPort.getCommPorts()) {
            if (portName.equalsIgnoreCase(p.getSystemPortName())) {
                return p;
            }
        }
        return null;
    }

    public static String describeAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            return "(none detected — install com0com and create a pair, e.g. COM10 ↔ COM11)";
        }
        StringBuilder sb = new StringBuilder();
        for (SerialPort p : ports) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(p.getSystemPortName());
            String desc = p.getDescriptivePortName();
            if (desc != null && !desc.isBlank()) {
                sb.append(" (").append(desc).append(')');
            }
        }
        return sb.toString();
    }

    public static String openFailureDetail(SerialPort port) {
        return "";
    }

    /**
     * Other end of a com0com pair. Override with {@code QFRDS_PAIR_PORT} if needed.
     */
    public static String pairPortName(String primaryPort) {
        String env = System.getenv("QFRDS_PAIR_PORT");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        if ("COM10".equalsIgnoreCase(primaryPort)) {
            return "COM11";
        }
        if ("COM11".equalsIgnoreCase(primaryPort)) {
            return "COM10";
        }
        if ("COM3".equalsIgnoreCase(primaryPort)) {
            return "COM4";
        }
        if ("COM4".equalsIgnoreCase(primaryPort)) {
            return "COM3";
        }
        return null;
    }

    public static String linkLabel() {
        if (TransportConfig.useTcp()) {
            return "TCP:" + TransportConfig.tcpPort();
        }
        return portName();
    }
}
