package com.railway.qfrds;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * UPI payment QR settings. Defaults are hardcoded for the demo; override with
 * {@code QFRDS_UPI_VPA} / {@code QFRDS_UPI_PAYEE_NAME} if needed.
 */
public final class UpiQrConfig {

    private static final String DEFAULT_VPA = "9289704566@ptsbi";
    private static final String DEFAULT_PAYEE_NAME = "Naman Garg";

    private static final String ENV_VPA = "QFRDS_UPI_VPA";
    private static final String ENV_PAYEE_NAME = "QFRDS_UPI_PAYEE_NAME";
    private static final String ENV_STATIC_AMOUNT = "QFRDS_UPI_STATIC_AMOUNT";

    private UpiQrConfig() {
    }

    public static boolean isConfigured() {
        String vpa = vpa();
        return vpa != null && !vpa.isBlank();
    }

    public static String vpa() {
        return firstNonBlank(
                System.getenv(ENV_VPA),
                System.getProperty("qfrds.upi.vpa"),
                DEFAULT_VPA);
    }

    public static String payeeName() {
        return firstNonBlank(
                System.getenv(ENV_PAYEE_NAME),
                System.getProperty("qfrds.upi.payeeName"),
                DEFAULT_PAYEE_NAME);
    }

    /** When set, QR always uses this amount instead of the ticket fare. */
    public static String staticAmount() {
        return firstNonBlank(System.getenv(ENV_STATIC_AMOUNT), System.getProperty("qfrds.upi.staticAmount"));
    }

    /**
     * NPCI UPI deep link, e.g. {@code upi://pay?pa=merchant@upi&pn=Name&am=120.00&cu=INR&tn=TX123}.
     */
    public static String buildPaymentUri(TicketData ticket) {
        String amount = staticAmount();
        if (amount == null || amount.isBlank()) {
            amount = formatAmount(ticket.getFare());
        }
        String txnNote = ticket.getTransactionId() + " " + ticket.getSourceStation()
                + "-" + ticket.getDestinationStation();

        StringBuilder uri = new StringBuilder("upi://pay?");
        uri.append("pa=").append(encode(vpa()));
        uri.append("&pn=").append(encode(payeeName()));
        uri.append("&am=").append(encode(amount));
        uri.append("&cu=INR");
        uri.append("&tn=").append(encode(txnNote));
        return uri.toString();
    }

    private static String formatAmount(String fare) {
        if (fare == null || fare.isBlank()) {
            return "0.00";
        }
        String trimmed = fare.trim().replace("₹", "").trim();
        try {
            double value = Double.parseDouble(trimmed);
            return String.format("%.2f", value);
        } catch (NumberFormatException ex) {
            return trimmed;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }
}
