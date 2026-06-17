package com.railway.supervisor;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Supervisor form: builds ticket packets and sends them to the controller over RS232.
 * <p>
 * Development: USB-to-serial adapter on the console PC (select its COM port in the UI).
 * Production: direct RS232 from the CRIS terminal to the controller RS232 input.
 * </p>
 */
public class SupervisorController {

    private static final Pattern NUMERIC_FARE = Pattern.compile("^\\d+(\\.\\d+)?$");

    @FXML
    private ComboBox<TicketType> ticketTypeCombo;
    @FXML
    private TextField sourceField;
    @FXML
    private TextField destinationField;
    @FXML
    private TextField fareField;
    @FXML
    private TextField transactionField;
    @FXML
    private TextField passengerNameField;
    @FXML
    private Label passengerNameLabel;
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
        appendLog("Click Connect, then Generate Ticket.");
        appendLog("Controller listens on RS232 — set QFRDS_CONTROLLER_PORT on the thin client if needed.");
        appendLog("See SERIAL_SETUP.md in the repo root for wiring and port help.");

        ticketTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePassengerVisibility(newVal));
        updatePassengerVisibility(ticketTypeCombo.getValue());
    }

    @FXML
    private void onConnectLink() {
        serialService = buildSerialService();
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

        String port = trimOrEmpty(serialPortField.getText());
        if (port.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Serial link",
                    "Enter the USB-serial COM port (Device Manager → Ports), then click Connect.");
            return;
        }

        if (serialService == null || serialService.isMockMode()) {
            onConnectLink();
        }

        String src = trimOrEmpty(sourceField.getText());
        String dst = trimOrEmpty(destinationField.getText());
        String fare = trimOrEmpty(fareField.getText());
        String txn = trimOrEmpty(transactionField.getText());
        String pname = trimOrEmpty(passengerNameField.getText());

        if (src.isEmpty() || dst.isEmpty() || fare.isEmpty() || txn.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Source, destination, fare, and transaction ID are required.");
            return;
        }

        if (!NUMERIC_FARE.matcher(fare).matches()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Fare must be numeric (integer or decimal).");
            return;
        }

        if (type == TicketType.PRS && pname.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Passenger name is required for PRS tickets.");
            return;
        }

        Instant ts = Instant.now();
        TicketData data = new TicketData(type, src, dst, fare, txn, type == TicketType.PRS ? pname : null);
        String packet = PacketBuilder.build(data, ts);

        appendLog("Packet built:");
        appendLog(packet);

        if (serialService == null) {
            showAlert(Alert.AlertType.ERROR, "Serial link",
                    "Could not open the COM port. Check Device Manager and that the controller RS232 cable is connected.");
            return;
        }

        boolean ok = serialService.sendLine(packet);
        if (ok) {
            appendLog(serialService.isMockMode()
                    ? "Send failed — still in mock mode. Click Connect after plugging in the USB-serial adapter."
                    : "Packet sent successfully over RS232.");
        }
    }

    private SerialService buildSerialService() {
        if (serialService != null) {
            serialService.disconnectQuietly();
        }
        String port = trimOrEmpty(serialPortField.getText());
        return new SerialService(this::appendLog, port.isEmpty() ? null : port);
    }

    private void updatePassengerVisibility(TicketType type) {
        boolean prs = type == TicketType.PRS;
        passengerNameLabel.setVisible(prs);
        passengerNameLabel.setManaged(prs);
        passengerNameField.setVisible(prs);
        passengerNameField.setManaged(prs);
        if (!prs) {
            passengerNameField.clear();
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
