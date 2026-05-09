package com.railway.supervisor;

import java.util.Objects;

/**
 * Immutable snapshot of supervisor form values used for ticket packet generation.
 * Keeps UI concerns out of {@link PacketBuilder}.
 */
public final class TicketData {

    private final TicketType ticketType;
    private final String sourceStation;
    private final String destinationStation;
    private final String fare;
    private final String transactionId;
    /** Required when ticket type is PRS; may be null or blank for UTS. */
    private final String passengerName;

    public TicketData(
            TicketType ticketType,
            String sourceStation,
            String destinationStation,
            String fare,
            String transactionId,
            String passengerName
    ) {
        this.ticketType = Objects.requireNonNull(ticketType, "ticketType");
        this.sourceStation = sourceStation;
        this.destinationStation = destinationStation;
        this.fare = fare;
        this.transactionId = transactionId;
        this.passengerName = passengerName;
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

    public String getPassengerName() {
        return passengerName;
    }
}
