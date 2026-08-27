package com.railway.supervisor;

/**
 * Sends newline-delimited UTF-8 packets to the controller over RS232.
 */
public interface LineOutputService {

    void connect();

    boolean sendLine(String payload);

    boolean isMockMode();

    void disconnectQuietly();

    String linkLabel();
}
