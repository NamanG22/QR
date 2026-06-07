package com.railway.qfrds;

/**
 * Mock transport selection: RS232 (default) or TCP when {@code QFRDS_TRANSPORT=tcp}.
 */
public final class TransportConfig {

    private static final int DEFAULT_TCP_PORT = 9000;

    private TransportConfig() {
    }

    public static boolean useTcp() {
        String mode = System.getenv("QFRDS_TRANSPORT");
        return mode != null && mode.trim().equalsIgnoreCase("tcp");
    }

    public static int tcpPort() {
        String env = System.getenv("QFRDS_TCP_PORT");
        if (env == null || env.isBlank()) {
            return DEFAULT_TCP_PORT;
        }
        try {
            return Integer.parseInt(env.trim());
        } catch (NumberFormatException ex) {
            return DEFAULT_TCP_PORT;
        }
    }
}
