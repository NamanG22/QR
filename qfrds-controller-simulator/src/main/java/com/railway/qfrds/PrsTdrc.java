package com.railway.qfrds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PRS TDRC + passenger block (sub-function 111). Inner fields are {@code $NN:value}.
 * Extra passengers use {@code $19:}–{@code $38:} (not a repeated {@code $15:}–{@code $18:}).
 */
public final class PrsTdrc {

    private static final Pattern FIELD = Pattern.compile("\\$(\\d{2}):");

    public static final int TRAIN_CHARS = 5;
    public static final int STATION_CHARS = 4;
    public static final int CLASS_CHARS = 2;
    public static final int QUOTA_CHARS = 2;
    public static final int PAX_COUNT_CHARS = 2;
    public static final int OP_CODE_CHARS = 6;
    public static final int OP_NAME_CHARS = 20;
    public static final int SPECIAL_CHARS = 25;
    public static final int FARE_CHARS = 6;
    public static final int NAME_CHARS = 16;
    public static final int SEX_CHARS = 1;
    public static final int AGE_CHARS = 2;
    public static final int STATUS_CHARS = 10;
    public static final int MAX_PASSENGERS = 6;

    private final String trainNo;
    private final String day;
    private final String month;
    private final String from;
    private final String travelClass;
    private final String quota;
    private final String destination;
    private final String paxCount;
    private final String reservationUpto;
    private final String boarding;
    private final String operatorCode;
    private final String operatorName;
    private final String specialMessage;
    private final String fare;
    private final List<PrsPassenger> passengers;
    private final String qrPayload;
    private final String qrMessage;
    private final String paymentText;

    public PrsTdrc(
            String trainNo,
            String day,
            String month,
            String from,
            String travelClass,
            String quota,
            String destination,
            String paxCount,
            String reservationUpto,
            String boarding,
            String operatorCode,
            String operatorName,
            String specialMessage,
            String fare,
            List<PrsPassenger> passengers,
            String qrPayload,
            String qrMessage,
            String paymentText
    ) {
        this.trainNo = trainNo == null ? "" : trainNo;
        this.day = day == null ? "" : day;
        this.month = month == null ? "" : month;
        this.from = from == null ? "" : from;
        this.travelClass = travelClass == null ? "" : travelClass;
        this.quota = quota == null ? "" : quota;
        this.destination = destination == null ? "" : destination;
        this.paxCount = paxCount == null ? "" : paxCount;
        this.reservationUpto = reservationUpto == null ? "" : reservationUpto;
        this.boarding = boarding == null ? "" : boarding;
        this.operatorCode = operatorCode == null ? "" : operatorCode;
        this.operatorName = operatorName == null ? "" : operatorName;
        this.specialMessage = specialMessage == null ? "" : specialMessage;
        this.fare = fare == null ? "" : fare;
        this.passengers = passengers == null
                ? List.of()
                : List.copyOf(passengers);
        this.qrPayload = qrPayload == null ? "" : qrPayload;
        this.qrMessage = qrMessage == null ? "" : qrMessage;
        this.paymentText = paymentText == null ? "" : paymentText;
    }

    public static PrsTdrc blank() {
        return new PrsTdrc("", "", "", "", "", "", "", "", "", "", "", "", "", "",
                List.of(), "", "", "");
    }

    public String packBody() {
        StringBuilder sb = new StringBuilder(256);
        append(sb, 1, clip(trainNo, TRAIN_CHARS));
        append(sb, 2, clip(day, 2));
        append(sb, 3, clip(month, 2));
        append(sb, 4, clip(from, STATION_CHARS));
        append(sb, 5, clip(travelClass, CLASS_CHARS));
        append(sb, 6, clip(quota, QUOTA_CHARS));
        append(sb, 7, clip(destination, STATION_CHARS));
        List<PrsPassenger> pax = passengers.size() > MAX_PASSENGERS
                ? passengers.subList(0, MAX_PASSENGERS)
                : passengers;
        String count = paxCount.isBlank()
                ? String.format("%02d", pax.size())
                : padDigits(paxCount, PAX_COUNT_CHARS);
        append(sb, 8, count);
        append(sb, 9, clip(reservationUpto, STATION_CHARS));
        append(sb, 10, clip(boarding, STATION_CHARS));
        append(sb, 11, clip(operatorCode, OP_CODE_CHARS));
        append(sb, 12, clip(operatorName, OP_NAME_CHARS));
        append(sb, 13, clip(specialMessage, SPECIAL_CHARS));
        append(sb, 14, padDigits(fare, FARE_CHARS));
        for (int i = 0; i < pax.size(); i++) {
            int base = 15 + (i * 4);
            PrsPassenger p = pax.get(i);
            append(sb, base, clip(p.name(), NAME_CHARS));
            append(sb, base + 1, clip(p.sex(), SEX_CHARS));
            append(sb, base + 2, clip(p.age(), AGE_CHARS));
            append(sb, base + 3, clip(p.status(), STATUS_CHARS));
        }
        return sb.toString();
    }

    public static Optional<PrsTdrc> unpackBody(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        NavigableMap<Integer, String> fields = parseFields(body);
        List<PrsPassenger> pax = new ArrayList<>();
        for (int i = 0; i < MAX_PASSENGERS; i++) {
            int base = 15 + (i * 4);
            String name = fields.getOrDefault(base, "").trim();
            if (name.isEmpty() && !fields.containsKey(base)) {
                continue;
            }
            pax.add(new PrsPassenger(
                    name,
                    fields.getOrDefault(base + 1, "").trim(),
                    fields.getOrDefault(base + 2, "").trim(),
                    fields.getOrDefault(base + 3, "").trim()
            ));
        }
        return Optional.of(new PrsTdrc(
                fields.getOrDefault(1, ""),
                fields.getOrDefault(2, ""),
                fields.getOrDefault(3, ""),
                fields.getOrDefault(4, ""),
                fields.getOrDefault(5, ""),
                fields.getOrDefault(6, ""),
                fields.getOrDefault(7, ""),
                fields.getOrDefault(8, ""),
                fields.getOrDefault(9, ""),
                fields.getOrDefault(10, ""),
                fields.getOrDefault(11, ""),
                fields.getOrDefault(12, ""),
                fields.getOrDefault(13, ""),
                stripLeadingZeros(fields.getOrDefault(14, "")),
                pax,
                "",
                "",
                ""
        ));
    }

    public PrsTdrc withQr(String payload, String message) {
        return new PrsTdrc(trainNo, day, month, from, travelClass, quota, destination, paxCount,
                reservationUpto, boarding, operatorCode, operatorName, specialMessage, fare,
                passengers, payload, message, paymentText);
    }

    public PrsTdrc withPaymentText(String text) {
        return new PrsTdrc(trainNo, day, month, from, travelClass, quota, destination, paxCount,
                reservationUpto, boarding, operatorCode, operatorName, specialMessage, fare,
                passengers, qrPayload, qrMessage, text);
    }

    public String getTrainNo() {
        return trainNo;
    }

    public String getDay() {
        return day;
    }

    public String getMonth() {
        return month;
    }

    public String getFrom() {
        return from.trim();
    }

    public String getTravelClass() {
        return travelClass.trim();
    }

    public String getQuota() {
        return quota.trim();
    }

    public String getDestination() {
        return destination.trim();
    }

    public String getPaxCount() {
        return paxCount.trim();
    }

    public String getReservationUpto() {
        return reservationUpto.trim();
    }

    public String getBoarding() {
        return boarding.trim();
    }

    public String getOperatorCode() {
        return operatorCode.trim();
    }

    public String getOperatorName() {
        return operatorName.trim();
    }

    public String getSpecialMessage() {
        return specialMessage.trim();
    }

    public String getFare() {
        return fare;
    }

    public List<PrsPassenger> getPassengers() {
        return Collections.unmodifiableList(passengers);
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public String getQrMessage() {
        return qrMessage;
    }

    public String getPaymentText() {
        return paymentText;
    }

    public String dateDisplay() {
        String d = day.isBlank() ? "--" : clip(day.trim(), 2);
        String m = month.isBlank() ? "--" : clip(month.trim(), 2);
        return d + "/" + m;
    }

    private static void append(StringBuilder sb, int tag, String value) {
        sb.append('$').append(String.format("%02d", tag)).append(':').append(value == null ? "" : value);
    }

    static NavigableMap<Integer, String> parseFields(String body) {
        NavigableMap<Integer, String> map = new TreeMap<>();
        Matcher matcher = FIELD.matcher(body);
        List<int[]> spans = new ArrayList<>();
        while (matcher.find()) {
            spans.add(new int[]{matcher.start(), matcher.end(), Integer.parseInt(matcher.group(1))});
        }
        for (int i = 0; i < spans.size(); i++) {
            int valueStart = spans.get(i)[1];
            int valueEnd = i + 1 < spans.size() ? spans.get(i + 1)[0] : body.length();
            String value = body.substring(valueStart, valueEnd).replace("(", "").trim();
            map.put(spans.get(i)[2], value);
        }
        return map;
    }

    static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    static String padDigits(String raw, int width) {
        String digits = raw == null ? "" : raw.trim().replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return "0".repeat(width);
        }
        try {
            int n = Integer.parseInt(digits);
            String formatted = String.format("%0" + width + "d", n);
            return formatted.length() <= width ? formatted : formatted.substring(formatted.length() - width);
        } catch (NumberFormatException ex) {
            return clip(digits, width);
        }
    }

    static String stripLeadingZeros(String fare) {
        if (fare == null || fare.isBlank()) {
            return "";
        }
        String trimmed = fare.trim();
        try {
            return String.valueOf(Integer.parseInt(trimmed));
        } catch (NumberFormatException ex) {
            return trimmed;
        }
    }

    public record PrsPassenger(String name, String sex, String age, String status) {
        public PrsPassenger {
            name = name == null ? "" : name;
            sex = sex == null ? "" : sex;
            age = age == null ? "" : age;
            status = status == null ? "" : status;
        }
    }
}
