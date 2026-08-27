package com.railway.supervisor;

import java.util.Objects;

/**
 * Colon-separated station payload for UTS commands 01/02 and reserved 17–20.
 * <p>
 * Data: {@code code:english:hindi} (trailing colon is added by the envelope).
 * Names are not space-padded; envelope Length is UTF-8 bytes of {@code Data^}.
 * </p>
 */
public final class StationField {

    private final String code;
    private final String english;
    private final String hindi;

    public StationField(String code, String english, String hindi) {
        this.code = code == null ? "" : code.trim();
        this.english = english == null ? "" : english.trim();
        this.hindi = hindi == null ? "" : hindi.trim();
    }

    public static StationField fromCode(String code) {
        return new StationField(code, "", "");
    }

    public String getCode() {
        return code;
    }

    public String getEnglish() {
        return english;
    }

    public String getHindi() {
        return hindi;
    }

    /** English name if present, otherwise station code. */
    public String displayName() {
        return english.isBlank() ? code : english;
    }

    public String pack() {
        return truncate(code, CommandSet.STATION_CODE_CHARS)
                + CommandFrame.SEP
                + truncate(english, CommandSet.STATION_ENG_CHARS)
                + CommandFrame.SEP
                + truncate(hindi, CommandSet.STATION_HINDI_CHARS);
    }

    public static StationField unpack(String data) {
        String raw = data == null ? "" : data;
        while (raw.endsWith(":")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        String[] parts = raw.split(":", -1);
        String stationCode = parts.length > 0 ? parts[0].trim() : "";
        String eng = parts.length > 1 ? parts[1].trim() : "";
        String hi = parts.length > 2 ? parts[2].trim() : "";
        return new StationField(stationCode, eng, hi);
    }

    public static String pad(String raw, int maxChars) {
        int[] cps = (raw == null ? "" : raw).codePoints().toArray();
        int keep = Math.min(cps.length, maxChars);
        StringBuilder sb = new StringBuilder();
        sb.append(new String(cps, 0, keep));
        for (int i = keep; i < maxChars; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String truncate(String raw, int maxChars) {
        int[] cps = (raw == null ? "" : raw).codePoints().toArray();
        int keep = Math.min(cps.length, maxChars);
        return new String(cps, 0, keep);
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StationField other)) {
            return false;
        }
        return code.equals(other.code) && english.equals(other.english) && hindi.equals(other.hindi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, english, hindi);
    }
}
