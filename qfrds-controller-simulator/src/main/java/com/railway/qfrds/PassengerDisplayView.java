package com.railway.qfrds;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
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
 * Passenger-facing QFRDS display: **UTS** and **PRS** RDSO-style boards; {@link TicketType} selects
 * which board is shown.
 */
public class PassengerDisplayView implements Initializable {

    private static final DateTimeFormatter LAST_UPDATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter[] TS_INPUTS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    private static final DateTimeFormatter DISPLAY_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DDMM = DateTimeFormatter.ofPattern("dd/MM");

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
    private ImageView utsLogoPlaceholder;

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
    private Label prsOperatorName;
    @FXML
    private StackPane prsQrPlaceholder;
    @FXML
    private ImageView prsQrImage;
    @FXML
    private ImageView prsLogoPlaceholder;
    @FXML
    private TableView<PaxRow> prsPaxTable;

    @FXML
    private Label footerLastUpdated;

    private final ObservableList<PaxRow> prsPaxRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Tooltip.install(utsLogoPlaceholder, new Tooltip("Indian Railways Logo"));
        Tooltip.install(prsLogoPlaceholder, new Tooltip("Indian Railways Logo"));
        initPassengerTable();
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
        utsFare.setText(formatFareRupee(t.getFare()));
        utsTrainType.setText("—");
        utsPayMode.setText("UPI-QR");
        utsTxnType.setText(t.getTransactionId());
        utsOperator.setText("—");

        bindQr(utsQrImage, utsQrPlaceholder, qrImage);
        footerLastUpdated.setText("Last updated: " + LocalDateTime.now().format(LAST_UPDATED_FMT));
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
        prsFare.setText(formatFareRupee(t.getFare()));
        prsBoarding.setText(t.getSourceStation());
        prsResUpto.setText(t.getDestinationStation());

        Optional<String> name = t.getPassengerName();
        applyPassengerTableOverlay(name);

        prsOperatorName.setText("—");

        bindQr(prsQrImage, prsQrPlaceholder, qrImage);
        footerLastUpdated.setText("Last updated: " + LocalDateTime.now().format(LAST_UPDATED_FMT));
    }

    private void applyPassengerTableOverlay(Optional<String> ticketName) {
        restoreDefaultPassengerRows();
        ticketName.filter(n -> !n.isBlank()).ifPresent(n -> prsPaxRows.get(0).nameProperty().set(n.trim()));
    }

    private void restoreDefaultPassengerRows() {
        prsPaxRows.setAll(
                new PaxRow("Ravi", "M", "23", "S4-45"),
                new PaxRow("AmitK", "F", "34", "S4-65"),
                new PaxRow("TARAN", "M", "33", "S4-67"),
                new PaxRow("Neeraj", "M", "22", "S4-68"),
                new PaxRow("Sangeeta", "M", "34", "S4-80"),
                new PaxRow("Kohitz", "M", "23", "S4-70")
        );
    }

    private void initPassengerTable() {
        TableColumn<PaxRow, String> colName = new TableColumn<>("Passenger Name");
        colName.setCellValueFactory(cd -> cd.getValue().nameProperty());

        TableColumn<PaxRow, String> colSex = new TableColumn<>("Sex");
        colSex.setCellValueFactory(cd -> cd.getValue().sexProperty());
        colSex.setMaxWidth(56);

        TableColumn<PaxRow, String> colAge = new TableColumn<>("Age");
        colAge.setCellValueFactory(cd -> cd.getValue().ageProperty());
        colAge.setMaxWidth(52);

        TableColumn<PaxRow, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(cd -> cd.getValue().statusProperty());

        prsPaxTable.getColumns().clear();
        prsPaxTable.getColumns().add(colName);
        prsPaxTable.getColumns().add(colSex);
        prsPaxTable.getColumns().add(colAge);
        prsPaxTable.getColumns().add(colStatus);
        prsPaxTable.setItems(prsPaxRows);
        prsPaxTable.setFixedCellSize(24);
        prsPaxTable.setPlaceholder(new Label(""));
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
        footerLastUpdated.setText("Last updated: —");
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
        prsOperatorName.setText("—");
        restoreDefaultPassengerRows();
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

    private static String formatFareRupee(String fare) {
        if (fare == null || fare.isBlank()) {
            return "—";
        }
        String trimmed = fare.trim();
        if (trimmed.startsWith("₹")) {
            return trimmed;
        }
        try {
            double v = Double.parseDouble(trimmed);
            return String.format("₹ %.2f", v);
        } catch (NumberFormatException ex) {
            return "₹ " + trimmed;
        }
    }

    /** Mutable row for the PRS passenger {@link TableView}. */
    public static final class PaxRow {
        private final StringProperty name = new SimpleStringProperty();
        private final StringProperty sex = new SimpleStringProperty();
        private final StringProperty age = new SimpleStringProperty();
        private final StringProperty status = new SimpleStringProperty();

        public PaxRow(String name, String sex, String age, String status) {
            this.name.set(name);
            this.sex.set(sex);
            this.age.set(age);
            this.status.set(status);
        }

        public StringProperty nameProperty() {
            return name;
        }

        public StringProperty sexProperty() {
            return sex;
        }

        public StringProperty ageProperty() {
            return age;
        }

        public StringProperty statusProperty() {
            return status;
        }
    }

}
