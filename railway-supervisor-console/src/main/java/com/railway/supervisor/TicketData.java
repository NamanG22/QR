package com.railway.supervisor;

import java.util.Objects;

/**
 * Immutable snapshot of supervisor form values used for ticket packet generation.
 * Keeps UI concerns out of {@link PacketBuilder}.
 */
public final class TicketData {

    private final TicketType ticketType;
    private final StationField source;
    private final StationField destination;
    private final String fare;
    private final String transactionId;
    private final String passengerName;
    private final String day;
    private final String month;
    private final String adult;
    private final String child;
    private final String trainType;
    private final String travelClass;
    private final String txnType;
    private final OperatorSession operator;
    private final String paymentGw;
    private final String qrPayload;

    public TicketData(
            TicketType ticketType,
            StationField source,
            StationField destination,
            String fare,
            String transactionId,
            String passengerName,
            String day,
            String month,
            String adult,
            String child,
            String trainType,
            String travelClass,
            String txnType,
            OperatorSession operator,
            String paymentGw,
            String qrPayload
    ) {
        this.ticketType = Objects.requireNonNull(ticketType, "ticketType");
        this.source = Objects.requireNonNull(source, "source");
        this.destination = Objects.requireNonNull(destination, "destination");
        this.fare = fare;
        this.transactionId = transactionId;
        this.passengerName = passengerName;
        this.day = day;
        this.month = month;
        this.adult = adult;
        this.child = child;
        this.trainType = trainType;
        this.travelClass = travelClass;
        this.txnType = txnType;
        this.operator = operator;
        this.paymentGw = paymentGw == null ? "" : paymentGw;
        this.qrPayload = qrPayload == null ? "" : qrPayload;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public StationField getSource() {
        return source;
    }

    public StationField getDestination() {
        return destination;
    }

    public String getSourceStation() {
        return source.getCode();
    }

    public String getDestinationStation() {
        return destination.getCode();
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

    public String getDay() {
        return day;
    }

    public String getMonth() {
        return month;
    }

    public String getAdult() {
        return adult;
    }

    public String getChild() {
        return child;
    }

    public String getTrainType() {
        return trainType;
    }

    public String getTravelClass() {
        return travelClass;
    }

    public String getTxnType() {
        return txnType;
    }

    public OperatorSession getOperator() {
        return operator;
    }

    public String getPaymentGw() {
        return paymentGw;
    }

    public String getQrPayload() {
        return qrPayload;
    }
}
