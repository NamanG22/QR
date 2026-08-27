package com.railway.qfrds;

import java.util.Optional;

/**
 * Operator details (command 15): {@code name:terminal:window:shift} (colon-separated).
 */
public final class OperatorSession {

    private final String operatorName;
    private final String terminalId;
    private final String windowNo;
    private final String shiftNo;

    public OperatorSession(String operatorName, String terminalId, String windowNo, String shiftNo) {
        this.operatorName = operatorName == null ? "" : operatorName.trim();
        this.terminalId = terminalId == null ? "" : terminalId.trim();
        this.windowNo = windowNo == null ? "" : windowNo.trim();
        this.shiftNo = shiftNo == null ? "" : shiftNo.trim();
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public String getWindowNo() {
        return windowNo;
    }

    public String getShiftNo() {
        return shiftNo;
    }

    public String windowDisplay() {
        if (windowNo.isBlank()) {
            return shiftNo.isBlank() ? "" : shiftNo;
        }
        return shiftNo.isBlank() ? windowNo : windowNo + "-" + shiftNo;
    }

    public String pack() {
        return operatorName + ":" + terminalId + ":" + windowNo + ":" + shiftNo;
    }

    public static Optional<OperatorSession> packFrom(
            String operatorName, String terminalId, String windowRaw, String shiftRaw) {
        if (operatorName == null || operatorName.isBlank()
                || terminalId == null || terminalId.isBlank()) {
            return Optional.empty();
        }
        String name = operatorName.trim();
        String terminal = terminalId.trim();
        if (name.indexOf(':') >= 0 || terminal.indexOf(':') >= 0) {
            return Optional.empty();
        }
        if (name.codePointCount(0, name.length()) > CommandSet.OPERATOR_NAME_CHARS) {
            return Optional.empty();
        }
        if (terminal.codePointCount(0, terminal.length()) > CommandSet.TERMINAL_CHARS) {
            return Optional.empty();
        }
        int window;
        int shift;
        try {
            window = Integer.parseInt(windowRaw == null ? "" : windowRaw.trim());
            shift = Integer.parseInt(shiftRaw == null ? "" : shiftRaw.trim());
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
        if (window < 0 || window > 999 || shift < 0 || shift > 9) {
            return Optional.empty();
        }
        return Optional.of(new OperatorSession(name, terminal, String.valueOf(window), String.valueOf(shift)));
    }

    public static Optional<OperatorSession> unpack(String data) {
        if (data == null || data.isBlank()) {
            return Optional.empty();
        }
        String raw = data;
        while (raw.endsWith(":")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        String[] parts = raw.split(":", -1);
        if (parts.length != 4) {
            return Optional.empty();
        }
        String name = parts[0].trim();
        String terminal = parts[1].trim();
        String window = parts[2].trim();
        String shift = parts[3].trim();
        if (name.isEmpty() || terminal.isEmpty() || !window.matches("\\d{1,3}") || !shift.matches("\\d")) {
            return Optional.empty();
        }
        return Optional.of(new OperatorSession(name, terminal, window, shift));
    }
}
