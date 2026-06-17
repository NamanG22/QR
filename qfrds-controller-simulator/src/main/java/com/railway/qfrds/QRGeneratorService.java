package com.railway.qfrds;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Encodes ticket data as a scannable QR matrix (ZXing) for the passenger display.
 * <p>
 * When {@link UpiQrConfig} is set ({@code QFRDS_UPI_VPA}), the QR is a real UPI payment link
 * ({@code upi://pay?...}) with amount from the ticket fare. Otherwise falls back to the demo
 * pipe-delimited text payload.
 * </p>
 */
public final class QRGeneratorService {

    private static final int QR_WIDTH_PX = 280;

    private final QRCodeWriter writer = new QRCodeWriter();

    /**
     * Builds the QR text payload: UPI payment URI when configured, else demo verification text.
     */
    public String buildQrPayload(TicketData ticket) {
        Objects.requireNonNull(ticket, "ticket");
        if (UpiQrConfig.isConfigured()) {
            return UpiQrConfig.buildPaymentUri(ticket);
        }
        return buildDemoPayload(ticket);
    }

    private static String buildDemoPayload(TicketData ticket) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("TXN=").append(ticket.getTransactionId());
        sb.append("|FARE=").append(ticket.getFare());
        sb.append("|SRC=").append(ticket.getSourceStation());
        sb.append("|DST=").append(ticket.getDestinationStation());
        sb.append("|TS=").append(ticket.getTimestampRaw());
        if (ticket.getTicketType() == TicketType.PRS) {
            ticket.getPassengerName().ifPresent(n -> sb.append("|PNAME=").append(n));
        }
        return sb.toString();
    }

    /**
     * Renders a square QR bitmap for on-screen display. Encoding failures are reported via
     * {@link OptionalImageResult#getError()} so the UI can log without crashing the listener thread.
     */
    public OptionalImageResult renderQrImage(String payload) {
        Objects.requireNonNull(payload, "payload");
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, QR_WIDTH_PX, QR_WIDTH_PX, hints);
            WritableImage fx = bitMatrixToWritableImage(matrix);
            return OptionalImageResult.ok(fx);
        } catch (WriterException ex) {
            return OptionalImageResult.fail(ex.getMessage());
        }
    }

    /**
     * Converts ZXing {@link BitMatrix} to JavaFX image without Swing — keeps dependency footprint small.
     */
    private static WritableImage bitMatrixToWritableImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pw.setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return image;
    }

    public static final class OptionalImageResult {
        private final WritableImage image;
        private final String error;

        private OptionalImageResult(WritableImage image, String error) {
            this.image = image;
            this.error = error;
        }

        public static OptionalImageResult ok(WritableImage image) {
            return new OptionalImageResult(image, null);
        }

        public static OptionalImageResult fail(String error) {
            return new OptionalImageResult(null, error);
        }

        public boolean isSuccess() {
            return image != null;
        }

        public WritableImage getImage() {
            return image;
        }

        public String getError() {
            return error;
        }
    }
}
