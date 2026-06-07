# QFRDS Controller Simulator

JavaFX application that simulates the **Remote Data Acquisition Intelligent Controller** for an Indian Railways **QFRDS** (QR Fare Repeater Display System) demo: listens on **COM11** (paired with supervisor **COM10** via com0com) for UTF‑8 ticket lines from the [Railway Supervisor Console Simulator](../railway-supervisor-console/), parses packets, builds payment QR codes (**ZXing**), and drives **two windows** — engineering status and passenger-facing display.

## Requirements

- JDK **17**
- Maven **3.8+**
- Windows: **COM11** virtual pair to supervisor **COM10** (adjust `SerialListenerService.DEFAULT_PORT_NAME` if your com0com pair uses other numbers; COM3/COM4 when those ports are free)

## Run

```bash
cd qfrds-controller-simulator
mvn clean javafx:run
```

Two windows open:

1. **Engineering** — serial/mock status, parse log, LEDs, CRIS (simulated), reconnect counter.
2. **Passenger display** — branding, fare, route, QR, “Scan QR to Pay”, last updated. Press **F11** for fullscreen.

## Wiring with the Supervisor simulator

| Supervisor sends | Controller listens |
|------------------|-------------------|
| **COM10** (default) | **COM11** (default) |

Use **com0com** (or hardware null-modem) so **COM10 ↔ COM11** form one pair; bytes written on one appear on the other.

Packet format (newline-terminated, UTF‑8):

```text
TYPE=UTS|SRC=NDLS|DST=AGC|FARE=120|TXN=TX123|TS=2026-05-09 12:30:00
TYPE=PRS|SRC=NDLS|DST=MUM|FARE=2450|TXN=TX555|TS=2026-05-09 12:30:00|PNAME=Rahul
```

QR payload embedded in the QR matrix:

```text
TXN=<txn>|FARE=<fare>|SRC=<src>|DST=<dst>|TS=<timestamp>[|PNAME=<name>]
```

## Behaviour

- Listener **starts automatically** on launch.
- If **COM11 is unavailable**, the app stays in **mock listening mode** (logs reconnect attempts; UI works).
- **Auto-reconnect** after disconnect or read failure.

## Project layout

| Class | Role |
|-------|------|
| `MainApp` | Dual-stage launcher |
| `SerialListenerService` | RS232 COM11 (default), 9600 8N1, line reader, reconnect |
| `TicketPacketParser` | Pipe-separated field parser; ignores unknown keys |
| `TicketData` | Parsed ticket value object |
| `QRGeneratorService` | ZXing QR bitmap → JavaFX `WritableImage` |
| `DisplayController` | Parse → QR → `Platform.runLater` UI updates |
| `ControllerStatusView` | Engineering dashboard FXML controller |
| `PassengerDisplayView` | Passenger screen FXML controller |

CSS: `styles/industrial_dashboard.css`, `styles/passenger_display.css`.

## Packaging note

Prefer `mvn javafx:run` for JavaFX module classpath. Fat-jar deployment needs explicit JavaFX `--module-path` setup.
