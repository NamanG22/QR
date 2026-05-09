package com.railway.supervisor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX entry point for the Railway Supervisor Console Simulator.
 * Loads FXML, applies terminal-themed CSS, and shows the primary stage.
 */
public class MainApp extends Application {

    private static final double MIN_WIDTH = 720;
    private static final double MIN_HEIGHT = 640;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                MainApp.class.getResource("/fxml/supervisor_console.fxml")));
        Parent root = loader.load();
        SupervisorController controller = loader.getController();

        Scene scene = new Scene(root);
        String css = Objects.requireNonNull(
                MainApp.class.getResource("/styles/railway-terminal.css")).toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Railway Supervisor Console Simulator");
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
