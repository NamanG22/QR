package com.railway.qfrds;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Passenger-facing QFRDS display: switches between **UTS** and **PRS** TAN-style boards per
 * {@link TicketType}. Fields not carried on the serial packet use neutral placeholders (—) so the
 * layout matches the government mock-up while the demo wire format stays unchanged.
 */
public class PassengerDisplayView implements Initializable {

    private static final DateTimeFormatter LAST_UPDATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter[] TS_INPUTS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    private static final DateTimeFormatter DISPLAY_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DDMM = DateTimeFormatter.ofPattern("dd / MM");

    @FXML
    private BorderPane utsBoard;
    @FXML
    private BorderPane prsBoard;

    /* UTS */
    @FXML
    private Label utsTerminalId;
    @FXML
    private Label utsWindowNo;
    @FXML
    private Label utsFrom;
    @FXML
    private Label utsTo;
    @FXML
    private Label utsDate;
    @FXML
    private Label utsAdult;
    @FXML
    private Label utsChild;
    @FXML
    private Label utsClass;
    @FXML
    private Label utsFare;
    @FXML
    private Label utsTrainType;
    @FXML
    private Label utsPayMode;
    @FXML
    private Label utsTxnType;
    @FXML
    private Label utsOperator;
    @FXML
    private StackPane utsQrPlaceholder;
    @FXML
    private ImageView utsQrImage;
    @FXML
    private Label utsLastUpdated;

    /* PRS */
    @FXML
    private Label prsOperatorCode;
    @FXML
    private Label prsFrom;
    @FXML
    private Label prsTo;
    @FXML
    private Label prsTrainNo;
    @FXML
    private Label prsQuota;
    @FXML
    private Label prsDate;
    @FXML
    private Label prsTotalPax;
    @FXML
    private Label prsClass;
    @FXML
    private Label prsFare;
    @FXML
    private Label prsBoarding;
    @FXML
    private Label prsResUpto;
    @FXML
    private Label prsPaxName;
    @FXML
    private Label prsPaxSex;
    @FXML
    private Label prsPaxAge;
    @FXML
    private Label prsPaxStatus;
    @FXML
    private Label prsOperatorName;
    @FXML
    private StackPane prsQrPlaceholder;
    @FXML
    private ImageView prsQrImage;
    @FXML
    private Label prsLastUpdated;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clearAll();
    }

    /**
     * Refreshes the active board (UTS vs PRS) and QR bitmap. JavaFX thread only.
     */
    public void applyTicketUpdate(TicketData ticket, WritableImage qrImage) {
        boolean prs = ticket.getTicketType() == TicketType.PRS;
        utsBoard.setVisible(!prs);
        utsBoard.setManaged(!prs);
        prsBoard.setVisible(prs);
        prsBoard.setManaged(prs);

        if (prs) {
            fillPrs(ticket, qrImage);
        } else {
            fillUts(ticket, qrImage);
        }
    }

    private void fillUts(TicketData t, WritableImage qrImage) {
        utsTerminalId.setText(compactTerminalId(t.getTransactionId()));
        utsWindowNo.setText("1");
        utsFrom.setText(t.getSourceStation());
        utsTo.setText(t.getDestinationStation());
        utsDate.setText(formatDateLine(t.getTimestampRaw()));
        utsAdult.setText("1");
        utsChild.setText("0");
        utsClass.setText("II");
        utsFare.setText(formatFareUts(t.getFare()));
        utsTrainType.setText("—");
        utsPayMode.setText("UPI-QR");
        utsTxnType.setText(t.getTransactionId());
        utsOperator.setText("—");

        bindQr(utsQrImage, utsQrPlaceholder, qrImage);
        utsLastUpdated.setText("Last updated: " + LocalDateTime.now().format(LAST_UPDATED_FMT));
    }

    private void fillPrs(TicketData t, WritableImage qrImage) {
        prsOperatorCode.setText("CLIENT");
        prsFrom.setText(t.getSourceStation());
        prsTo.setText(t.getDestinationStation());
        prsTrainNo.setText("—");
        prsQuota.setText("GN");
        prsDate.setText(formatDateShort(t.getTimestampRaw()));
        prsTotalPax.setText("01");
        prsClass.setText("SL");
        prsFare.setText(formatFarePrs(t.getFare()));
        prsBoarding.setText(t.getSourceStation());
        prsResUpto.setText(t.getDestinationStation());

        Optional<String> name = t.getPassengerName();
        prsPaxName.setText(name.orElse("—"));
        prsPaxSex.setText("—");
        prsPaxAge.setText("—");
        prsPaxStatus.setText("—");
        prsOperatorName.setText("—");

        bindQr(prsQrImage, prsQrPlaceholder, qrImage);
        prsLastUpdated.setText("Last updated: " + LocalDateTime.now().format(LAST_UPDATED_FMT));
    }

    private static void bindQr(ImageView imageView, StackPane placeholder, WritableImage qrImage) {
        if (qrImage != null) {
            imageView.setImage(qrImage);
            imageView.setVisible(true);
            imageView.setManaged(true);
            placeholder.setVisible(false);
            placeholder.setManaged(false);
        } else {
            imageView.setImage(null);
            imageView.setVisible(false);
            imageView.setManaged(false);
            placeholder.setVisible(true);
            placeholder.setManaged(true);
        }
    }

    private void clearAll() {
        utsBoard.setVisible(true);
        utsBoard.setManaged(true);
        prsBoard.setVisible(false);
        prsBoard.setManaged(false);

        utsTerminalId.setText("—");
        utsWindowNo.setText("—");
        utsFrom.setText("—");
        utsTo.setText("—");
        utsDate.setText("—/--/----");
        utsAdult.setText("0");
        utsChild.setText("0");
        utsClass.setText("—");
        utsFare.setText("—");
        utsTrainType.setText("—");
        utsPayMode.setText("UPI-QR");
        utsTxnType.setText("—");
        utsOperator.setText("—");
        utsLastUpdated.setText("Last updated: —");
        bindQr(utsQrImage, utsQrPlaceholder, null);

        prsFrom.setText("—");
        prsTo.setText("—");
        prsTrainNo.setText("—");
        prsQuota.setText("GN");
        prsDate.setText("—");
        prsTotalPax.setText("—");
        prsClass.setText("—");
        prsFare.setText("—");
        prsBoarding.setText("—");
        prsResUpto.setText("—");
        prsPaxName.setText("—");
        prsPaxSex.setText("—");
        prsPaxAge.setText("—");
        prsPaxStatus.setText("—");
        prsOperatorName.setText("—");
        prsLastUpdated.setText("Last updated: —");
        bindQr(prsQrImage, prsQrPlaceholder, null);
    }

    /** Short terminal id derived from transaction id for the UTS header strip. */
    private static String compactTerminalId(String txn) {
        if (txn == null || txn.isBlank()) {
            return "—";
        }
        String u = txn.trim();
        return u.length() <= 12 ? u : u.substring(0, 12);
    }

    private static String formatDateLine(String tsRaw) {
        Optional<LocalDate> d = parseDate(tsRaw);
        return d.map(DISPLAY_DDMMYYYY::format).orElse(tsRaw != null ? tsRaw : "—");
    }

    private static String formatDateShort(String tsRaw) {
        Optional<LocalDate> d = parseDate(tsRaw);
        return d.map(DISPLAY_DDMM::format).orElse("—");
    }

    private static Optional<LocalDate> parseDate(String tsRaw) {
        if (tsRaw == null || tsRaw.isBlank()) {
            return Optional.empty();
        }
        String s = tsRaw.trim();
        for (DateTimeFormatter f : TS_INPUTS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(s, f);
                return Optional.of(ldt.toLocalDate());
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        try {
            return Optional.of(LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private static String formatFareUts(String fare) {
        return fare == null ? "—" : fare.trim();
    }

    private static String formatFarePrs(String fare) {
        if (fare == null || fare.isBlank()) {
            return "—";
        }
        try {
            double v = Double.parseDouble(fare.trim());
            return String.format("%.2f", v);
        } catch (NumberFormatException ex) {
            return fare.trim();
        }
    }

}
