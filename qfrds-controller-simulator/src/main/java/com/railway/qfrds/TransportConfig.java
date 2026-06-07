package com.railway.qfrds;

/**
 * Mock transport selection: RS232 (default) or TCP when {@code QFRDS_TRANSPORT=tcp}
 * or {@code -Dqfrds.transport=tcp}.
 */
public final class TransportConfig {

    private static final int DEFAULT_TCP_PORT = 9000;

    private TransportConfig() {
    }

    public static boolean useTcp() {
        return "tcp".equalsIgnoreCase(readMode());
    }

    public static String modeDescription() {
        return useTcp()
                ? "TCP (port " + tcpPort() + ")"
                : "SERIAL (" + SerialPortConfig.portName() + ")";
    }

    public static int tcpPort() {
        String env = firstNonBlank(System.getenv("QFRDS_TCP_PORT"), System.getProperty("qfrds.tcp.port"));
        if (env == null) {
            return DEFAULT_TCP_PORT;
        }
        try {
            return Integer.parseInt(env.trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_TCP_PORT;
        }
    }

    private static String readMode() {
        String mode = firstNonBlank(System.getenv("QFRDS_TRANSPORT"), System.getProperty("qfrds.transport"));
        return mode == null ? "serial" : mode.trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }
}
