package com.railway.qfrds;

import javafx.application.Platform;

import java.util.Objects;

/**
 * Central coordinator for QFRDS: bridges RS232 lines → parse → QR synthesis →
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
    private MultiSerialListenerService serial;
    private int packetsReceived;

    public DisplayController(ControllerStatusView statusView, PassengerDisplayView passengerView) {
        this.statusView = Objects.requireNonNull(statusView, "statusView");
        this.passengerView = Objects.requireNonNull(passengerView, "passengerView");
    }

    /**
     * Starts the RS232 listener on the controller serial port (production RS232 or lab USB-serial pair).
     */
    public void start() {
        this.serial = new MultiSerialListenerService(
                this::handleRawLine,
                msg -> Platform.runLater(() -> statusView.appendLog(msg)),
                () -> Platform.runLater(statusView::pulseSerialActivity)
        );
        serial.start();
        Platform.runLater(() -> {
            statusView.setLinkLabel(serial.linkLabel());
            statusView.setMockMode(serial.isMockMode());
            statusView.setReconnectCount(serial.getReconnectAttempts());
            if (UpiQrConfig.isConfigured()) {
                statusView.appendLog(LogFormatter.ts(
                        "UPI QR enabled — VPA " + UpiQrConfig.vpa() + " (" + UpiQrConfig.payeeName() + ")"));
            } else {
                statusView.appendLog(LogFormatter.ts(
                        "UPI QR not set — using ticket-text QR"));
            }
            statusView.appendLog(LogFormatter.ts(
                    SerialPortConfig.useExplicitPort()
                            ? "RS232 listener on " + SerialPortConfig.explicitPortName()
                            : "RS232 listener on " + SerialPortConfig.DEFAULT_PORT_NAME));
            refreshPassengerLinkStatus();
        });
        javafx.animation.Timeline sync = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> Platform.runLater(() -> {
                    statusView.setMockMode(serial.isMockMode());
                    statusView.setReconnectCount(serial.getReconnectAttempts());
                    refreshPassengerLinkStatus();
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
     * Invoked from the jSerialComm listener thread — parse quickly, UI + QR on FX thread.
     */
    private void handleRawLine(String line) {
        packetsReceived++;
        TicketPacketParser.ParseResult result = parser.parse(line);
        Platform.runLater(() -> applyPacketToUi(line, result));
    }

    private void applyPacketToUi(String line, TicketPacketParser.ParseResult result) {
        statusView.setLastPacketPreview(truncate(line, 512));
        statusView.setReconnectCount(serial != null ? serial.getReconnectAttempts() : 0);
        statusView.setMockMode(serial != null && serial.isMockMode());
        refreshPassengerLinkStatus();

        if (result.getErrorMessage().isPresent()) {
            statusView.appendLog(LogFormatter.ts("PARSE ERROR: " + result.getErrorMessage().get()));
            statusView.setQrGenerationStatus("FAILED");
            statusView.setDetectedTicketType("—");
            statusView.pulseErrorLed();
            return;
        }

        TicketData t = result.getData().orElseThrow();
        statusView.setDetectedTicketType(t.getTicketType().name());
        statusView.appendLog(LogFormatter.ts("Packet parsed OK — TYPE=" + t.getTicketType()));

        String qrPayload = qrGenerator.buildQrPayload(t);
        QRGeneratorService.OptionalImageResult qrImage = qrGenerator.renderQrImage(qrPayload);

        if (!qrImage.isSuccess()) {
            statusView.setQrGenerationStatus("FAILED: " + qrImage.getError());
            statusView.appendLog(LogFormatter.ts("QR encode error: " + qrImage.getError()));
            passengerView.applyTicketUpdate(t, null);
            statusView.pulseQrWarningLed();
            return;
        }

        statusView.setQrGenerationStatus("OK");
        statusView.appendLog(LogFormatter.ts("QR payload: " + truncate(qrPayload, 300)));
        passengerView.applyTicketUpdate(t, qrImage.getImage());
        statusView.pulseQrOkLed();
        statusView.pulseDisplayPipelineLed();
    }

    private void refreshPassengerLinkStatus() {
        String port = serial != null ? serial.linkLabel() : "—";
        boolean live = serial != null && serial.isLinkLive();
        boolean reconnecting = serial != null && serial.isReconnecting();
        String hint = serial != null ? serial.statusHint() : "starting";
        passengerView.setLinkStatus(port, live, reconnecting, packetsReceived, hint);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
