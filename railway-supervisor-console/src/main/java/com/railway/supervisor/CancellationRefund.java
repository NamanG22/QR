package com.railway.supervisor;

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

    public static Optional<CancellationRefund> packFromAmount(String amountRaw) {
        Optional<String> digits = FareField.pack(amountRaw);
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        int n = Integer.parseInt(digits.get());
        if (n > 99_999) {
            return Optional.empty();
        }
        String padded = String.format("%0" + CommandSet.REFUND_AMOUNT_DIGITS + "d", n);
        return Optional.of(new CancellationRefund(
                CommandSet.REFUND_CODE_VALUE,
                CommandSet.REFUND_TYPE_VALUE,
                padded
        ));
    }

    public String pack() {
        return StationField.pad(code, 4)
                + StationField.pad(type, 4)
                + amount;
    }

    public String getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    public String getAmount() {
        return String.valueOf(Integer.parseInt(amount));
    }

    public String getAmountPadded() {
        return amount;
    }
}
