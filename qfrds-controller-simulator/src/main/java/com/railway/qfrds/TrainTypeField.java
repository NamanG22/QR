package com.railway.qfrds;

import java.util.Optional;

/**
 * Type of train (command 07). Wire data is a single letter (trailing {@code :} added by envelope).
 */
public final class TrainTypeField {

    private TrainTypeField() {
    }

    public static Optional<String> pack(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        char letter = Character.toUpperCase(raw.trim().charAt(0));
        if (CommandSet.TRAIN_TYPE_VALUES.indexOf(letter) < 0) {
            return Optional.empty();
        }
        return Optional.of(String.valueOf(letter));
    }

    public static Optional<String> unpack(String data) {
        if (data == null) {
            return Optional.empty();
        }
        String trimmed = data.endsWith(":") ? data.substring(0, data.length() - 1) : data;
        return pack(trimmed);
    }

    /** Passenger-facing label for a wire letter. */
    public static String display(String code) {
        if (code == null || code.isBlank()) {
            return "—";
        }
        return switch (code.trim().toUpperCase()) {
            case "O" -> "ORD";
            case "E" -> "M/E";
            case "S" -> "SUP";
            case "T" -> "MMT";
            case "C" -> "COM";
            case "R" -> "RAJ";
            case "D" -> "SHT";
            case "M" -> "RMT";
            case "H" -> "DHI";
            case "J" -> "JAN";
            case "P" -> "PRM";
            default -> code.trim().toUpperCase();
        };
    }
}
