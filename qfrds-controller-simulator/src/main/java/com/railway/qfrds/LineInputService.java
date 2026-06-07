package com.railway.qfrds;

/**
 * Newline-delimited UTF-8 packet source (serial or TCP mock link).
 */
public interface LineInputService {

    void start();

    void stop();

    boolean isMockMode();

    int getReconnectAttempts();

    /** Short label for engineering UI, e.g. {@code COM10} or {@code TCP:9000}. */
    String linkLabel();
}
