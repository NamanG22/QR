package com.railway.qfrds;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

/**
 * Launches the passenger-facing repeater display in exclusive fullscreen kiosk mode.
 * Engineering status view stays in memory (not shown) for {@link DisplayController} logging/state.
 * RS232 listener starts automatically via {@link DisplayController#start()}.
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

        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("QFRDS Passenger Display");
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);
        primaryStage.setFullScreenExitKeyCombination(null);

        Scene passengerScene = new Scene(passengerRoot, 1024, 768);
        passengerScene.setFill(Color.WHITE);
        passengerScene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("/styles/passenger_display.css")).toExternalForm());
        primaryStage.setScene(passengerScene);

        passengerScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                e.consume();
            }
        });

        primaryStage.setOnCloseRequest(e -> {
            orchestrator.shutdown();
            Platform.exit();
        });

        primaryStage.setFullScreen(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
