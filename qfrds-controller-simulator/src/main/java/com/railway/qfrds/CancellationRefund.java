package com.railway.qfrds;

import java.util.Optional;

/**
 * Cancellation refund (command 14): {@code CANC} + {@code RFND} + 5-digit amount.
 */
public final class CancellationRefund {

    private final String code;
    private final String type;
    private final String amount;

    public CancellationRefund(String code, String type, String amount) {
        this.code = code;
        this.type = type;
        this.amount = amount;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    /** Amount without leading zeros, e.g. {@code 790}. */
    public String getAmount() {
        return String.valueOf(Integer.parseInt(amount));
    }

    public String pack() {
        return code + type + amount;
    }

    public static Optional<CancellationRefund> unpack(String data) {
        if (data == null || data.length() != 13) {
            return Optional.empty();
        }
        String code = data.substring(0, 4);
        String type = data.substring(4, 8);
        String amount = data.substring(8, 13);
        if (!CommandSet.REFUND_CODE_VALUE.equals(code)
                || !CommandSet.REFUND_TYPE_VALUE.equals(type)
                || !amount.matches("\\d{5}")) {
            return Optional.empty();
        }
        return Optional.of(new CancellationRefund(code, type, amount));
    }
}
