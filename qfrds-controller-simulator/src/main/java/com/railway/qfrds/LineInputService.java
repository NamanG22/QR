package com.railway.qfrds;

/**
 * {@code $...^} command-frame source (RS232).
 */
public interface LineInputService {

    void start();

    void stop();

    boolean isMockMode();

    int getReconnectAttempts();

    /** Short label for engineering UI, e.g. {@code COM10}. */
    String linkLabel();
}
