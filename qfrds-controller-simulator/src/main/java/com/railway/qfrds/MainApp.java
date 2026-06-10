package com.railway.qfrds;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

/**
 * Launches the passenger-facing repeater display in exclusive fullscreen kiosk mode.
 * Engineering status view stays in memory (not shown) for {@link DisplayController} logging/state.
 * Hidden operator exit: {@code Ctrl+Shift+Q}. All other close paths are blocked.
 */
public class MainApp extends Application {

    private DisplayController orchestrator;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader statusLoader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource("/fxml/controller_status.fxml")));

        Font.loadFont(
            getClass().getResourceAsStream("/fonts/PlayfairDisplay-Black.ttf"),
            14
        );

        statusLoader.load();
        ControllerStatusView statusView = statusLoader.getController();

        FXMLLoader passengerLoader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource("/fxml/passenger_display.fxml")));
        Parent passengerRoot = passengerLoader.load();
        PassengerDisplayView passengerView = passengerLoader.getController();

        orchestrator = new DisplayController(statusView, passengerView);
        orchestrator.start();

        Rectangle2D screen = Screen.getPrimary().getBounds();

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("QFRDS Passenger Display");
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setX(screen.getMinX());
        primaryStage.setY(screen.getMinY());
        primaryStage.setWidth(screen.getWidth());
        primaryStage.setHeight(screen.getHeight());

        Scene passengerScene = new Scene(passengerRoot, screen.getWidth(), screen.getHeight());
        passengerScene.setFill(Color.WHITE);
        passengerScene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("/styles/passenger_display.css")).toExternalForm());
        primaryStage.setScene(passengerScene);

        // Borderless screen-sized stage is kiosk-safe on Windows (ESC does not shrink it).
        installKioskGuards(primaryStage, passengerScene);

        primaryStage.show();
    }

    /**
     * Kiosk guard: only {@code Ctrl+Shift+Q} shuts down. Blocks ESC, Alt+F4, window close, etc.
     */
    private void installKioskGuards(Stage stage, Scene scene) {
        stage.setOnCloseRequest(e -> e.consume());

        EventHandler<KeyEvent> blockExitShortcuts = e -> {
            if (isHiddenExit(e)) {
                return;
            }
            if (isBlockedExitShortcut(e)) {
                e.consume();
            }
        };

        EventHandler<KeyEvent> hiddenExit = e -> {
            if (isHiddenExit(e)) {
                e.consume();
                shutdownAndExit();
            }
        };

        scene.addEventFilter(KeyEvent.KEY_PRESSED, hiddenExit);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, blockExitShortcuts);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, blockExitShortcuts);
        stage.addEventFilter(KeyEvent.KEY_PRESSED, hiddenExit);
        stage.addEventFilter(KeyEvent.KEY_PRESSED, blockExitShortcuts);
        stage.addEventFilter(KeyEvent.KEY_RELEASED, blockExitShortcuts);
    }

    private static boolean isHiddenExit(KeyEvent e) {
        return e.getEventType() == KeyEvent.KEY_PRESSED
                && e.getCode() == KeyCode.Q
                && e.isControlDown()
                && e.isShiftDown()
                && !e.isAltDown()
                && !e.isMetaDown();
    }

    private static boolean isBlockedExitShortcut(KeyEvent e) {
        KeyCode code = e.getCode();
        if (code == KeyCode.ESCAPE) {
            return true;
        }
        if (code == KeyCode.F4 && e.isAltDown()) {
            return true;
        }
        if (code == KeyCode.Q && (e.isControlDown() || e.isMetaDown()) && !e.isShiftDown()) {
            return true;
        }
        if (code == KeyCode.W && e.isControlDown()) {
            return true;
        }
        return code == KeyCode.F4 && e.isControlDown();
    }

    private void shutdownAndExit() {
        orchestrator.shutdown();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
