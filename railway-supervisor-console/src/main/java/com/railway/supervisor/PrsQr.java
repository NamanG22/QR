package com.railway.supervisor;

/**
 * PRS QR payload (sub-function 112): {@code <upi string>$<display message>}.
 * Split on the first {@code $} in the body (UPI itself has no {@code $}).
 */
public final class PrsQr {

    public static final int QR_MAX = 350;
    public static final int MESSAGE_MAX = 100;

    private PrsQr() {
    }

    public static String pack(String qr, String message) {
        String left = qr == null ? "" : qr;
        if (left.length() > QR_MAX) {
            left = left.substring(0, QR_MAX);
        }
        String right = message == null ? "" : message;
        if (right.length() > MESSAGE_MAX) {
            right = right.substring(0, MESSAGE_MAX);
        }
        return left + '$' + right;
    }

    public static Split unpack(String body) {
        if (body == null) {
            return new Split("", "");
        }
        int sep = body.indexOf('$');
        if (sep < 0) {
            return new Split(body.trim(), "");
        }
        return new Split(body.substring(0, sep).trim(), body.substring(sep + 1).trim());
    }

    public record Split(String qr, String message) {
    }
}
