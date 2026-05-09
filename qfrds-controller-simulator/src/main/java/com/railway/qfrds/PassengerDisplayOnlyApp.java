package com.railway.qfrds;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Opens only the passenger-facing RDSO display window — no engineering console, no serial listener.
 * <p>
 * Demo content is seeded so the layout is visible immediately. Use {@code QFRDS_PREVIEW=prs} or
 * argument {@code prs} (via {@link #main}) to preview the PRS board instead of UTS.
 * </p>
 */
public class PassengerDisplayOnlyApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                PassengerDisplayOnlyApp.class.getResource("/fxml/passenger_display.fxml")));
        Parent root = loader.load();
        PassengerDisplayView view = loader.getController();

        boolean prs = wantPrsBoard();
        TicketData demo = prs ? demoPrs() : demoUts();
        view.applyTicketUpdate(demo, null);

        Scene scene = new Scene(root, 1024, 768);
        scene.setFill(Color.WHITE);
        scene.getStylesheets().add(Objects.requireNonNull(
                PassengerDisplayOnlyApp.class.getResource("/styles/passenger_display.css")).toExternalForm());

        stage.setTitle(prs ? "QFRDS Passenger Display (PRS preview)" : "QFRDS Passenger Display (UTS preview)");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(768);
        stage.setMaxWidth(1024);
        stage.setMaxHeight(768);
        stage.setResizable(false);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                e.consume();
            }
        });
        stage.setOnCloseRequest(e -> Platform.exit());
        stage.show();
    }

    private static boolean wantPrsBoard() {
        String env = System.getenv("QFRDS_PREVIEW");
        if (env != null && "prs".equalsIgnoreCase(env.trim())) {
            return true;
        }
        return Boolean.getBoolean("qfrds.preview.prs");
    }

    private static TicketData demoUts() {
        return new TicketData(
                TicketType.UTS,
                "NDLS",
                "MAS",
                "120",
                "DEMO-UTS-TXN",
                "2026-07-14T10:00:00",
                Optional.empty()
        );
    }

    private static TicketData demoPrs() {
        return new TicketData(
                TicketType.PRS,
                "NDLS",
                "MAS",
                "4680",
                "DEMO-PRS-TXN",
                "2026-07-14T10:00:00",
                Optional.of("Ravi")
        );
    }

    /**
     * Pass {@code prs} as the first argument to show the PRS layout; otherwise UTS.
     */
    public static void main(String[] args) {
        if (args != null && args.length > 0 && "prs".equalsIgnoreCase(args[0].trim())) {
            System.setProperty("qfrds.preview.prs", "true");
        }
        launch(args);
    }
}
