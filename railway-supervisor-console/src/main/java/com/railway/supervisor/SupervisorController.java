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
 * Supervisor form: builds ticket packets and sends them to the controller over TCP (network)
 * or local serial (same PC + com0com).
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
    private TextField controllerHostField;
    @FXML
    private TextField controllerPortField;
    @FXML
    private TextArea logArea;

    private LineOutputService linkService;

    public void shutdown() {
        if (linkService != null) {
            linkService.disconnectQuietly();
        }
    }

    public void initialize() {
        ticketTypeCombo.getItems().setAll(TicketType.values());
        ticketTypeCombo.setValue(TicketType.UTS);

        String savedHost = firstNonBlank(System.getenv("QFRDS_TCP_HOST"), System.getProperty("qfrds.tcp.host"));
        controllerHostField.setText(savedHost == null ? "" : savedHost);
        controllerPortField.setText(String.valueOf(TransportConfig.tcpPort()));

        appendLog("Enter the thin client IP above, click Connect, then Generate Ticket.");
        appendLog("See NETWORK_SETUP.md in the repo root for firewall and IP help.");

        ticketTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePassengerVisibility(newVal));
        updatePassengerVisibility(ticketTypeCombo.getValue());
    }

    @FXML
    private void onConnectLink() {
        linkService = buildLinkService();
        appendLog("Connecting to " + linkService.linkLabel() + " …");
        linkService.connect();
    }

    @FXML
    private void onGenerateTicket() {
        TicketType type = ticketTypeCombo.getValue();
        if (type == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Please select a ticket type.");
            return;
        }

        String host = trimOrEmpty(controllerHostField.getText());
        if (host.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Controller link",
                    "Enter the thin client IP address (from ipconfig on the thin client), then click Connect.");
            return;
        }

        if (linkService == null || linkService.isMockMode()) {
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

        if (linkService == null) {
            showAlert(Alert.AlertType.ERROR, "Controller link", "Could not connect. Check IP, firewall, and that controller is running on the thin client.");
            return;
        }

        boolean ok = linkService.sendLine(packet);
        if (ok) {
            appendLog(linkService.isMockMode()
                    ? "Send failed — still in mock mode. Click Connect after starting the controller on the thin client."
                    : "Packet sent successfully.");
        }
    }

    private LineOutputService buildLinkService() {
        if (linkService != null) {
            linkService.disconnectQuietly();
        }
        String host = trimOrEmpty(controllerHostField.getText());
        int port = parsePort(controllerPortField.getText());
        return new TcpOutputService(this::appendLog, host.isEmpty() ? "127.0.0.1" : host, port);
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) {
            return TransportConfig.tcpPort();
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return TransportConfig.tcpPort();
        }
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
