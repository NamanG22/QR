package com.railway.qfrds;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;

import java.io.IOException;
import java.io.InputStream;
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

    private static final String PASSENGER_FONT_PATH = "/fonts/RozhaOne-Regular.ttf";
    private static volatile boolean passengerFontLoaded;

    private static void ensurePassengerFontLoaded() {
        if (passengerFontLoaded) {
            return;
        }
        synchronized (PassengerDisplayView.class) {
            if (passengerFontLoaded) {
                return;
            }
            try (InputStream in = PassengerDisplayView.class.getResourceAsStream(PASSENGER_FONT_PATH)) {
                if (in != null) {
                    Font.loadFont(in, 12);
                }
            } catch (IOException ignored) {
                // fall back to stylesheet font stack
            }
            passengerFontLoaded = true;
        }
    }

    private static final DateTimeFormatter LAST_UPDATED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter[] TS_INPUTS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    };

    private static final DateTimeFormatter DISPLAY_DDMMYYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DISPLAY_DDMM = DateTimeFormatter.ofPattern("dd/MM");

    private static final String UNSET_TEXT = "-".repeat(10);
    private static final String UNSET_DATE = "-".repeat(2) + "/" + "-".repeat(2) + "/" + "-".repeat(4);
    private static final String UNSET_SHORT = "-".repeat(2);
    private static final String UNSET_FARE = "-".repeat(2) + "." + "-".repeat(2);

    @FXML
    private BorderPane utsBoard;
    @FXML
    private BorderPane prsBoard;
    @FXML
    private BorderPane designBoard;
    @FXML
    private HBox designPrsWrap;
    @FXML
    private HBox designUtsWrap;
    @FXML
    private StackPane designPrsBox;
    @FXML
    private StackPane designUtsBox;

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
    private Label designOperatorCode;
    @FXML
    private Label prsFrom;
    @FXML
    private Label designFrom;
    @FXML
    private Label prsTo;
    @FXML
    private Label designTo;
    @FXML
    private Label prsTrainNo;
    @FXML
    private Label designTrainNo;
    @FXML
    private Label prsQuota;
    @FXML
    private Label designQuota;
    @FXML
    private Label prsDate;
    @FXML
    private Label designDate;
    @FXML
    private Label prsTotalPax;
    @FXML
    private Label designTotalPax;
    @FXML
    private Label prsClass;
    @FXML
    private Label designClass;
    @FXML
    private Label prsFare;
    @FXML
    private Label designFare;
    @FXML
    private Label prsBoarding;
    @FXML
    private Label designBoarding;
    @FXML
    private Label prsResUpto;
    @FXML
    private Label designResUpto;
    @FXML
    private Label prsOperatorName;
    @FXML
    private Label designOperatorName;
    @FXML
    private StackPane prsQrPlaceholder;
    @FXML
    private ImageView prsQrImage;
    @FXML
    private StackPane designQrPlaceholder;
    @FXML
    private ImageView designQrImage;
    @FXML
    private ImageView prsLogoPlaceholder;
    @FXML
    private Label prsPayStatus;
    @FXML
    private Label designPayStatus;
    @FXML
    private TableView<PaxRow> prsPaxTable;
    @FXML
    private GridPane designPaxSheet;

    /* UTS design canvas (PRS layout copy) */
    @FXML
    private Label designUtsTerminalId;
    @FXML
    private Label designUtsWindowNo;
    @FXML
    private Label designUtsFrom;
    @FXML
    private Label designUtsTo;
    @FXML
    private Label designUtsDate;
    @FXML
    private Label designUtsAdult;
    @FXML
    private Label designUtsChild;
    @FXML
    private Label designUtsClass;
    @FXML
    private Label designUtsFare;
    @FXML
    private Label designUtsTrainType;
    @FXML
    private Label designUtsPayMode;
    @FXML
    private Label designUtsTxnType;
    @FXML
    private Label designUtsOperatorName;
    @FXML
    private StackPane designUtsQrPlaceholder;
    @FXML
    private ImageView designUtsQrImage;

    @FXML
    private Label footerLastUpdated;
    @FXML
    private Label footerLinkStatus;
    @FXML
    private HBox passengerFooter;

    private final ObservableList<PaxRow> prsPaxRows = FXCollections.observableArrayList();
    private final Label[][] designPaxCells = new Label[PrsTdrc.MAX_PASSENGERS][4];
    private final Scale designPrsScale = new Scale(1, 1, 0, 0);
    private final Scale designUtsScale = new Scale(1, 1, 0, 0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ensurePassengerFontLoaded();
        Tooltip.install(utsLogoPlaceholder, new Tooltip("Indian Railways Logo"));
        Tooltip.install(prsLogoPlaceholder, new Tooltip("Indian Railways Logo"));
        initPassengerTable();
        initDesignPaxSheet();
        clearAll();
        bindDesignBoxToScreen();
        setLinkStatus("—", false, false, 0, "starting");
    }

    /** RS232 health shown on the passenger screen (engineering dashboard is hidden in kiosk mode). */
    public void setLinkStatus(String port, boolean live, boolean reconnecting, int packetsReceived, String hint) {
        String state = live ? "LIVE" : (reconnecting ? "RECONNECT" : "WAITING");
        footerLinkStatus.setText("RS232 " + port + " · " + state + " · rx=" + packetsReceived + " · " + hint);
    }

    /**
     * Refreshes the active board (UTS vs PRS) and QR bitmap. JavaFX thread only.
     */
    public void applyTicketUpdate(TicketData ticket, WritableImage qrImage) {
        boolean prs = ticket.getTicketType() == TicketType.PRS;
        if (prs) {
            showDesignPrsBoard();
            fillPrs(ticket, qrImage);
        } else {
            showDesignUtsBoard();
            fillUts(ticket, qrImage);
        }
    }

    private void fillUts(TicketData t, WritableImage qrImage) {
        String terminalId = t.getOperator()
                .map(OperatorSession::getTerminalId)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> compactTerminalId(t.getTransactionId()));
        String windowNo = t.getOperator()
                .map(OperatorSession::windowDisplay)
                .filter(s -> !s.isBlank())
                .orElse("");
        String operatorName = t.getOperator()
                .map(OperatorSession::getOperatorName)
                .filter(s -> !s.isBlank())
                .orElse("");
        String terminalShown = "—".equals(terminalId) ? "" : terminalId;

        utsTerminalId.setText(terminalShown.isBlank() ? "—" : terminalShown);
        utsWindowNo.setText(windowNo.isBlank() ? "—" : windowNo);
        utsFrom.setText(t.getSourceBoardText());
        utsTo.setText(t.getDestinationBoardText());
        utsDate.setText(t.getDateDisplay().isBlank() ? "--/--" : t.getDateDisplay());
        utsAdult.setText(t.getAdult().isBlank() ? "—" : t.getAdult());
        utsChild.setText(t.getChild().isBlank() ? "—" : t.getChild());
        utsClass.setText(t.getTravelClass().isBlank() ? "—" : t.getTravelClass());
        utsFare.setText(formatFareRupee(t.getFare()));
        utsTrainType.setText(t.getTrainType().isBlank() ? "—" : TrainTypeField.display(t.getTrainType()));
        utsPayMode.setText(t.getPaymentGw().isBlank() ? "—" : t.getPaymentGw());
        utsTxnType.setText(t.getTxnType().isBlank() ? "—" : TxnTypeField.display(t.getTxnType()));
        utsOperator.setText(dash(operatorName));

        setUts(designUtsTerminalId, terminalShown, UNSET_TEXT);
        setUts(designUtsWindowNo, windowNo, UNSET_TEXT);
        setUts(designUtsFrom, t.getSourceBoardText(), UNSET_TEXT);
        setUts(designUtsTo, t.getDestinationBoardText(), UNSET_TEXT);
        setUts(designUtsDate, utsDesignDate(t), UNSET_DATE);
        setUts(designUtsAdult, t.getAdult(), UNSET_SHORT);
        setUts(designUtsChild, t.getChild(), UNSET_SHORT);
        setUts(designUtsClass, t.getTravelClass(), UNSET_SHORT);
        if (designUtsFare != null) {
            String plain = formatFarePlain(t.getFare());
            designUtsFare.setText(plain.isBlank() ? UNSET_FARE : plain);
        }
        setUts(designUtsTrainType, t.getTrainType().isBlank() ? "" : TrainTypeField.display(t.getTrainType()), UNSET_TEXT);
        setUts(designUtsPayMode, t.getPaymentGw(), UNSET_TEXT);
        setUts(designUtsTxnType, t.getTxnType().isBlank() ? "" : TxnTypeField.display(t.getTxnType()), UNSET_TEXT);
        setUts(designUtsOperatorName, operatorName, "");

        bindQr(utsQrImage, utsQrPlaceholder, qrImage);
        bindQr(designUtsQrImage, designUtsQrPlaceholder, qrImage);
        footerLastUpdated.setText("Last updated: " + LocalDateTime.now().format(LAST_UPDATED_FMT));
    }

    /**
     * Refreshes the PRS board from a TDRC/QR/payment packet. JavaFX thread only.
     */
    public void applyPrsUpdate(PrsTdrc booking, WritableImage qrImage) {
        showDesignPrsBoard();
        fillPrsBooking(booking, qrImage);
    }

    private void fillPrs(TicketData t, WritableImage qrImage) {
        setOperatorCode("CLIENT");
        setPaired(prsFrom, designFrom, t.getSourceStation(), UNSET_TEXT);
        setPaired(prsTo, designTo, t.getDestinationStation(), UNSET_TEXT);
        setPaired(prsTrainNo, designTrainNo, "", UNSET_TEXT);
        setPaired(prsQuota, designQuota, "GN", UNSET_SHORT);
        setPaired(prsDate, designDate, formatDateShort(t.getTimestampRaw()));
        setUts(designDate, parsedFullDate(t.getTimestampRaw()), UNSET_DATE);
        setTotalPax("01");
        setTravelClass("SL");
        setFare(t.getFare());
        setPaired(prsBoarding, designBoarding, t.getSourceStation(), UNSET_TEXT);
        setPaired(prsResUpto, designResUpto, t.getDestinationStation(), UNSET_TEXT);

        Optional<String> name = t.getPassengerName();
        applyPassengerTableOverlay(name);
        clearDesignPax();
        name.filter(n -> !n.isBlank()).ifPresent(n -> setDesignPaxCell(0, 0, n.trim()));

        setPaired(prsOperatorName, designOperatorName, "", "");
        setPayStatus("");

        bindPrsQr(qrImage);
        footerLastUpdated.setText("Last updated: " + LocalDateTime.now().format(LAST_UPDATED_FMT));
    }

    private void fillPrsBooking(PrsTdrc t, WritableImage qrImage) {
        setOperatorCode(t.getOperatorCode());
        setPaired(prsFrom, designFrom, t.getFrom(), UNSET_TEXT);
        setPaired(prsTo, designTo, t.getDestination(), UNSET_TEXT);
        setPaired(prsTrainNo, designTrainNo, t.getTrainNo(), UNSET_TEXT);
        setPaired(prsQuota, designQuota, t.getQuota(), UNSET_SHORT);
        setPaired(prsDate, designDate, t.dateDisplay());
        setUts(designDate, designDateFromDayMonth(t.getDay(), t.getMonth()), UNSET_DATE);
        String pax = t.getPaxCount();
        if (pax.isBlank() && !t.getPassengers().isEmpty()) {
            pax = String.format("%02d", t.getPassengers().size());
        }
        setTotalPax(pax);
        setTravelClass(t.getTravelClass());
        setFare(t.getFare());
        setPaired(prsBoarding, designBoarding, t.getBoarding(), UNSET_TEXT);
        setPaired(prsResUpto, designResUpto, t.getReservationUpto(), UNSET_TEXT);
        setPaired(prsOperatorName, designOperatorName, t.getOperatorName(), "");
        prsPaxRows.clear();
        for (PrsTdrc.PrsPassenger p : t.getPassengers()) {
            prsPaxRows.add(new PaxRow(p.name(), p.sex(), p.age(), p.status()));
        }
        fillDesignPaxFromRows();

        setPayStatus(t.getPaymentText());
        bindPrsQr(qrImage);
        footerLastUpdated.setText("Last updated: " + LocalDateTime.now().format(LAST_UPDATED_FMT));
    }

    private void setOperatorCode(String value) {
        setPaired(prsOperatorCode, designOperatorCode, value, UNSET_TEXT);
    }

    private void setTotalPax(String value) {
        setPaired(prsTotalPax, designTotalPax, value, UNSET_SHORT);
    }

    private void setTravelClass(String value) {
        setPaired(prsClass, designClass, value, UNSET_SHORT);
    }

    private void setFare(String value) {
        if (prsFare != null) {
            prsFare.setText(formatFareRupee(value));
        }
        if (designFare != null) {
            String plain = formatFarePlain(value);
            designFare.setText(plain.isBlank() ? UNSET_FARE : plain);
        }
    }

    private void setPaired(Label original, Label design, String value) {
        setPaired(original, design, value, "—");
    }

    private void setPaired(Label original, Label design, String value, String designUnset) {
        if (original != null) {
            original.setText(dash(value));
        }
        setUts(design, value, designUnset);
    }

    private void clearDesignValues() {
        setUts(designOperatorCode, "", UNSET_TEXT);
        setUts(designFrom, "", UNSET_TEXT);
        setUts(designTo, "", UNSET_TEXT);
        setUts(designTrainNo, "", UNSET_TEXT);
        setUts(designQuota, "", UNSET_SHORT);
        setUts(designDate, "", UNSET_DATE);
        setUts(designTotalPax, "", UNSET_SHORT);
        setUts(designClass, "", UNSET_SHORT);
        setUts(designFare, "", UNSET_FARE);
        setUts(designBoarding, "", UNSET_TEXT);
        setUts(designResUpto, "", UNSET_TEXT);
        setUts(designOperatorName, "", "");
        setPayStatus("");
        clearDesignPax();
        bindQr(designQrImage, designQrPlaceholder, null);
        clearDesignUtsValues();
    }

    private static void setEmpty(Label label) {
        setDesign(label, "");
    }

    private static void setDesign(Label label, String value) {
        if (label != null) {
            label.setText(dash(value));
        }
    }

    private void setPayStatus(String status) {
        String text = status == null ? "" : status.trim();
        boolean show = !text.isBlank();
        if (prsPayStatus != null) {
            prsPayStatus.setText(text);
            prsPayStatus.setVisible(show);
            prsPayStatus.setManaged(show);
        }
        if (designPayStatus != null) {
            designPayStatus.setText(text);
            designPayStatus.setVisible(show);
            designPayStatus.setManaged(show);
        }
    }

    private void bindPrsQr(WritableImage qrImage) {
        bindQr(prsQrImage, prsQrPlaceholder, qrImage);
        bindQr(designQrImage, designQrPlaceholder, qrImage);
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "—" : value;
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
        prsPaxTable.setFixedCellSize(26);
        prsPaxTable.setPlaceholder(new Label(""));
    }

    private void initDesignPaxSheet() {
        if (designPaxSheet == null) {
            return;
        }
        for (Node node : designPaxSheet.getChildren()) {
            if (!(node instanceof Label label)) {
                continue;
            }
            int row = gridIndex(GridPane.getRowIndex(node));
            int col = gridIndex(GridPane.getColumnIndex(node));
            if (row >= 1 && row <= PrsTdrc.MAX_PASSENGERS && col >= 0 && col < 4) {
                designPaxCells[row - 1][col] = label;
            }
        }
    }

    private static int gridIndex(Integer index) {
        return index == null ? 0 : index;
    }

    private void fillDesignPaxFromRows() {
        clearDesignPax();
        int limit = Math.min(prsPaxRows.size(), PrsTdrc.MAX_PASSENGERS);
        for (int i = 0; i < limit; i++) {
            PaxRow row = prsPaxRows.get(i);
            setDesignPaxCell(i, 0, row.nameProperty().get());
            setDesignPaxCell(i, 1, row.sexProperty().get());
            setDesignPaxCell(i, 2, row.ageProperty().get());
            setDesignPaxCell(i, 3, row.statusProperty().get());
        }
    }

    private void setDesignPaxCell(int row, int col, String value) {
        Label cell = designPaxCells[row][col];
        if (cell != null) {
            String unset = (col == 1 || col == 2) ? UNSET_SHORT : UNSET_TEXT;
            cell.setText(value == null || value.isBlank() ? unset : value);
        }
    }

    private void clearDesignPax() {
        for (int row = 0; row < PrsTdrc.MAX_PASSENGERS; row++) {
            for (int col = 0; col < 4; col++) {
                setDesignPaxCell(row, col, "");
            }
        }
    }

    private static void bindQr(ImageView imageView, StackPane placeholder, WritableImage qrImage) {
        if (imageView == null || placeholder == null) {
            return;
        }
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

    public void clearDisplay() {
        clearAll();
    }

    private void showUtsBoard() {
        hideDesignBoard();
        utsBoard.setVisible(true);
        utsBoard.setManaged(true);
        prsBoard.setVisible(false);
        prsBoard.setManaged(false);
    }

    private void showDesignPrsBoard() {
        showDesignBoard(false);
    }

    private void showDesignUtsBoard() {
        showDesignBoard(true);
    }

    private void showDesignBoard(boolean uts) {
        utsBoard.setVisible(false);
        utsBoard.setManaged(false);
        prsBoard.setVisible(false);
        prsBoard.setManaged(false);
        designBoard.setVisible(true);
        designBoard.setManaged(true);
        setWrapVisible(designPrsWrap, !uts);
        setWrapVisible(designUtsWrap, uts);
        if (passengerFooter != null) {
            passengerFooter.setVisible(false);
            passengerFooter.setManaged(false);
        }
        updateDesignBoxScale();
    }

    private static void setWrapVisible(HBox wrap, boolean visible) {
        if (wrap == null) {
            return;
        }
        wrap.setVisible(visible);
        wrap.setManaged(visible);
    }

    /**
     * Stretch the 40cm × 22cm PRS board so it fills the current screen. Layout stays in cm;
     * only the painted size changes.
     */
    private void bindDesignBoxToScreen() {
        bindBoxToScreen(designPrsBox, designPrsScale);
        bindBoxToScreen(designUtsBox, designUtsScale);
    }

    private void bindBoxToScreen(StackPane box, Scale scale) {
        if (box == null) {
            return;
        }
        box.getTransforms().add(scale);
        box.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null) {
                scene.widthProperty().addListener((o, a, b) -> updateDesignBoxScale());
                scene.heightProperty().addListener((o, a, b) -> updateDesignBoxScale());
                updateDesignBoxScale();
            }
        });
        box.widthProperty().addListener((o, a, b) -> updateDesignBoxScale());
        box.heightProperty().addListener((o, a, b) -> updateDesignBoxScale());
        updateDesignBoxScale();
    }

    private void updateDesignBoxScale() {
        updateBoxScale(designPrsBox, designPrsScale);
        updateBoxScale(designUtsBox, designUtsScale);
    }

    private void updateBoxScale(StackPane box, Scale scale) {
        if (box == null) {
            return;
        }
        Scene scene = box.getScene();
        if (scene == null) {
            return;
        }
        double boxW = box.getWidth();
        double boxH = box.getHeight();
        double sceneW = scene.getWidth();
        double sceneH = scene.getHeight();
        if (boxW <= 0 || boxH <= 0 || sceneW <= 0 || sceneH <= 0) {
            return;
        }
        scale.setX(sceneW / boxW);
        scale.setY(sceneH / boxH);
    }

    private void hideDesignBoard() {
        designBoard.setVisible(false);
        designBoard.setManaged(false);
        if (passengerFooter != null) {
            passengerFooter.setVisible(true);
            passengerFooter.setManaged(true);
        }
    }

    private void clearDesignUtsValues() {
        setUts(designUtsTerminalId, "", UNSET_TEXT);
        setUts(designUtsWindowNo, "", UNSET_TEXT);
        setUts(designUtsFrom, "", UNSET_TEXT);
        setUts(designUtsTo, "", UNSET_TEXT);
        setUts(designUtsDate, "", UNSET_DATE);
        setUts(designUtsAdult, "", UNSET_SHORT);
        setUts(designUtsChild, "", UNSET_SHORT);
        setUts(designUtsClass, "", UNSET_SHORT);
        setUts(designUtsFare, "", UNSET_FARE);
        setUts(designUtsTrainType, "", UNSET_TEXT);
        setUts(designUtsPayMode, "", UNSET_TEXT);
        setUts(designUtsTxnType, "", UNSET_TEXT);
        setUts(designUtsOperatorName, "", "");
        bindQr(designUtsQrImage, designUtsQrPlaceholder, null);
    }

    private static void setUts(Label label, String value, String unset) {
        if (label == null) {
            return;
        }
        label.setText(value == null || value.isBlank() ? unset : value);
    }

    private static String utsDesignDate(TicketData t) {
        String fromParts = designDateFromDayMonth(t.getDay(), t.getMonth());
        if (!fromParts.isBlank()) {
            return fromParts;
        }
        return parsedFullDate(t.getTimestampRaw());
    }

    private static String designDateFromDayMonth(String day, String month) {
        boolean hasDay = day != null && !day.isBlank();
        boolean hasMonth = month != null && !month.isBlank();
        if (!hasDay && !hasMonth) {
            return "";
        }
        String d = hasDay ? padTwo(day) : "-".repeat(2);
        String m = hasMonth ? padTwo(month) : "-".repeat(2);
        String y = hasDay && hasMonth ? String.valueOf(LocalDate.now().getYear()) : "-".repeat(4);
        return d + "/" + m + "/" + y;
    }

    private static String parsedFullDate(String tsRaw) {
        return parseDate(tsRaw).map(DISPLAY_DDMMYYYY::format).orElse("");
    }

    private static String padTwo(String raw) {
        String t = raw.trim();
        if (t.length() == 1) {
            return "0" + t;
        }
        return t.length() > 2 ? t.substring(0, 2) : t;
    }

    private void clearAll() {
        utsTerminalId.setText("—");
        utsWindowNo.setText("—");
        utsFrom.setText("—");
        utsTo.setText("—");
        utsDate.setText("—");
        utsAdult.setText("—");
        utsChild.setText("—");
        utsClass.setText("—");
        utsFare.setText("—");
        utsTrainType.setText("—");
        utsPayMode.setText("—");
        utsTxnType.setText("—");
        utsOperator.setText("—");
        footerLastUpdated.setText("Last updated: —");
        bindQr(utsQrImage, utsQrPlaceholder, null);

        setOperatorCode("");
        setPaired(prsFrom, designFrom, "");
        setPaired(prsTo, designTo, "");
        setPaired(prsTrainNo, designTrainNo, "");
        setPaired(prsQuota, designQuota, "");
        setPaired(prsDate, designDate, "");
        setTotalPax("");
        setTravelClass("");
        setFare("");
        setPaired(prsBoarding, designBoarding, "");
        setPaired(prsResUpto, designResUpto, "");
        setPaired(prsOperatorName, designOperatorName, "", "");
        setPayStatus("");
        prsPaxRows.clear();
        bindPrsQr(null);
        clearDesignValues();
        showDesignUtsBoard();
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

    private static String formatFarePlain(String fare) {
        if (fare == null || fare.isBlank()) {
            return "";
        }
        String trimmed = fare.trim();
        if (trimmed.startsWith("₹")) {
            trimmed = trimmed.substring(1).trim();
        }
        if (trimmed.isBlank() || "—".equals(trimmed)) {
            return "";
        }
        try {
            double v = Double.parseDouble(trimmed);
            return String.format("%.2f", v);
        } catch (NumberFormatException ex) {
            return trimmed;
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
