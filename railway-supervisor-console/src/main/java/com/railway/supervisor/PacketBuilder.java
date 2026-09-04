package com.railway.supervisor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds production RS232 command frames {@code $<code><Length><Data>^}.
 * <p>
 * UTS issue sequence: 13 clear, 00 select, 15 operator, 01–08, 09 class, 12 txn type,
 * 21 payment GW, 22 QR payload (22 omitted when empty).
 * Codes 17–20 are reserved and are not sent. Code 14 is a separate refund command.
 * PRS uses a separate SOH/STX/ETX envelope ({@link PrsFrame}).
 * </p>
 */
public final class PacketBuilder {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private PacketBuilder() {
    }

    public static String clearDisplay() {
        return CommandFrame.wrap(CommandSet.CLEAR_CODE, "");
    }

    public static List<String> cancellationRefund(CancellationRefund refund, String txnType) {
        Objects.requireNonNull(refund, "refund");
        List<String> frames = new ArrayList<>();
        if (txnType != null && !txnType.isBlank()) {
            frames.add(CommandFrame.wrap(CommandSet.TXN_TYPE_CODE, txnType.trim()));
        }
        frames.add(CommandFrame.wrap(CommandSet.REFUND_CODE, refund.pack()));
        return frames;
    }

    /**
     * One or more complete command frames (including SOT and EOT) for the ticket.
     */
    public static List<String> buildFrames(TicketData data, Instant timestamp) {
        Objects.requireNonNull(data, "data");
        Objects.requireNonNull(timestamp, "timestamp");

        if (data.getTicketType() == TicketType.UTS) {
            List<String> frames = new ArrayList<>();
            frames.add(CommandFrame.wrap(CommandSet.CLEAR_CODE, ""));
            frames.add(CommandFrame.wrap(CommandSet.UTS_CODE, CommandSet.UTS_DATA));
            frames.add(CommandFrame.wrap(CommandSet.OPERATOR_CODE, data.getOperator().pack()));
            frames.add(CommandFrame.wrap(CommandSet.SOURCE_STATION_CODE, data.getSource().pack()));
            frames.add(CommandFrame.wrap(CommandSet.DEST_STATION_CODE, data.getDestination().pack()));
            frames.add(CommandFrame.wrap(CommandSet.DATE_CODE, data.getDay()));
            frames.add(CommandFrame.wrap(CommandSet.MONTH_CODE, data.getMonth()));
            frames.add(CommandFrame.wrap(CommandSet.ADULT_CODE, data.getAdult()));
            frames.add(CommandFrame.wrap(CommandSet.CHILD_CODE, data.getChild()));
            frames.add(CommandFrame.wrap(CommandSet.TRAIN_TYPE_CODE, data.getTrainType()));
            frames.add(CommandFrame.wrap(CommandSet.FARE_CODE, data.getFare()));
            frames.add(CommandFrame.wrap(CommandSet.CLASS_CODE, data.getTravelClass()));
            frames.add(CommandFrame.wrap(CommandSet.TXN_TYPE_CODE, data.getTxnType()));
            if (data.getPaymentGw() != null && !data.getPaymentGw().isBlank()) {
                frames.add(CommandFrame.wrap(CommandSet.PAYMENT_GW_CODE, data.getPaymentGw()));
            }
            if (data.getQrPayload() != null && !data.getQrPayload().isBlank()) {
                String qr = UpiQueryAmount.overlayFare(data.getQrPayload(), data.getFare());
                frames.add(CommandFrame.wrap(CommandSet.QR_PAYLOAD_CODE, qr));
            }
            return frames;
        }

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

        return List.of(CommandFrame.wrap(type, sb.toString()));
    }
}
