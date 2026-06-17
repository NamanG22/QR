package com.railway.qfrds;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

/**
 * Resolves the controller RS232 RX port from {@code QFRDS_CONTROLLER_PORT} or the coded default.
 * Production: onboard RS232. Lab: may be a USB-serial adapter on the thin client if no native port.
 */
public final class SerialPortConfig {

    public static final String DEFAULT_PORT_NAME = "COM1";
    private static final String ENV_KEY = "QFRDS_CONTROLLER_PORT";

    private SerialPortConfig() {
    }

    public static boolean useExplicitPort() {
        String env = System.getenv(ENV_KEY);
        return env != null && !env.isBlank();
    }

    public static String explicitPortName() {
        return System.getenv(ENV_KEY).trim();
    }

    /**
     * Ports to open: one explicit port from env, or every port Windows exposes (lab auto mode).
     */
    public static String[] portsToListen() {
        if (useExplicitPort()) {
            return new String[] { explicitPortName() };
        }
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 0) {
            return new String[] { DEFAULT_PORT_NAME };
        }
        String[] names = new String[ports.length];
        for (int i = 0; i < ports.length; i++) {
            names[i] = ports[i].getSystemPortName();
        }
        java.util.Arrays.sort(names, SerialPortConfig::compareComPortNames);
        return names;
    }

    /** COM1 before COM2 before COM10, etc. */
    private static int compareComPortNames(String a, String b) {
        return Integer.compare(comPortNumber(a), comPortNumber(b));
    }

    private static int comPortNumber(String portName) {
        if (portName == null) {
            return Integer.MAX_VALUE;
        }
        String digits = portName.replaceAll("(?i)^COM", "").trim();
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return Integer.MAX_VALUE;
        }
    }

    public static String portName() {
        if (useExplicitPort()) {
            return explicitPortName();
        }
        String detected = detectUsbSerialPort();
        if (detected != null) {
            return detected;
        }
        String sole = detectSingleAvailablePort();
        if (sole != null) {
            return sole;
        }
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length > 0) {
            return "AUTO(" + ports.length + ")";
        }
        return DEFAULT_PORT_NAME;
    }

    /** First COM port whose description looks like a USB-serial or RS232 adapter. */
    public static String detectUsbSerialPort() {
        for (SerialPort p : SerialPort.getCommPorts()) {
            String desc = p.getDescriptivePortName();
            if (desc != null && looksLikeSerialAdapter(desc)) {
                return p.getSystemPortName();
            }
        }
        return null;
    }

    /** When the machine exposes exactly one serial port, use it (common on thin clients). */
    public static String detectSingleAvailablePort() {
        SerialPort[] ports = SerialPort.getCommPorts();
        if (ports.length == 1) {
            return ports[0].getSystemPortName();
        }
        return null;
    }

    private static boolean looksLikeSerialAdapter(String descriptiveName) {
        String lower = descriptiveName.toLowerCase();
        return lower.contains("usb")
                || lower.contains("serial")
                || lower.contains("communication")
                || lower.contains("dtech")
                || lower.contains("ftdi")
                || lower.contains("prolific")
                || lower.contains("ch340")
                || lower.contains("cp210")
                || lower.contains("rs232");
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
            return "(none detected — connect RS232 cable or set QFRDS_CONTROLLER_PORT to the correct COM port)";
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
     * com0com partner port — only used when {@code QFRDS_PAIR_PORT} is set explicitly.
     */
    public static String pairPortName(String primaryPort) {
        String env = System.getenv("QFRDS_PAIR_PORT");
        if (env != null && !env.isBlank()) {
            return env.trim();
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
