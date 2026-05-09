package com.railway.qfrds;

import javafx.application.Platform;

import java.util.Objects;

/**
 * Central coordinator for the QFRDS demo: bridges RS232 lines → parse → QR synthesis →
 * passenger-facing display and engineering status screen.
 * <p>
 * All UI mutations run on the JavaFX application thread via {@link Platform#runLater}.
 * Heavy parsing and ZXing encode stay on the caller thread (serial listener) before a single
 * batched UI runnable is queued.
 * </p>
 */
public final class DisplayController {

    private final ControllerStatusView statusView;
    private final PassengerDisplayView passengerView;
    private final TicketPacketParser parser = new TicketPacketParser();
    private final QRGeneratorService qrGenerator = new QRGeneratorService();
    private SerialListenerService serial;

    public DisplayController(ControllerStatusView statusView, PassengerDisplayView passengerView) {
        this.statusView = Objects.requireNonNull(statusView, "statusView");
        this.passengerView = Objects.requireNonNull(passengerView, "passengerView");
    }

    /**
     * Wires {@link SerialListenerService} and starts listening immediately (auto-start requirement).
     */
    public void start() {
        this.serial = new SerialListenerService(
                this::handleRawLine,
                msg -> Platform.runLater(() -> statusView.appendLog(msg)),
                () -> Platform.runLater(statusView::pulseSerialActivity)
        );
        serial.start();
        Platform.runLater(() -> {
            statusView.setMockMode(serial.isMockMode());
            statusView.setReconnectCount(serial.getReconnectAttempts());
        });
        // Periodically sync mock/reconnect state from listener (listener updates off FX thread)
        javafx.animation.Timeline sync = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> Platform.runLater(() -> {
                    statusView.setMockMode(serial.isMockMode());
                    statusView.setReconnectCount(serial.getReconnectAttempts());
                }))
        );
        sync.setCycleCount(javafx.animation.Animation.INDEFINITE);
        sync.play();
    }

    public void shutdown() {
        if (serial != null) {
            serial.stop();
        }
    }

    /**
     * Invoked from the serial daemon thread for each newline-delimited UTF-8 packet.
     */
    private void handleRawLine(String line) {
        TicketPacketParser.ParseResult result = parser.parse(line);

        String qrPayloadForStatus = "";
        TicketData ticket = null;
        QRGeneratorService.OptionalImageResult qrImage = QRGeneratorService.OptionalImageResult.fail("skipped");

        if (result.getData().isPresent()) {
            ticket = result.getData().get();
            qrPayloadForStatus = qrGenerator.buildQrPayload(ticket);
            qrImage = qrGenerator.renderQrImage(qrPayloadForStatus);
        }

        TicketPacketParser.ParseResult resultFinal = result;
        TicketData ticketFinal = ticket;
        QRGeneratorService.OptionalImageResult qrImageFinal = qrImage;
        String qrPayloadFinal = qrPayloadForStatus;

        Platform.runLater(() -> {
            statusView.setLastPacketPreview(truncate(line, 512));
            statusView.setReconnectCount(serial != null ? serial.getReconnectAttempts() : 0);
            statusView.setMockMode(serial != null && serial.isMockMode());

            if (resultFinal.getErrorMessage().isPresent()) {
                statusView.appendLog(LogFormatter.ts("PARSE ERROR: " + resultFinal.getErrorMessage().get()));
                statusView.setQrGenerationStatus("FAILED");
                statusView.setDetectedTicketType("—");
                statusView.pulseErrorLed();
                return;
            }

            TicketData t = Objects.requireNonNull(ticketFinal);
            statusView.setDetectedTicketType(t.getTicketType().name());
            statusView.appendLog(LogFormatter.ts("Packet parsed OK — TYPE=" + t.getTicketType()));

            if (!qrImageFinal.isSuccess()) {
                statusView.setQrGenerationStatus("FAILED: " + qrImageFinal.getError());
                statusView.appendLog(LogFormatter.ts("QR encode error: " + qrImageFinal.getError()));
                passengerView.applyTicketUpdate(t, null);
                statusView.pulseQrWarningLed();
                return;
            }

            statusView.setQrGenerationStatus("OK");
            statusView.appendLog(LogFormatter.ts("QR payload: " + truncate(qrPayloadFinal, 300)));
            passengerView.applyTicketUpdate(t, qrImageFinal.getImage());
            statusView.pulseQrOkLed();
            statusView.pulseDisplayPipelineLed();
        });
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
