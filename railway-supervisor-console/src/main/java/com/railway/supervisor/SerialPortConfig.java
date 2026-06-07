package com.railway.supervisor;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

/**
 * Resolves the supervisor serial port from {@code QFRDS_SUPERVISOR_PORT} or the coded default,
 * and helps log what Windows actually exposes to jSerialComm.
 */
public final class SerialPortConfig {

    public static final String DEFAULT_PORT_NAME = "COM10";
    private static final String ENV_KEY = "QFRDS_SUPERVISOR_PORT";

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
}
