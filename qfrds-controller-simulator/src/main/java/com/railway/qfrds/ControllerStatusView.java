package com.railway.qfrds;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Engineering dashboard for the simulated intelligent controller: serial health, parsing trace,
 * QR pipeline status, and animated status LEDs (bonus).
 */
public class ControllerStatusView implements Initializable {

    @FXML
    private Label serialConnectionLabel;
    @FXML
    private Label lastPacketLabel;
    @FXML
    private TextArea parseLogArea;
    @FXML
    private Label ticketTypeLabel;
    @FXML
    private Label qrStatusLabel;
    @FXML
    private Label reconnectLabel;
    @FXML
    private Label mockModeLabel;
    @FXML
    private Label heartbeatLedLabel;
    @FXML
    private Circle ledSerial;
    @FXML
    private Circle ledQr;
    @FXML
    private Circle ledDisplay;
    @FXML
    private Circle ledError;

    private volatile long lastActivityNanos;
    private Timeline heartbeatTimeline;
    private Timeline ledPulseTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lastActivityNanos = System.nanoTime();
        appendLog(LogFormatter.ts("Controller status UI initialized."));

        heartbeatTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> updateHeartbeatIndicator()));
        heartbeatTimeline.setCycleCount(Animation.INDEFINITE);
        heartbeatTimeline.play();

        ledPulseTimeline = new Timeline(new KeyFrame(Duration.millis(600), e -> decayLeds()));
        ledPulseTimeline.setCycleCount(Animation.INDEFINITE);
        ledPulseTimeline.play();

        setSerialConnectionSummary("Starting…");
        setQrGenerationStatus("IDLE");
        setDetectedTicketType("—");
        setReconnectCount(0);
        setMockMode(true);
    }

    /**
     * Call when bytes / lines are processed so the COM heartbeat widget stays green.
     */
    public void pulseSerialActivity() {
        lastActivityNanos = System.nanoTime();
        flashLed(ledSerial, 1.0);
        ledSerial.getStyleClass().removeAll("led-amber", "led-red");
        ledSerial.getStyleClass().add("led-green");
    }

    public void pulseQrOkLed() {
        flashLed(ledQr, 1.0);
        ledQr.getStyleClass().removeAll("led-amber", "led-red");
        ledQr.getStyleClass().add("led-green");
    }

    public void pulseQrWarningLed() {
        ledQr.setOpacity(0.85);
        ledQr.getStyleClass().removeAll("led-amber", "led-green", "led-red");
        ledQr.getStyleClass().add("led-amber");
    }

    public void pulseDisplayPipelineLed() {
        flashLed(ledDisplay, 1.0);
        ledDisplay.getStyleClass().removeAll("led-amber", "led-red");
        ledDisplay.getStyleClass().add("led-green");
    }

    public void pulseErrorLed() {
        flashLed(ledError, 1.0);
        ledError.getStyleClass().removeAll("led-green", "led-amber");
        ledError.getStyleClass().add("led-red");
    }

    private void flashLed(Circle c, double opacity) {
        if (c == null) {
            return;
        }
        c.setOpacity(opacity);
    }

    private void decayLeds() {
        fade(ledSerial, 0.35);
        fade(ledQr, 0.35);
        fade(ledDisplay, 0.35);
        fade(ledError, 0.25);
    }

    private static void fade(Circle c, double floor) {
        if (c == null) {
            return;
        }
        double o = c.getOpacity();
        if (o > floor) {
            c.setOpacity(Math.max(floor, o - 0.08));
        }
    }

    private void updateHeartbeatIndicator() {
        long agoMs = (System.nanoTime() - lastActivityNanos) / 1_000_000L;
        if (heartbeatLedLabel == null) {
            return;
        }
        if (agoMs < 2_500) {
            heartbeatLedLabel.setText("● LINK");
            heartbeatLedLabel.getStyleClass().removeAll("hb-stale", "hb-ok");
            heartbeatLedLabel.getStyleClass().add("hb-ok");
        } else {
            heartbeatLedLabel.setText("○ IDLE");
            heartbeatLedLabel.getStyleClass().removeAll("hb-ok", "hb-stale");
            heartbeatLedLabel.getStyleClass().add("hb-stale");
        }
    }

    public void setSerialConnectionSummary(String summary) {
        if (serialConnectionLabel != null) {
            serialConnectionLabel.setText(summary);
        }
    }

    public void setLastPacketPreview(String packet) {
        if (lastPacketLabel != null) {
            lastPacketLabel.setText(packet == null || packet.isEmpty() ? "—" : packet);
        }
    }

    public void appendLog(String line) {
        if (parseLogArea != null) {
            parseLogArea.appendText(line + "\n");
            parseLogArea.positionCaret(parseLogArea.getText().length());
        }
    }

    public void setDetectedTicketType(String type) {
        if (ticketTypeLabel != null) {
            ticketTypeLabel.setText(type);
        }
    }

    public void setQrGenerationStatus(String status) {
        if (qrStatusLabel != null) {
            qrStatusLabel.setText(status);
        }
    }

    public void setReconnectCount(int n) {
        if (reconnectLabel != null) {
            reconnectLabel.setText(String.valueOf(n));
        }
    }

    public void setMockMode(boolean mock) {
        if (mockModeLabel != null) {
            mockModeLabel.setText(mock ? ("MOCK (no " + SerialPortConfig.portName() + ")") : "LIVE");
            mockModeLabel.getStyleClass().removeAll("mock-on", "mock-off");
            mockModeLabel.getStyleClass().add(mock ? "mock-on" : "mock-off");
        }
        setSerialConnectionSummary(mock
                ? "DISCONNECTED — retrying " + SerialPortConfig.portName()
                : "CONNECTED — " + SerialPortConfig.portName() + " 9600 8N1");
    }
}
