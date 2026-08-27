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
    private TicketData utsTicket = TicketData.blankUts();

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

        TicketPacketParser.Kind kind = result.getKind().orElse(null);
        if (kind == TicketPacketParser.Kind.UTS_SELECT) {
            utsTicket = TicketData.blankUts();
            statusView.setDetectedTicketType("UTS");
            statusView.setQrGenerationStatus("—");
            statusView.appendLog(LogFormatter.ts("Thin Client UTS selected (code 00)"));
            passengerView.applyTicketUpdate(utsTicket, null);
            statusView.pulseDisplayPipelineLed();
            return;
        }
        if (kind == TicketPacketParser.Kind.SOURCE_STATION) {
            StationField station = result.getStation().orElseThrow();
            utsTicket = utsTicket.withSource(station);
            applyUtsPartial("Source station (code 01): " + station.displayName());
            return;
        }
        if (kind == TicketPacketParser.Kind.DEST_STATION) {
            StationField station = result.getStation().orElseThrow();
            utsTicket = utsTicket.withDestination(station);
            applyUtsPartial("Destination station (code 02): " + station.displayName());
            return;
        }
        if (kind == TicketPacketParser.Kind.DATE) {
            String day = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withDay(day);
            applyUtsPartial("Date (code 03): " + day);
            return;
        }
        if (kind == TicketPacketParser.Kind.MONTH) {
            String month = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withMonth(month);
            applyUtsPartial("Month (code 04): " + month);
            return;
        }
        if (kind == TicketPacketParser.Kind.ADULT) {
            String adult = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withAdult(adult);
            applyUtsPartial("Adult (code 05): " + adult);
            return;
        }
        if (kind == TicketPacketParser.Kind.CHILD) {
            String child = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withChild(child);
            applyUtsPartial("Child (code 06): " + child);
            return;
        }
        if (kind == TicketPacketParser.Kind.TRAIN_TYPE) {
            String trainType = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withTrainType(trainType);
            applyUtsPartial("Type of Train (code 07): " + trainType);
            return;
        }
        if (kind == TicketPacketParser.Kind.FARE) {
            String fare = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withFare(fare);
            statusView.setDetectedTicketType("UTS");
            statusView.appendLog(LogFormatter.ts("Fare (code 08): " + fare));
            refreshUtsQrAndDisplay();
            return;
        }
        if (kind == TicketPacketParser.Kind.CLASS) {
            String travelClass = result.getTwoDigit().orElseThrow().trim();
            utsTicket = utsTicket.withTravelClass(travelClass);
            applyUtsPartial("Class (code 09): " + travelClass);
            return;
        }
        if (kind == TicketPacketParser.Kind.TXN_TYPE) {
            String txnType = result.getTwoDigit().orElseThrow().trim();
            utsTicket = utsTicket.withTxnType(txnType);
            applyUtsPartial("Transaction Type (code 12): " + TxnTypeField.display(txnType));
            return;
        }
        if (kind == TicketPacketParser.Kind.CLEAR) {
            utsTicket = TicketData.blankUts();
            statusView.setDetectedTicketType("UTS");
            statusView.setQrGenerationStatus("—");
            statusView.appendLog(LogFormatter.ts("Clear display (code 13)"));
            passengerView.clearDisplay();
            statusView.pulseDisplayPipelineLed();
            return;
        }
        if (kind == TicketPacketParser.Kind.REFUND) {
            CancellationRefund refund = CancellationRefund.unpack(result.getTwoDigit().orElseThrow()).orElseThrow();
            utsTicket = TicketData.blankUts()
                    .withTravelClass(refund.getCode())
                    .withTxnType(refund.getType())
                    .withFare(refund.getAmount());
            statusView.setDetectedTicketType("UTS");
            statusView.appendLog(LogFormatter.ts(
                    "Cancellation refund (code 14): " + refund.getCode() + " " + refund.getType()
                            + " " + refund.getAmount()));
            statusView.setQrGenerationStatus("—");
            passengerView.applyTicketUpdate(utsTicket, null);
            statusView.pulseDisplayPipelineLed();
            return;
        }
        if (kind == TicketPacketParser.Kind.OPERATOR) {
            OperatorSession session = OperatorSession.unpack(result.getTwoDigit().orElseThrow()).orElseThrow();
            utsTicket = utsTicket.withOperator(session);
            applyUtsPartial("Operator (code 15): " + session.getOperatorName()
                    + " " + session.getTerminalId() + " win " + session.getWindowNo()
                    + " shift " + session.getShiftNo());
            return;
        }
        if (kind == TicketPacketParser.Kind.SOURCE_STATION_2
                || kind == TicketPacketParser.Kind.SOURCE_STATION_3
                || kind == TicketPacketParser.Kind.DEST_STATION_2
                || kind == TicketPacketParser.Kind.DEST_STATION_3) {
            StationField station = result.getStation().orElseThrow();
            statusView.setDetectedTicketType("UTS");
            statusView.appendLog(LogFormatter.ts(
                    "Reserved station command " + reservedStationCode(kind) + " (not applied): "
                            + station.displayName()));
            return;
        }
        if (kind == TicketPacketParser.Kind.PAYMENT_GW) {
            String gateway = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withPaymentGw(gateway);
            applyUtsPartial("Payment gateway (code 21): " + gateway);
            return;
        }
        if (kind == TicketPacketParser.Kind.QR_PAYLOAD) {
            String payload = result.getTwoDigit().orElseThrow();
            utsTicket = utsTicket.withQrPayload(payload);
            statusView.setDetectedTicketType("UTS");
            statusView.appendLog(LogFormatter.ts("QR Code (code 22) received (" + payload.length() + " chars)"));
            refreshUtsQrAndDisplay();
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

    private void applyUtsPartial(String message) {
        statusView.setDetectedTicketType("UTS");
        statusView.appendLog(LogFormatter.ts(message));
        passengerView.applyTicketUpdate(utsTicket, null);
        statusView.pulseDisplayPipelineLed();
    }

    private void refreshUtsQrAndDisplay() {
        String qrPayload = utsTicket.getQrPayload().isBlank()
                ? qrGenerator.buildQrPayload(utsTicket)
                : utsTicket.getQrPayload();
        QRGeneratorService.OptionalImageResult qrImage = qrGenerator.renderQrImage(qrPayload);
        if (!qrImage.isSuccess()) {
            statusView.setQrGenerationStatus("FAILED: " + qrImage.getError());
            statusView.appendLog(LogFormatter.ts("QR encode error: " + qrImage.getError()));
            passengerView.applyTicketUpdate(utsTicket, null);
            statusView.pulseQrWarningLed();
            return;
        }
        statusView.setQrGenerationStatus("OK");
        statusView.appendLog(LogFormatter.ts("QR payload: " + truncate(qrPayload, 300)));
        passengerView.applyTicketUpdate(utsTicket, qrImage.getImage());
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

    private static String reservedStationCode(TicketPacketParser.Kind kind) {
        return switch (kind) {
            case SOURCE_STATION_2 -> "17";
            case SOURCE_STATION_3 -> "18";
            case DEST_STATION_2 -> "19";
            case DEST_STATION_3 -> "20";
            default -> kind.name();
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
