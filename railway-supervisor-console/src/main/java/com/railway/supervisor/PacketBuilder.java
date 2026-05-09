package com.railway.supervisor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Builds the UTF-8 serial payload as a single line for the fare repeater / QR demo listener.
 * <p>
 * Packet grammar (pipe-separated key=value segments):
 * </p>
 * <pre>
 * TYPE=&lt;UTS|PRS&gt;|SRC=&lt;source&gt;|DST=&lt;destination&gt;|FARE=&lt;fare&gt;|TXN=&lt;txn&gt;|TS=&lt;timestamp&gt;
 * Optional PRS suffix: |PNAME=&lt;passengerName&gt;
 * </pre>
 */
public final class PacketBuilder {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private PacketBuilder() {
    }

    /**
     * Composes the wire-format packet from validated ticket data and an authoritative timestamp.
     *
     * @param data      non-null ticket fields from the UI
     * @param timestamp typically {@link Instant#now()} captured at send time
     * @return complete packet string (without trailing newline; serial layer adds it)
     */
    public static String build(TicketData data, Instant timestamp) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(timestamp, "timestamp");

        String type = data.getTicketType().name();
        String ts = TS_FORMAT.format(timestamp);

        StringBuilder sb = new StringBuilder(256);
        sb.append("TYPE=").append(type);
        sb.append("|SRC=").append(data.getSourceStation());
        sb.append("|DST=").append(data.getDestinationStation());
        sb.append("|FARE=").append(data.getFare());
        sb.append("|TXN=").append(data.getTransactionId());
        sb.append("|TS=").append(ts);

        if (data.getTicketType() == TicketType.PRS) {
            Objects.requireNonNull(data.getPassengerName(), "passengerName for PRS");
            sb.append("|PNAME=").append(data.getPassengerName());
        }

        return sb.toString();
    }
}
