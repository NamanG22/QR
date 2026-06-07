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
 * Controller for the supervisor form: validates input, orchestrates {@link PacketBuilder}
 * and {@link SerialService}, and mirrors status into the log panel.
 * <p>
 * UI handling notes:
 * </p>
 * <ul>
 *   <li>{@link #initialize()} runs after FXML injection — sets combo defaults, toggles PRS-only fields,
 *       and triggers serial auto-connect so the operator sees link status immediately.</li>
 *   <li>Ticket type drives visibility/managed state for passenger name so UTS layouts stay compact.</li>
 *   <li>Validation uses modal {@link Alert}s (blocking) consistent with kiosk-style operator tooling.</li>
 *   <li>All log lines go through {@link #appendLog(String)} on the FX thread via {@link Platform#runLater}
 *       so {@link SerialService} stays UI-thread agnostic.</li>
 * </ul>
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
    private TextArea logArea;

    private LineOutputService linkService;

    /** Releases transport resources when the primary stage closes. */
    public void shutdown() {
        if (linkService != null) {
            linkService.disconnectQuietly();
        }
    }

    /**
     * FXML lifecycle hook — wires logging, ticket-type UX, and attempts link auto-connect.
     */
    public void initialize() {
        ticketTypeCombo.getItems().setAll(TicketType.values());
        ticketTypeCombo.setValue(TicketType.UTS);

        if (TransportConfig.useTcp()) {
            linkService = new TcpOutputService(this::appendLog);
        } else {
            linkService = new SerialService(this::appendLog);
        }
        linkService.connect();

        ticketTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePassengerVisibility(newVal));
        updatePassengerVisibility(ticketTypeCombo.getValue());
    }

    /**
     * Called from FXML — validates fields, builds packet, sends over serial (or mock).
     */
    @FXML
    private void onGenerateTicket() {
        TicketType type = ticketTypeCombo.getValue();
        if (type == null) {
            showAlert(Alert.AlertType.ERROR, "Validation", "Please select a ticket type.");
            return;
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

        boolean ok = linkService.sendLine(packet);
        if (ok) {
            appendLog(linkService.isMockMode()
                    ? "Packet logged successfully (mock mode — no hardware write)."
                    : "Packet sent successfully.");
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

    /**
     * Serial callbacks may originate off the FX thread; marshal UI updates safely.
     */
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
