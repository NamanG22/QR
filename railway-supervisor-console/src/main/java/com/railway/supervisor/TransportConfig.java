package com.railway.supervisor;

/**
 * Mock transport selection: RS232 (default) or TCP when {@code QFRDS_TRANSPORT=tcp}.
 */
public final class TransportConfig {

    private static final int DEFAULT_TCP_PORT = 9000;
    private static final String DEFAULT_TCP_HOST = "127.0.0.1";

    private TransportConfig() {
    }

    public static boolean useTcp() {
        String mode = System.getenv("QFRDS_TRANSPORT");
        return mode != null && mode.trim().equalsIgnoreCase("tcp");
    }

    public static String tcpHost() {
        String env = System.getenv("QFRDS_TCP_HOST");
        if (env == null || env.isBlank()) {
            return DEFAULT_TCP_HOST;
        }
        return env.trim();
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
