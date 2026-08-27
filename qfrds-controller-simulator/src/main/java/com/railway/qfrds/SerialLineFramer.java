package com.railway.qfrds;

import java.util.ArrayList;
import java.util.List;

/**
 * Assembles {@code $...^} command frames from arbitrary serial byte chunks.
 * Bytes outside a frame (including leftover CR/LF) are discarded.
 */
final class SerialLineFramer {

    private static final int MAX_FRAME_CHARS = 4096;

    private final StringBuilder pending = new StringBuilder(256);
    private boolean inFrame;

    synchronized List<String> takeLinesFromChunk(byte[] data, int length) {
        List<String> frames = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            char c = (char) (data[i] & 0xFF);
            if (!inFrame) {
                if (c == CommandFrame.SOT) {
                    inFrame = true;
                    pending.setLength(0);
                    pending.append(c);
                }
                continue;
            }
            pending.append(c);
            if (c == CommandFrame.EOT) {
                frames.add(pending.toString());
                pending.setLength(0);
                inFrame = false;
            } else if (pending.length() > MAX_FRAME_CHARS) {
                pending.setLength(0);
                inFrame = false;
            }
        }
        return frames;
    }

    synchronized void reset() {
        pending.setLength(0);
        inFrame = false;
    }
}
