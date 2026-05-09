package com.railway.qfrds;

import java.util.Objects;
import java.util.Optional;

/**
 * Parsed fare ticket snapshot received over RS232 for display and QR synthesis.
 * Immutable value object shared between {@link TicketPacketParser}, {@link QRGeneratorService},
 * and {@link PassengerDisplayView}.
 */
public final class TicketData {

    private final TicketType ticketType;
    private final String sourceStation;
    private final String destinationStation;
    private final String fare;
    private final String transactionId;
    private final String timestampRaw;
    /** Present only for {@link TicketType#PRS}. */
    private final Optional<String> passengerName;

    public TicketData(
            TicketType ticketType,
            String sourceStation,
            String destinationStation,
            String fare,
            String transactionId,
            String timestampRaw,
            Optional<String> passengerName
    ) {
        this.ticketType = Objects.requireNonNull(ticketType);
        this.sourceStation = Objects.requireNonNull(sourceStation);
        this.destinationStation = Objects.requireNonNull(destinationStation);
        this.fare = Objects.requireNonNull(fare);
        this.transactionId = Objects.requireNonNull(transactionId);
        this.timestampRaw = Objects.requireNonNull(timestampRaw);
        this.passengerName = Objects.requireNonNull(passengerName);
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public String getSourceStation() {
        return sourceStation;
    }

    public String getDestinationStation() {
        return destinationStation;
    }

    public String getFare() {
        return fare;
    }

    public String getTransactionId() {
        return transactionId;
    }

    /**
     * Raw timestamp string from packet ({@code TS=}) — displayed and embedded in QR as-is.
     */
    public String getTimestampRaw() {
        return timestampRaw;
    }

    public Optional<String> getPassengerName() {
        return passengerName;
    }
}
