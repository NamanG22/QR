package com.railway.qfrds;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Launches two windows: engineering controller status and passenger-facing repeater display.
 * RS232 listener starts automatically via {@link DisplayController#start()}.
 */
public class MainApp extends Application {

    private DisplayController orchestrator;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader statusLoader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource("/fxml/controller_status.fxml")));
        Parent statusRoot = statusLoader.load();
        ControllerStatusView statusView = statusLoader.getController();

        FXMLLoader passengerLoader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource("/fxml/passenger_display.fxml")));
        Parent passengerRoot = passengerLoader.load();
        PassengerDisplayView passengerView = passengerLoader.getController();

        orchestrator = new DisplayController(statusView, passengerView);
        orchestrator.start();

        Scene statusScene = new Scene(statusRoot, 920, 740);
        statusScene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("/styles/industrial_dashboard.css")).toExternalForm());

        primaryStage.setTitle("QFRDS Controller Simulator — Engineering");
        primaryStage.setScene(statusScene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(640);
        primaryStage.setOnCloseRequest(e -> {
            orchestrator.shutdown();
            Platform.exit();
        });
        primaryStage.show();

        Stage passengerStage = new Stage();
        Scene passengerScene = new Scene(passengerRoot, 1024, 768);
        passengerScene.getStylesheets().add(Objects.requireNonNull(
                MainApp.class.getResource("/styles/passenger_display.css")).toExternalForm());
        passengerStage.setTitle("QFRDS Passenger Display");
        passengerStage.setScene(passengerScene);
        passengerStage.setMinWidth(1024);
        passengerStage.setMinHeight(768);
        passengerStage.setMaxWidth(1024);
        passengerStage.setMaxHeight(768);
        passengerStage.setResizable(false);
        passengerScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F11) {
                passengerStage.setFullScreen(!passengerStage.isFullScreen());
                e.consume();
            }
        });
        passengerStage.show();

        primaryStage.setX(80);
        primaryStage.setY(60);
        passengerStage.setX(primaryStage.getX() + primaryStage.getWidth() + 20);
        passengerStage.setY(primaryStage.getY());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
