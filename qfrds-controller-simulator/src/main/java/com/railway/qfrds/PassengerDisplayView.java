package com.railway.qfrds;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Passenger-facing industrial display (1024×768 oriented layout): branding, fare emphasis,
 * conditional PRS name line, QR bitmap, and payment call-to-action.
 */
public class PassengerDisplayView implements Initializable {

    private static final DateTimeFormatter LAST_UPDATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @FXML
    private Label ticketTypeValue;
    @FXML
    private Label sourceValue;
    @FXML
    private Label destinationValue;
    @FXML
    private Label fareValue;
    @FXML
    private Label txnValue;
    @FXML
    private Label timestampValue;
    @FXML
    private Label passengerRow;
    @FXML
    private Label passengerValue;
    @FXML
    private Label lastUpdatedLabel;
    @FXML
    private ImageView qrImageView;
    @FXML
    private StackPane qrPlaceholder;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearDisplay();
    }

    /**
     * Atomically refreshes all passenger-visible fields and swaps the QR bitmap.
     * Safe to call from JavaFX thread only (invoked via {@link Platform#runLater} from controller).
     */
    public void applyTicketUpdate(TicketData ticket, WritableImage qrImage) {
        ticketTypeValue.setText(ticket.getTicketType().name());
        sourceValue.setText(ticket.getSourceStation());
        destinationValue.setText(ticket.getDestinationStation());
        fareValue.setText("₹ " + ticket.getFare());
        txnValue.setText(ticket.getTransactionId());
        timestampValue.setText(ticket.getTimestampRaw());

        boolean prs = ticket.getTicketType() == TicketType.PRS;
        passengerRow.setVisible(prs);
        passengerRow.setManaged(prs);
        if (prs) {
            passengerValue.setText(ticket.getPassengerName().orElse("—"));
        }

        if (qrImage != null) {
            qrImageView.setImage(qrImage);
            qrImageView.setVisible(true);
            qrImageView.setManaged(true);
            qrPlaceholder.setVisible(false);
            qrPlaceholder.setManaged(false);
        } else {
            qrImageView.setImage(null);
            qrImageView.setVisible(false);
            qrImageView.setManaged(false);
            qrPlaceholder.setVisible(true);
            qrPlaceholder.setManaged(true);
        }

        lastUpdatedLabel.setText("Last updated: " + LAST_UPDATED_FMT.format(Instant.now()));
    }

    private void clearDisplay() {
        ticketTypeValue.setText("—");
        sourceValue.setText("—");
        destinationValue.setText("—");
        fareValue.setText("—");
        txnValue.setText("—");
        timestampValue.setText("—");
        passengerRow.setVisible(false);
        passengerRow.setManaged(false);
        lastUpdatedLabel.setText("Last updated: —");
        qrImageView.setImage(null);
    }
}
