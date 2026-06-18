package com.railway.qfrds;

import java.util.ArrayList;
import java.util.List;

/** Assembles newline-delimited UTF-8 lines from arbitrary serial byte chunks. */
final class SerialLineFramer {

    private final StringBuilder pending = new StringBuilder(256);

    synchronized List<String> takeLinesFromChunk(byte[] data, int length) {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            char c = (char) (data[i] & 0xFF);
            if (c == '\n') {
                String line = pending.toString().trim();
                pending.setLength(0);
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            } else if (c != '\r') {
                pending.append(c);
            }
        }
        return lines;
    }

    synchronized void reset() {
        pending.setLength(0);
    }
}
