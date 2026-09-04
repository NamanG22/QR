package com.railway.supervisor;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Supervisor form: builds ticket packets and sends them to the controller over RS232.
 * <p>
 * Development: USB-to-serial adapter on the console PC (select its COM port in the UI).
 * Production: direct RS232 from the CRIS terminal to the controller RS232 input.
 * </p>
 */
public class SupervisorController {

    @FXML
    private ComboBox<TicketType> ticketTypeCombo;
    @FXML
    private TextField sourceField;
    @FXML
    private TextField sourceEngField;
    @FXML
    private TextField sourceHindiField;
    @FXML
    private TextField destinationField;
    @FXML
    private TextField destEngField;
    @FXML
    private TextField destHindiField;
    @FXML
    private Label dateLabel;
    @FXML
    private HBox dateBox;
    @FXML
    private TextField dayField;
    @FXML
    private TextField monthField;
    @FXML
    private Label adultChildLabel;
    @FXML
    private HBox adultChildBox;
    @FXML
    private TextField adultField;
    @FXML
    private TextField childField;
    @FXML
    private Label trainTypeLabel;
    @FXML
    private ComboBox<String> trainTypeCombo;
    @FXML
    private Label classTxnLabel;
    @FXML
    private HBox classTxnBox;
    @FXML
    private ComboBox<String> classCombo;
    @FXML
    private ComboBox<String> txnTypeCombo;
    @FXML
    private TextField fareField;
    @FXML
    private Label refundLabel;
    @FXML
    private HBox refundBox;
    @FXML
    private TextField refundAmountField;
    @FXML
    private Label operatorLabel;
    @FXML
    private TextField operatorField;
    @FXML
    private Label terminalLabel;
    @FXML
    private HBox terminalBox;
    @FXML
    private TextField terminalField;
    @FXML
    private TextField windowField;
    @FXML
    private TextField shiftField;
    @FXML
    private Label paymentLabel;
    @FXML
    private HBox paymentBox;
    @FXML
    private TextField paymentGwField;
    @FXML
    private TextField qrPayloadField;
    @FXML
    private Label transactionLabel;
    @FXML
    private TextField transactionField;
    @FXML
    private TextField passengerNameField;
    @FXML
    private Label passengerNameLabel;
    @FXML
    private VBox prsPanel;
    @FXML
    private TextField prsTrainField;
    @FXML
    private TextField prsClassField;
    @FXML
    private TextField prsQuotaField;
    @FXML
    private TextField prsBoardingField;
    @FXML
    private TextField prsResUptoField;
    @FXML
    private TextField prsOpCodeField;
    @FXML
    private TextField prsOpNameField;
    @FXML
    private TextField prsSpecialField;
    @FXML
    private TextField prsQrField;
    @FXML
    private TextField prsQrMessageField;
    @FXML
    private TextField prsPaymentTextField;
    @FXML
    private VBox prsPaxRowsBox;
    @FXML
    private Button refundButton;
    @FXML
    private Button prsPingButton;
    @FXML
    private Button prsPayOkButton;
    @FXML
    private Button prsPayFailButton;
    @FXML
    private TextField serialPortField;
    @FXML
    private TextArea logArea;

    private SerialService serialService;

    public void shutdown() {
        if (serialService != null) {
            serialService.disconnectQuietly();
        }
    }

    public void initialize() {
        ticketTypeCombo.getItems().setAll(TicketType.values());
        ticketTypeCombo.setValue(TicketType.UTS);

        String savedPort = firstNonBlank(
                System.getenv("QFRDS_SUPERVISOR_PORT"),
                System.getProperty("qfrds.supervisor.port"));
        if (savedPort != null) {
            serialPortField.setText(savedPort);
        } else {
            String port = SerialPortConfig.portName();
            serialPortField.setText(port);
            String detected = SerialPortConfig.detectUsbSerialPort();
            if (detected != null && detected.equalsIgnoreCase(port)) {
                appendLog("Auto-detected USB-serial port: " + port);
            }
        }

        serialService = buildSerialService();
        appendLog("Connecting to " + serialService.linkLabel() + " …");
        serialService.connect();
        appendLog("Click Generate Ticket when connected.");
        appendLog("Controller listens on RS232 — set QFRDS_CONTROLLER_PORT on the thin client if needed.");
        appendLog("See SERIAL_SETUP.md in the repo root for wiring and port help.");

        ticketTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePassengerVisibility(newVal));
        updatePassengerVisibility(ticketTypeCombo.getValue());

        LocalDate today = LocalDate.now();
        dayField.setText(String.format("%02d", today.getDayOfMonth()));
        monthField.setText(String.format("%02d", today.getMonthValue()));
        adultField.setText("01");
        childField.setText("00");
        trainTypeCombo.getItems().setAll(
                "O — ORD",
                "E — M/E",
                "S — SUP",
                "T — MMT",
                "C — COM",
                "R — RAJ",
                "D — SHT",
                "M — RMT",
                "H — DHI",
                "J — JAN",
                "P — PRM"
        );
        trainTypeCombo.setValue("E — M/E");
        classCombo.getItems().setAll("I", "II");
        classCombo.setValue("II");
        txnTypeCombo.getItems().setAll(TxnTypeField.comboLabels());
        txnTypeCombo.setValue("PLAT — PLATFORM");
        refundAmountField.setText("790");
        operatorField.setText("MUKESH KUMAR GARHWAL");
        terminalField.setText("NDLS99");
        windowField.setText("99");
        shiftField.setText("3");
        paymentGwField.setText("SBI PAYMENT GATE WAY");
        qrPayloadField.setText(
                "upi://pay?pa=abc@sbi&pn=test&mc=&tr=ref000003&tn=&am=1&cu=INR&url=&mode=05&purpose=03&orgid=159002&sign=MEUCIFaORLs4mJLK7pSkb5eP69d5Xd6LstvC6xJjSXeQO9HvAiEAZh7T/OYWhaPmraL4VsY6RkVXaBq+Hel2iRewCOARItf=");
        prsTrainField.setText("12420");
        prsClassField.setText("CC");
        prsQuotaField.setText("GN");
        prsBoardingField.setText("NDLS");
        prsResUptoField.setText("LKO");
        prsOpCodeField.setText("ASHWAN");
        prsOpNameField.setText("ASHWANI");
        sourceField.setText("NDLS");
        destinationField.setText("LKO");
        fareField.setText("725");
        prsQrField.setText(
                "upi://pay?ver=01&pa=railsbiupi11@sbi&pn=RailwayPayment&mc=4112&tr=803210000686309NR&am=1&cu=INR");
        prsQrMessageField.setText("INDIAN RAILWAYS NDLS TO LKO (CC) TRN:12420 TO PAY:Rs.725");
        prsPaymentTextField.setText("PAYMENT SUCCESS");
        fillFirstPrsPassenger("RAVI KUMAR", "M", "32", "C2 - 43");
    }

    private void fillFirstPrsPassenger(String name, String sex, String age, String status) {
        if (prsPaxRowsBox == null || prsPaxRowsBox.getChildren().isEmpty()) {
            return;
        }
        Node first = prsPaxRowsBox.getChildren().get(0);
        if (!(first instanceof HBox row) || row.getChildren().size() < 4) {
            return;
        }
        ((TextField) row.getChildren().get(0)).setText(name);
        ((TextField) row.getChildren().get(1)).setText(sex);
        ((TextField) row.getChildren().get(2)).setText(age);
        ((TextField) row.getChildren().get(3)).setText(status);
    }

    @FXML
    private void onConnectLink() {
        String port = trimOrEmpty(serialPortField.getText());
        if (serialService != null
                && port.equals(serialService.linkLabel())
                && !serialService.isMockMode()) {
            appendLog("Already connected to " + port + ".");
            return;
        }
        if (serialService != null) {
            serialService.disconnectQuietly();
        }
        serialService = new SerialService(this::appendLog, port.isEmpty() ? null : port);
        appendLog("Connecting to " + serialService.linkLabel() + " …");
        serialService.connect();
    }

    @FXML
    private void onGenerateTicket() {
        TicketType type = ticketTypeCombo.getValue();
        if (type == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Please select a ticket type.");
            return;
        }
        if (type == TicketType.PRS) {
            sendPrsBooking();
            return;
        }

        String port = trimOrEmpty(serialPortField.getText());
        if (port.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Serial link",
                    "Enter the USB-serial COM port (Device Manager → Ports), then click Connect.");
            return;
        }

        if (serialService == null || serialService.isMockMode()) {
            if (serialService == null) {
                serialService = buildSerialService();
            }
            if (serialService.isMockMode()) {
                serialService.connect();
            }
        }

        String src = trimOrEmpty(sourceField.getText());
        String srcEng = trimOrEmpty(sourceEngField.getText());
        String srcHi = trimOrEmpty(sourceHindiField.getText());
        String dst = trimOrEmpty(destinationField.getText());
        String dstEng = trimOrEmpty(destEngField.getText());
        String dstHi = trimOrEmpty(destHindiField.getText());
        String fare = trimOrEmpty(fareField.getText());
        String txn = trimOrEmpty(transactionField.getText());
        String pname = trimOrEmpty(passengerNameField.getText());
        String dayPacked = null;
        String monthPacked = null;
        String adultPacked = null;
        String childPacked = null;
        String trainPacked = null;
        String classPacked = null;
        String txnTypePacked = null;
        OperatorSession operatorPacked = null;
        String paymentGwPacked = null;
        String qrPayloadPacked = null;
        String farePacked = fare;

        if (src.isEmpty() || dst.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Source and destination station codes are required.");
            return;
        }
        if (src.codePointCount(0, src.length()) > CommandSet.STATION_CODE_CHARS
                || dst.codePointCount(0, dst.length()) > CommandSet.STATION_CODE_CHARS) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Station code must be at most 4 characters.");
            return;
        }

        if (type == TicketType.UTS) {
            dayPacked = TwoDigitField.pack(trimOrEmpty(dayField.getText()), CommandSet.DATE_MIN, CommandSet.DATE_MAX)
                    .orElse(null);
            monthPacked = TwoDigitField.pack(trimOrEmpty(monthField.getText()), CommandSet.MONTH_MIN, CommandSet.MONTH_MAX)
                    .orElse(null);
            if (dayPacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Date must be a 2-digit day (01–31).");
                return;
            }
            if (monthPacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Month must be a 2-digit month (01–12).");
                return;
            }
            adultPacked = TwoDigitField.pack(trimOrEmpty(adultField.getText()), CommandSet.COUNT_MIN, CommandSet.COUNT_MAX)
                    .orElse(null);
            childPacked = TwoDigitField.pack(trimOrEmpty(childField.getText()), CommandSet.COUNT_MIN, CommandSet.COUNT_MAX)
                    .orElse(null);
            if (adultPacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Adult must be a 2-digit count (00–99).");
                return;
            }
            if (childPacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Child must be a 2-digit count (00–99).");
                return;
            }
            trainPacked = TrainTypeField.pack(trainTypeCombo.getValue()).orElse(null);
            if (trainPacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation",
                        "Type of Train must be O, E, S, T, C, R, D, M, H, J, or P.");
                return;
            }
            farePacked = FareField.pack(fare).orElse(null);
            if (farePacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Fare must be 1–5 numeric digits (e.g. 525).");
                return;
            }
            classPacked = ClassField.pack(classCombo.getValue()).orElse(null);
            if (classPacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Class must be I or II.");
                return;
            }
            txnTypePacked = TxnTypeField.pack(txnTypeCombo.getValue()).orElse(null);
            if (txnTypePacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation", "Select a transaction type (e.g. PLAT).");
                return;
            }
            operatorPacked = OperatorSession.packFrom(
                    trimOrEmpty(operatorField.getText()),
                    trimOrEmpty(terminalField.getText()),
                    trimOrEmpty(windowField.getText()),
                    trimOrEmpty(shiftField.getText())
            ).orElse(null);
            if (operatorPacked == null) {
                showAlert(Alert.AlertType.ERROR, "Validation",
                        "Operator: name ≤25 chars (no colon), terminal ≤6 chars, window 0–999, shift 0–9.");
                return;
            }
            paymentGwPacked = trimOrEmpty(paymentGwField.getText());
            qrPayloadPacked = UpiQueryAmount.overlayFare(trimOrEmpty(qrPayloadField.getText()), fare);
            if (!qrPayloadPacked.equals(trimOrEmpty(qrPayloadField.getText()))) {
                qrPayloadField.setText(qrPayloadPacked);
            }
            if (qrPayloadPacked.indexOf(CommandFrame.EOT) >= 0 || qrPayloadPacked.indexOf(CommandFrame.SOT) >= 0) {
                showAlert(Alert.AlertType.ERROR, "Validation",
                        "QR payload cannot contain SOT $ or EOT ^.");
                return;
            }
        }

        Instant ts = Instant.now();
        TicketData data = new TicketData(
                type,
                new StationField(src, srcEng, srcHi),
                new StationField(dst, dstEng, dstHi),
                farePacked,
                txn,
                type == TicketType.PRS ? pname : null,
                dayPacked,
                monthPacked,
                adultPacked,
                childPacked,
                trainPacked,
                classPacked,
                txnTypePacked,
                operatorPacked,
                paymentGwPacked,
                qrPayloadPacked
        );
        java.util.List<String> frames = PacketBuilder.buildFrames(data, ts);

        appendLog("Frames built (" + frames.size() + "):");
        for (String frame : frames) {
            appendLog(frame);
        }

        if (serialService == null) {
            showAlert(Alert.AlertType.ERROR, "Serial link",
                    "Could not open the COM port. Check Device Manager and that the controller RS232 cable is connected.");
            return;
        }

        boolean ok = true;
        for (String frame : frames) {
            if (!serialService.sendLine(frame)) {
                ok = false;
                break;
            }
        }
        if (ok) {
            appendLog("Packet(s) sent successfully over RS232.");
        } else {
            appendLog("Send failed — check COM port and cable, then click Connect.");
        }
    }

    @FXML
    private void onPrsPing() {
        if (!ensureSerialReady()) {
            return;
        }
        sendOne(PrsFrame.wrapPingQuery(), "PRS ping (110) sent — controller should reply with S.");
    }

    @FXML
    private void onPrsPayOk() {
        sendPrsPayment(PrsFrame.SUB_PAY_OK, "PRS payment success (113) sent.");
    }

    @FXML
    private void onPrsPayFail() {
        sendPrsPayment(PrsFrame.SUB_PAY_FAIL, "PRS payment fail (114) sent.");
    }

    private void sendPrsPayment(String sub, String successMessage) {
        if (!ensureSerialReady()) {
            return;
        }
        String text = trimOrEmpty(prsPaymentTextField.getText());
        if (text.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Enter payment text (up to 25 characters).");
            return;
        }
        if (text.length() > 25) {
            text = text.substring(0, 25);
            prsPaymentTextField.setText(text);
        }
        sendOne(PrsFrame.wrap(sub, PrsFrame.QUERY, text), successMessage);
    }

    private void sendPrsBooking() {
        if (!ensureSerialReady()) {
            return;
        }
        String src = trimOrEmpty(sourceField.getText());
        String dst = trimOrEmpty(destinationField.getText());
        if (src.isEmpty() || dst.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Source and destination station codes are required.");
            return;
        }
        String train = trimOrEmpty(prsTrainField.getText());
        if (train.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Train number is required.");
            return;
        }
        String day = TwoDigitField.pack(trimOrEmpty(dayField.getText()), CommandSet.DATE_MIN, CommandSet.DATE_MAX)
                .orElse(null);
        String month = TwoDigitField.pack(trimOrEmpty(monthField.getText()), CommandSet.MONTH_MIN, CommandSet.MONTH_MAX)
                .orElse(null);
        if (day == null || month == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Date must be DD and month MM.");
            return;
        }
        List<PrsTdrc.PrsPassenger> pax = collectPrsPassengers();
        if (pax.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Enter at least one passenger (name + status/seat).");
            return;
        }
        String resUpto = trimOrEmpty(prsResUptoField.getText());
        if (resUpto.isEmpty()) {
            resUpto = dst;
        }
        String boarding = trimOrEmpty(prsBoardingField.getText());
        if (boarding.isEmpty()) {
            boarding = src;
        }
        PrsTdrc tdrc = new PrsTdrc(
                train,
                day,
                month,
                src,
                trimOrEmpty(prsClassField.getText()),
                trimOrEmpty(prsQuotaField.getText()),
                dst,
                String.format("%02d", pax.size()),
                resUpto,
                boarding,
                trimOrEmpty(prsOpCodeField.getText()),
                trimOrEmpty(prsOpNameField.getText()),
                trimOrEmpty(prsSpecialField.getText()),
                trimOrEmpty(fareField.getText()),
                pax,
                "",
                "",
                ""
        );
        List<String> frames = new ArrayList<>();
        frames.add(PrsFrame.wrap(PrsFrame.SUB_TDRC, PrsFrame.QUERY, tdrc.packBody()));
        String qr = trimOrEmpty(prsQrField.getText());
        if (!qr.isEmpty()) {
            String overlaid = overlayPrsQrAmount(qr, tdrc.getFare());
            String message = trimOrEmpty(prsQrMessageField.getText());
            frames.add(PrsFrame.wrap(PrsFrame.SUB_QR, PrsFrame.QUERY, PrsQr.pack(overlaid, message)));
        }
        sendFrames(frames, "PRS TDRC (111)" + (qr.isEmpty() ? "" : " + QR (112)") + " sent.");
    }

    private List<PrsTdrc.PrsPassenger> collectPrsPassengers() {
        List<PrsTdrc.PrsPassenger> pax = new ArrayList<>();
        if (prsPaxRowsBox == null) {
            return pax;
        }
        for (Node node : prsPaxRowsBox.getChildren()) {
            if (!(node instanceof HBox row) || row.getChildren().size() < 4) {
                continue;
            }
            String name = trimOrEmpty(((TextField) row.getChildren().get(0)).getText());
            String sex = trimOrEmpty(((TextField) row.getChildren().get(1)).getText());
            String age = trimOrEmpty(((TextField) row.getChildren().get(2)).getText());
            String status = trimOrEmpty(((TextField) row.getChildren().get(3)).getText());
            if (name.isEmpty() && status.isEmpty()) {
                continue;
            }
            pax.add(new PrsTdrc.PrsPassenger(name, sex, age, status));
        }
        return pax;
    }

    private static String overlayPrsQrAmount(String qr, String fare) {
        if (fare == null || fare.isBlank() || !qr.toLowerCase().contains("am=")) {
            return qr;
        }
        try {
            int n = Integer.parseInt(fare.trim().replaceAll("\\D", ""));
            return qr.replaceFirst("([?&]am=)[^&]*", "$1" + n);
        } catch (NumberFormatException ex) {
            return qr;
        }
    }

    private void sendFrames(List<String> frames, String successMessage) {
        boolean ok = true;
        for (String frame : frames) {
            appendLog("Frame: " + PrsFrame.toLog(frame));
            if (!serialService.sendLine(frame)) {
                ok = false;
                break;
            }
        }
        if (ok) {
            appendLog(successMessage);
        } else {
            appendLog("Send failed — check COM port and cable, then click Connect.");
        }
    }

    @FXML
    private void onClearDisplay() {
        if (!ensureSerialReady()) {
            return;
        }
        sendOne(PacketBuilder.clearDisplay(), "Clear display (code 13) sent.");
    }

    @FXML
    private void onCancellationRefund() {
        if (!ensureSerialReady()) {
            return;
        }
        CancellationRefund refund = CancellationRefund.packFromAmount(trimOrEmpty(refundAmountField.getText()))
                .orElse(null);
        if (refund == null) {
            showAlert(Alert.AlertType.ERROR, "Validation",
                    "Cancellation refund amount must be 1–5 numeric digits (e.g. 790).");
            return;
        }
        String txnTypePacked = TxnTypeField.pack(txnTypeCombo.getValue()).orElse(null);
        if (txnTypePacked == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Select a transaction type.");
            return;
        }
        java.util.List<String> frames = PacketBuilder.cancellationRefund(refund, txnTypePacked);
        boolean ok = true;
        for (String frame : frames) {
            appendLog("Frame: " + frame);
            if (!serialService.sendLine(frame)) {
                ok = false;
                break;
            }
        }
        if (ok) {
            appendLog("Cancellation refund (code 12/14) sent.");
        } else {
            appendLog("Send failed — check COM port and cable, then click Connect.");
        }
    }

    private SerialService buildSerialService() {
        String port = trimOrEmpty(serialPortField.getText());
        return new SerialService(this::appendLog, port.isEmpty() ? null : port);
    }

    private void updatePassengerVisibility(TicketType type) {
        boolean prs = type == TicketType.PRS;
        setShown(prsPanel, prs);
        setShown(prsPingButton, prs);
        setShown(prsPayOkButton, prs);
        setShown(prsPayFailButton, prs);
        setShown(passengerNameLabel, false);
        setShown(passengerNameField, false);
        setShown(transactionLabel, !prs);
        setShown(transactionField, !prs);
        sourceEngField.setVisible(!prs);
        sourceEngField.setManaged(!prs);
        sourceHindiField.setVisible(!prs);
        sourceHindiField.setManaged(!prs);
        destEngField.setVisible(!prs);
        destEngField.setManaged(!prs);
        destHindiField.setVisible(!prs);
        destHindiField.setManaged(!prs);
        dateLabel.setVisible(true);
        dateLabel.setManaged(true);
        dateBox.setVisible(true);
        dateBox.setManaged(true);
        adultChildLabel.setVisible(!prs);
        adultChildLabel.setManaged(!prs);
        adultChildBox.setVisible(!prs);
        adultChildBox.setManaged(!prs);
        trainTypeLabel.setVisible(!prs);
        trainTypeLabel.setManaged(!prs);
        trainTypeCombo.setVisible(!prs);
        trainTypeCombo.setManaged(!prs);
        classTxnLabel.setVisible(!prs);
        classTxnLabel.setManaged(!prs);
        classTxnBox.setVisible(!prs);
        classTxnBox.setManaged(!prs);
        setShown(refundLabel, !prs);
        setShown(refundBox, !prs);
        setShown(refundButton, !prs);
        operatorLabel.setVisible(!prs);
        operatorLabel.setManaged(!prs);
        operatorField.setVisible(!prs);
        operatorField.setManaged(!prs);
        terminalLabel.setVisible(!prs);
        terminalLabel.setManaged(!prs);
        terminalBox.setVisible(!prs);
        terminalBox.setManaged(!prs);
        paymentLabel.setVisible(!prs);
        paymentLabel.setManaged(!prs);
        paymentBox.setVisible(!prs);
        paymentBox.setManaged(!prs);
        if (!prs) {
            passengerNameField.clear();
        }
    }

    private static void setShown(Node node, boolean shown) {
        if (node == null) {
            return;
        }
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private boolean ensureSerialReady() {
        String port = trimOrEmpty(serialPortField.getText());
        if (port.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Serial link",
                    "Enter the USB-serial COM port (Device Manager → Ports), then click Connect.");
            return false;
        }
        if (serialService == null || serialService.isMockMode()) {
            if (serialService == null) {
                serialService = buildSerialService();
            }
            if (serialService.isMockMode()) {
                serialService.connect();
            }
        }
        if (serialService == null) {
            showAlert(Alert.AlertType.ERROR, "Serial link",
                    "Could not open the COM port. Check Device Manager and that the controller RS232 cable is connected.");
            return false;
        }
        return true;
    }

    private void sendOne(String frame, String successMessage) {
        appendLog("Frame: " + (PrsFrame.isFrame(frame) ? PrsFrame.toLog(frame) : frame));
        if (serialService.sendLine(frame)) {
            appendLog(successMessage);
        } else {
            appendLog("Send failed — check COM port and cable, then click Connect.");
        }
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    private void appendLog(String line) {
        Platform.runLater(() -> {
            logArea.appendText(line + "\n");
            logArea.positionCaret(logArea.getText().length());
        });
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
