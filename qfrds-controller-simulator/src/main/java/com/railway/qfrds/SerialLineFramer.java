package com.railway.qfrds;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles UTS {@code $...^} and PRS {@code SOH...ETX} frames from serial byte chunks.
 * Bytes outside a frame (including leftover CR/LF) are discarded.
 */
final class SerialLineFramer {

    private enum Mode { NONE, UTS, PRS }

    private static final int MAX_FRAME_CHARS = 4096;

    private final StringBuilder pending = new StringBuilder(256);
    private Mode mode = Mode.NONE;

    synchronized List<String> takeLinesFromChunk(byte[] data, int length) {
        List<String> frames = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            char c = (char) (data[i] & 0xFF);
            if (mode == Mode.NONE) {
                if (c == PrsFrame.SOH) {
                    mode = Mode.PRS;
                    pending.setLength(0);
                    pending.append(c);
                } else if (c == CommandFrame.SOT) {
                    mode = Mode.UTS;
                    pending.setLength(0);
                    pending.append(c);
                }
                continue;
            }
            pending.append(c);
            boolean complete = (mode == Mode.UTS && c == CommandFrame.EOT)
                    || (mode == Mode.PRS && c == PrsFrame.ETX);
            if (complete) {
                frames.add(pending.toString());
                pending.setLength(0);
                mode = Mode.NONE;
            } else if (pending.length() > MAX_FRAME_CHARS) {
                pending.setLength(0);
                mode = Mode.NONE;
            }
        }
        return frames;
    }

    synchronized void reset() {
        pending.setLength(0);
        mode = Mode.NONE;
    }
}
