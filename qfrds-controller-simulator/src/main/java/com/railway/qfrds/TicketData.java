package com.railway.qfrds;

import java.util.Objects;
import java.util.Optional;

/**
 * Parsed fare ticket snapshot received over RS232 for display and QR synthesis.
 */
public final class TicketData {

    private final TicketType ticketType;
    private final Optional<StationField> source;
    private final Optional<StationField> destination;
    private final String fare;
    private final String transactionId;
    private final String timestampRaw;
    private final String day;
    private final String month;
    private final String adult;
    private final String child;
    private final String trainType;
    private final String travelClass;
    private final String txnType;
    private final Optional<OperatorSession> operator;
    private final String paymentGw;
    private final String qrPayload;
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
        this(
                ticketType,
                sourceStation.isBlank() ? Optional.empty() : Optional.of(new StationField(sourceStation, "", "")),
                destinationStation.isBlank() ? Optional.empty() : Optional.of(new StationField(destinationStation, "", "")),
                fare,
                transactionId,
                timestampRaw,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                Optional.empty(),
                "",
                "",
                passengerName
        );
    }

    public TicketData(
            TicketType ticketType,
            Optional<StationField> source,
            Optional<StationField> destination,
            String fare,
            String transactionId,
            String timestampRaw,
            String day,
            String month,
            String adult,
            String child,
            String trainType,
            String travelClass,
            String txnType,
            Optional<OperatorSession> operator,
            String paymentGw,
            String qrPayload,
            Optional<String> passengerName
    ) {
        this.ticketType = Objects.requireNonNull(ticketType);
        this.source = Objects.requireNonNull(source);
        this.destination = Objects.requireNonNull(destination);
        this.fare = Objects.requireNonNull(fare);
        this.transactionId = Objects.requireNonNull(transactionId);
        this.timestampRaw = Objects.requireNonNull(timestampRaw);
        this.day = day == null ? "" : day;
        this.month = month == null ? "" : month;
        this.adult = adult == null ? "" : adult;
        this.child = child == null ? "" : child;
        this.trainType = trainType == null ? "" : trainType;
        this.travelClass = travelClass == null ? "" : travelClass;
        this.txnType = txnType == null ? "" : txnType;
        this.operator = operator == null ? Optional.empty() : operator;
        this.paymentGw = paymentGw == null ? "" : paymentGw;
        this.qrPayload = qrPayload == null ? "" : qrPayload;
        this.passengerName = Objects.requireNonNull(passengerName);
    }

    public static TicketData blankUts() {
        return new TicketData(
                TicketType.UTS,
                Optional.empty(),
                Optional.empty(),
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                Optional.empty(),
                "",
                "",
                Optional.empty()
        );
    }

    private TicketData copy(
            Optional<StationField> newSource,
            Optional<StationField> newDest,
            String newFare,
            String newDay,
            String newMonth,
            String newAdult,
            String newChild,
            String newTrainType,
            String newTravelClass,
            String newTxnType,
            Optional<OperatorSession> newOperator,
            String newPaymentGw,
            String newQrPayload
    ) {
        return new TicketData(
                ticketType, newSource, newDest, newFare, transactionId, timestampRaw,
                newDay, newMonth, newAdult, newChild, newTrainType, newTravelClass, newTxnType,
                newOperator, newPaymentGw, newQrPayload, passengerName
        );
    }

    public TicketData withSource(StationField station) {
        return copy(Optional.of(station), destination, fare, day, month, adult, child, trainType, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withDestination(StationField station) {
        return copy(source, Optional.of(station), fare, day, month, adult, child, trainType, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withDay(String value) {
        return copy(source, destination, fare, value, month, adult, child, trainType, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withMonth(String value) {
        return copy(source, destination, fare, day, value, adult, child, trainType, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withAdult(String value) {
        return copy(source, destination, fare, day, month, value, child, trainType, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withChild(String value) {
        return copy(source, destination, fare, day, month, adult, value, trainType, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withTrainType(String value) {
        return copy(source, destination, fare, day, month, adult, child, value, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withFare(String value) {
        return copy(source, destination, value, day, month, adult, child, trainType, travelClass, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withTravelClass(String value) {
        return copy(source, destination, fare, day, month, adult, child, trainType, value, txnType,
                operator, paymentGw, qrPayload);
    }

    public TicketData withTxnType(String value) {
        return copy(source, destination, fare, day, month, adult, child, trainType, travelClass, value,
                operator, paymentGw, qrPayload);
    }

    public TicketData withOperator(OperatorSession session) {
        return copy(source, destination, fare, day, month, adult, child, trainType, travelClass, txnType,
                Optional.of(session), paymentGw, qrPayload);
    }

    public TicketData withPaymentGw(String value) {
        return copy(source, destination, fare, day, month, adult, child, trainType, travelClass, txnType,
                operator, value, qrPayload);
    }

    public TicketData withQrPayload(String value) {
        return copy(source, destination, fare, day, month, adult, child, trainType, travelClass, txnType,
                operator, paymentGw, value);
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public Optional<StationField> getSource() {
        return source;
    }

    public Optional<StationField> getDestination() {
        return destination;
    }

    public String getSourceStation() {
        return source.map(StationField::displayName).orElse("");
    }

    public String getDestinationStation() {
        return destination.map(StationField::displayName).orElse("");
    }

    public String getSourceBoardText() {
        return source.map(StationField::boardText).orElse("");
    }

    public String getDestinationBoardText() {
        return destination.map(StationField::boardText).orElse("");
    }

    public String getFare() {
        return fare;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTimestampRaw() {
        return timestampRaw;
    }

    public String getDateDisplay() {
        if (day.isBlank() && month.isBlank()) {
            return timestampRaw;
        }
        String d = day.isBlank() ? "--" : day;
        String m = month.isBlank() ? "--" : month;
        return d + "/" + m;
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

    public Optional<OperatorSession> getOperator() {
        return operator;
    }

    public String getPaymentGw() {
        return paymentGw;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public Optional<String> getPassengerName() {
        return passengerName;
    }
}
