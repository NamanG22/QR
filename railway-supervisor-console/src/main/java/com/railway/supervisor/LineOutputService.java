package com.railway.supervisor;

/**
 * Sends {@code $<code><Length><Data>^} UTF-8 command frames to the controller over RS232.
 */
public interface LineOutputService {

    void connect();

    boolean sendLine(String payload);

    boolean isMockMode();

    void disconnectQuietly();

    String linkLabel();
}
