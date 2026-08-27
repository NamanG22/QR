package com.railway.qfrds;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parses production command frames {@code $<code><Length><Data>^}.
 * <p>
 * UTS commands: 00 select, 01–09 fields, 12 txn type, 13 clear, 14 refund, 15 operator,
 * 17–20 reserved stations, 21 payment GW, 22 QR payload.
 * Unknown codes still parse as legacy {@code KEY=value} until specified.
 * </p>
 */
public final class TicketPacketParser {

    private static final Set<String> KNOWN_KEYS = Set.of(
            "TYPE", "SRC", "DST", "FARE", "TXN", "TS", "PNAME"
    );

    public enum Kind {
        UTS_SELECT,
        SOURCE_STATION,
        DEST_STATION,
        DATE,
        MONTH,
        ADULT,
        CHILD,
        TRAIN_TYPE,
        FARE,
        CLASS,
        TXN_TYPE,
        CLEAR,
        REFUND,
        OPERATOR,
        SOURCE_STATION_2,
        SOURCE_STATION_3,
        DEST_STATION_2,
        DEST_STATION_3,
        PAYMENT_GW,
        QR_PAYLOAD,
        TICKET
    }

    /**
     * Result of a parse attempt — a typed command, a full ticket, or a failure reason.
     */
    public static final class ParseResult {
        private final Kind kind;
        private final Optional<TicketData> data;
        private final Optional<StationField> station;
        private final Optional<String> twoDigit;
        private final Optional<String> errorMessage;

        private ParseResult(
                Kind kind,
                Optional<TicketData> data,
                Optional<StationField> station,
                Optional<String> twoDigit,
                Optional<String> errorMessage
        ) {
            this.kind = kind;
            this.data = data;
            this.station = station;
            this.twoDigit = twoDigit;
            this.errorMessage = errorMessage;
        }

        public static ParseResult utsSelect() {
            return new ParseResult(Kind.UTS_SELECT, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public static ParseResult sourceStation(StationField station) {
            return new ParseResult(Kind.SOURCE_STATION, Optional.empty(), Optional.of(station), Optional.empty(), Optional.empty());
        }

        public static ParseResult destStation(StationField station) {
            return new ParseResult(Kind.DEST_STATION, Optional.empty(), Optional.of(station), Optional.empty(), Optional.empty());
        }

        public static ParseResult date(String day) {
            return new ParseResult(Kind.DATE, Optional.empty(), Optional.empty(), Optional.of(day), Optional.empty());
        }

        public static ParseResult month(String month) {
            return new ParseResult(Kind.MONTH, Optional.empty(), Optional.empty(), Optional.of(month), Optional.empty());
        }

        public static ParseResult adult(String adult) {
            return new ParseResult(Kind.ADULT, Optional.empty(), Optional.empty(), Optional.of(adult), Optional.empty());
        }

        public static ParseResult child(String child) {
            return new ParseResult(Kind.CHILD, Optional.empty(), Optional.empty(), Optional.of(child), Optional.empty());
        }

        public static ParseResult trainType(String trainType) {
            return new ParseResult(Kind.TRAIN_TYPE, Optional.empty(), Optional.empty(), Optional.of(trainType), Optional.empty());
        }

        public static ParseResult fare(String fare) {
            return new ParseResult(Kind.FARE, Optional.empty(), Optional.empty(), Optional.of(fare), Optional.empty());
        }

        public static ParseResult travelClass(String travelClass) {
            return new ParseResult(Kind.CLASS, Optional.empty(), Optional.empty(), Optional.of(travelClass), Optional.empty());
        }

        public static ParseResult txnType(String txnType) {
            return new ParseResult(Kind.TXN_TYPE, Optional.empty(), Optional.empty(), Optional.of(txnType), Optional.empty());
        }

        public static ParseResult clearDisplay() {
            return new ParseResult(Kind.CLEAR, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public static ParseResult refund(String packed) {
            return new ParseResult(Kind.REFUND, Optional.empty(), Optional.empty(), Optional.of(packed), Optional.empty());
        }

        public static ParseResult operator(String packed) {
            return new ParseResult(Kind.OPERATOR, Optional.empty(), Optional.empty(), Optional.of(packed), Optional.empty());
        }

        public static ParseResult sourceStation2(StationField station) {
            return new ParseResult(Kind.SOURCE_STATION_2, Optional.empty(), Optional.of(station), Optional.empty(), Optional.empty());
        }

        public static ParseResult sourceStation3(StationField station) {
            return new ParseResult(Kind.SOURCE_STATION_3, Optional.empty(), Optional.of(station), Optional.empty(), Optional.empty());
        }

        public static ParseResult destStation2(StationField station) {
            return new ParseResult(Kind.DEST_STATION_2, Optional.empty(), Optional.of(station), Optional.empty(), Optional.empty());
        }

        public static ParseResult destStation3(StationField station) {
            return new ParseResult(Kind.DEST_STATION_3, Optional.empty(), Optional.of(station), Optional.empty(), Optional.empty());
        }

        public static ParseResult paymentGw(String gateway) {
            return new ParseResult(Kind.PAYMENT_GW, Optional.empty(), Optional.empty(), Optional.of(gateway), Optional.empty());
        }

        public static ParseResult qrPayload(String payload) {
            return new ParseResult(Kind.QR_PAYLOAD, Optional.empty(), Optional.empty(), Optional.of(payload), Optional.empty());
        }

        public static ParseResult ticket(TicketData data) {
            return new ParseResult(Kind.TICKET, Optional.of(data), Optional.empty(), Optional.empty(), Optional.empty());
        }

        public static ParseResult fail(String message) {
            return new ParseResult(null, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(message));
        }

        public Optional<Kind> getKind() {
            return Optional.ofNullable(kind);
        }

        public Optional<TicketData> getData() {
            return data;
        }

        public Optional<StationField> getStation() {
            return station;
        }

        public Optional<String> getTwoDigit() {
            return twoDigit;
        }

        public Optional<String> getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Parses one complete {@code $...^} frame. Malformed packets
     * return {@link ParseResult#fail(String)} without throwing.
     */
    public ParseResult parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) {
            return ParseResult.fail("empty line");
        }

        Optional<CommandFrame.Unwrapped> frame = CommandFrame.unwrap(rawLine);
        if (frame.isEmpty()) {
            return ParseResult.fail("invalid command frame (expected $CodeLengthData^)");
        }
        CommandFrame.Unwrapped unwrapped = frame.get();
        String code = unwrapped.code();
        String data = unwrapped.data();

        if (CommandSet.UTS_CODE.equals(code)) {
            if (!CommandSet.UTS_DATA.equalsIgnoreCase(data)) {
                return ParseResult.fail("code 00 (Thin Client UTS) expected data thUts");
            }
            return ParseResult.utsSelect();
        }
        if (CommandSet.SOURCE_STATION_CODE.equals(code)) {
            return ParseResult.sourceStation(StationField.unpack(data));
        }
        if (CommandSet.DEST_STATION_CODE.equals(code)) {
            return ParseResult.destStation(StationField.unpack(data));
        }
        if (CommandSet.DATE_CODE.equals(code)) {
            return TwoDigitField.unpack(data, CommandSet.DATE_MIN, CommandSet.DATE_MAX)
                    .map(ParseResult::date)
                    .orElseGet(() -> ParseResult.fail("code 03 (Date) expected 2-digit day 01–31"));
        }
        if (CommandSet.MONTH_CODE.equals(code)) {
            return TwoDigitField.unpack(data, CommandSet.MONTH_MIN, CommandSet.MONTH_MAX)
                    .map(ParseResult::month)
                    .orElseGet(() -> ParseResult.fail("code 04 (Month) expected 2-digit month 01–12"));
        }
        if (CommandSet.ADULT_CODE.equals(code)) {
            return TwoDigitField.unpack(data, CommandSet.COUNT_MIN, CommandSet.COUNT_MAX)
                    .map(ParseResult::adult)
                    .orElseGet(() -> ParseResult.fail("code 05 (Adult) expected 2-digit count 00–99"));
        }
        if (CommandSet.CHILD_CODE.equals(code)) {
            return TwoDigitField.unpack(data, CommandSet.COUNT_MIN, CommandSet.COUNT_MAX)
                    .map(ParseResult::child)
                    .orElseGet(() -> ParseResult.fail("code 06 (Child) expected 2-digit count 00–99"));
        }
        if (CommandSet.TRAIN_TYPE_CODE.equals(code)) {
            return TrainTypeField.unpack(data)
                    .map(ParseResult::trainType)
                    .orElseGet(() -> ParseResult.fail("code 07 (Type of Train) expected O/E/S/T/C/R/D/M/H/J/P"));
        }
        if (CommandSet.FARE_CODE.equals(code)) {
            return FareField.unpack(data)
                    .map(ParseResult::fare)
                    .orElseGet(() -> ParseResult.fail("code 08 (Fare) expected 1–5 digit numeric value"));
        }
        if (CommandSet.CLASS_CODE.equals(code)) {
            return ClassField.unpack(data)
                    .map(ParseResult::travelClass)
                    .orElseGet(() -> ParseResult.fail("code 09 (Class) expected I or II"));
        }
        if (CommandSet.TXN_TYPE_CODE.equals(code)) {
            return TxnTypeField.unpack(data)
                    .map(ParseResult::txnType)
                    .orElseGet(() -> ParseResult.fail("code 12 (Transaction Type) expected a transaction code"));
        }
        if (CommandSet.CLEAR_CODE.equals(code)) {
            if (!data.isEmpty()) {
                return ParseResult.fail("code 13 (Clear) expected empty data");
            }
            return ParseResult.clearDisplay();
        }
        if (CommandSet.REFUND_CODE.equals(code)) {
            return CancellationRefund.unpack(data)
                    .map(r -> ParseResult.refund(data))
                    .orElseGet(() -> ParseResult.fail("code 14 (Cancellation Refund) expected CANC+RFND+5-digit amount"));
        }
        if (CommandSet.OPERATOR_CODE.equals(code)) {
            return OperatorSession.unpack(data)
                    .map(s -> ParseResult.operator(data))
                    .orElseGet(() -> ParseResult.fail("code 15 (Operator) expected name:terminal:window:shift"));
        }
        if (CommandSet.SOURCE_STATION_2_CODE.equals(code)) {
            return ParseResult.sourceStation2(StationField.unpack(data));
        }
        if (CommandSet.SOURCE_STATION_3_CODE.equals(code)) {
            return ParseResult.sourceStation3(StationField.unpack(data));
        }
        if (CommandSet.DEST_STATION_2_CODE.equals(code)) {
            return ParseResult.destStation2(StationField.unpack(data));
        }
        if (CommandSet.DEST_STATION_3_CODE.equals(code)) {
            return ParseResult.destStation3(StationField.unpack(data));
        }
        if (CommandSet.PAYMENT_GW_CODE.equals(code)) {
            if (data.isBlank()) {
                return ParseResult.fail("code 21 (Payment gateway) expected non-empty data");
            }
            return ParseResult.paymentGw(data);
        }
        if (CommandSet.QR_PAYLOAD_CODE.equals(code)) {
            if (data.isBlank()) {
                return ParseResult.fail("code 22 (QR Code) expected non-empty QR payload");
            }
            return ParseResult.qrPayload(data);
        }

        return parseLegacyTicket(unwrapped);
    }

    private ParseResult parseLegacyTicket(CommandFrame.Unwrapped unwrapped) {
        Map<String, String> fields = new LinkedHashMap<>();
        String[] segments = unwrapped.data().split("\\|");
        for (String segment : segments) {
            int eq = segment.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = segment.substring(0, eq).trim();
            String value = segment.substring(eq + 1).trim();
            if (key.isEmpty()) {
                continue;
            }
            String canonical = key.toUpperCase(Locale.ROOT);
            if (KNOWN_KEYS.contains(canonical)) {
                fields.put(canonical, value);
            }
        }

        String typeStr = fields.get("TYPE");
        if (typeStr == null || typeStr.isBlank()) {
            typeStr = unwrapped.code();
        }
        if (typeStr == null || typeStr.isBlank()) {
            return ParseResult.fail("missing TYPE field");
        }

        TicketType type;
        try {
            type = TicketType.valueOf(typeStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ParseResult.fail("unknown command code: " + typeStr);
        }

        String src = fields.get("SRC");
        String dst = fields.get("DST");
        String fare = fields.get("FARE");
        String txn = fields.get("TXN");
        String ts = fields.get("TS");

        if (isBlank(src) || isBlank(dst) || isBlank(fare) || isBlank(txn) || isBlank(ts)) {
            return ParseResult.fail("missing required field (SRC, DST, FARE, TXN, TS)");
        }

        Optional<String> pname = Optional.ofNullable(fields.get("PNAME")).map(String::trim).filter(s -> !s.isEmpty());
        if (type == TicketType.PRS && pname.isEmpty()) {
            return ParseResult.fail("PRS ticket missing PNAME");
        }
        if (type == TicketType.UTS) {
            pname = Optional.empty();
        }

        return ParseResult.ticket(new TicketData(type, src, dst, fare, txn, ts, pname));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
