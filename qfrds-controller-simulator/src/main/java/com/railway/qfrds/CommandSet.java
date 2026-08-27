package com.railway.qfrds;

/**
 * Production command-set identifiers for the thin-client link.
 * <p>
 * Each UTS command is {@code $<2-digit code><Length><Data>^}.
 * Length is the UTF-8 byte length of {@code Data^} (Data ends with {@code :}).
 * </p>
 */
public final class CommandSet {

    /** Code 00 — Thin Client UTS. Data is {@code thUts} (on the wire {@code thUts:}). */
    public static final String UTS_CODE = "00";
    public static final String UTS_DATA = "thUts";

    /** Code 01 — Source Station. */
    public static final String SOURCE_STATION_CODE = "01";
    /** Code 02 — Destination Station. */
    public static final String DEST_STATION_CODE = "02";

    public static final int STATION_CODE_CHARS = 4;
    public static final int STATION_ENG_CHARS = 16;
    public static final int STATION_HINDI_CHARS = 16;

    /** Code 03 — Date (day of month). Data length 2, e.g. {@code 01}. */
    public static final String DATE_CODE = "03";
    /** Code 04 — Month. Data length 2, e.g. {@code 12}. */
    public static final String MONTH_CODE = "04";
    public static final int DATE_MIN = 1;
    public static final int DATE_MAX = 31;
    public static final int MONTH_MIN = 1;
    public static final int MONTH_MAX = 12;

    /** Code 05 — Adult count. Data length 2. */
    public static final String ADULT_CODE = "05";
    /** Code 06 — Child count. Data length 2. */
    public static final String CHILD_CODE = "06";
    public static final int COUNT_MIN = 0;
    public static final int COUNT_MAX = 99;

    /** Code 07 — Type of Train. Data is one letter plus trailing colon, e.g. {@code E:}. */
    public static final String TRAIN_TYPE_CODE = "07";
    /** Wire letters: O E S T C R D M H J P. */
    public static final String TRAIN_TYPE_VALUES = "OESTCRDMHJP";

    /** Code 08 — Fare. Length varies, max 5 digits. */
    public static final String FARE_CODE = "08";
    public static final int FARE_MAX_DIGITS = 5;

    /** Code 09 — Class. Data length 2: {@code I} or {@code II}. */
    public static final String CLASS_CODE = "09";
    public static final int CLASS_CHARS = 2;

    /** Code 12 — Transaction Type. Data is a TRANSACTION CODE, e.g. {@code PLAT}. */
    public static final String TXN_TYPE_CODE = "12";

    /** Code 13 — Clear display device. Wire example {@code $1303:^}. */
    public static final String CLEAR_CODE = "13";

    /** Code 14 — Cancellation Refund. Data: 4-char code + 4-char type + 5-digit amount. */
    public static final String REFUND_CODE = "14";
    public static final String REFUND_CODE_VALUE = "CANC";
    public static final String REFUND_TYPE_VALUE = "RFND";
    public static final int REFUND_AMOUNT_DIGITS = 5;

    /** Code 15 — Operator details: {@code name:terminal:window:shift}. */
    public static final String OPERATOR_CODE = "15";
    public static final int OPERATOR_NAME_CHARS = 25;
    public static final int TERMINAL_CHARS = 6;
    public static final int WINDOW_DIGITS = 3;
    public static final int SHIFT_DIGITS = 1;

    /** Codes 17–20 — reserved extra stations (same layout as 01/02). Not sent by UTS today. */
    public static final String SOURCE_STATION_2_CODE = "17";
    public static final String SOURCE_STATION_3_CODE = "18";
    public static final String DEST_STATION_2_CODE = "19";
    public static final String DEST_STATION_3_CODE = "20";

    /** Code 21 — Payment gateway label. Length varies. */
    public static final String PAYMENT_GW_CODE = "21";

    /** Code 22 — QR payload string. No trailing colon; length is three digits when ≥100. */
    public static final String QR_PAYLOAD_CODE = "22";

    private CommandSet() {
    }
}
