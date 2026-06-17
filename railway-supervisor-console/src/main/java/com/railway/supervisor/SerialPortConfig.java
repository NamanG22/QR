package com.railway.supervisor;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortInvalidPortException;

/**
 * Resolves the supervisor TX port from {@code QFRDS_SUPERVISOR_PORT} or the coded default.
 * In the lab this is usually a USB-to-RS232 adapter; in production it is the CRIS RS232 port.
 */
public final class SerialPortConfig {

    public static final String DEFAULT_PORT_NAME = "COM11";
    private static final String ENV_KEY = "QFRDS_SUPERVISOR_PORT";

    private SerialPortConfig() {
    }

    public static String portName() {
        String env = System.getenv(ENV_KEY);
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        String detected = detectUsbSerialPort();
        if (detected != null) {
            return detected;
        }
        return DEFAULT_PORT_NAME;
    }

    /**
     * First COM port whose description looks like a USB-serial adapter (e.g. DTECH USB-Serial → COM5).
     */
    public static String detectUsbSerialPort() {
        for (SerialPort p : SerialPort.getCommPorts()) {
            String desc = p.getDescriptivePortName();
            if (desc != null && looksLikeUsbSerial(desc)) {
                return p.getSystemPortName();
            }
        }
        return null;
    }

    private static boolean looksLikeUsbSerial(String descriptiveName) {
        String lower = descriptiveName.toLowerCase();
        return lower.contains("usb")
                || lower.contains("serial")
                || lower.contains("dtech")
                || lower.contains("ftdi")
                || lower.contains("prolific")
                || lower.contains("ch340")
                || lower.contains("cp210");
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
            return "(none detected — plug in a USB-serial adapter or check Device Manager → Ports)";
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
