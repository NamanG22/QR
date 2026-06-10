# QFRDS Controller Simulator

JavaFX application that simulates the **Remote Data Acquisition Intelligent Controller** for an Indian Railways **QFRDS** (QR Fare Repeater Display System) demo: listens on **COM11** (paired with supervisor **COM10** via com0com) for UTF‑8 ticket lines from the [Railway Supervisor Console Simulator](../railway-supervisor-console/), parses packets, builds payment QR codes (**ZXing**), and drives **two windows** — engineering status and passenger-facing display.

## Requirements

- JDK **17**
- Maven **3.8+**
- Windows: **COM11** virtual pair to supervisor **COM10** (adjust `SerialListenerService.DEFAULT_PORT_NAME` if your com0com pair uses other numbers; COM3/COM4 when those ports are free)

## Run (development)

```bash
cd qfrds-controller-simulator
mvn clean javafx:run
```

Launches the passenger display in kiosk fullscreen (engineering status runs headless in memory). Hidden operator exit: **Ctrl+Shift+Q**.

## Windows standalone deployment (jpackage)

Build a portable **QFRDS.exe** with bundled **JRE 17** and **JavaFX 21** on **Windows 10** (must use a Windows machine with **JDK 17+** — jpackage is platform-specific):

```bat
cd qfrds-controller-simulator
build-windows.bat
```

Or manually:

```bat
mvn clean package -Pwindows-jpackage -DskipTests
```

**Output:** `target\dist\QFRDS\QFRDS.exe` (plus runtime files in the same folder — copy the whole `QFRDS` directory to the target PC).

### What the build does

1. **`javafx:jlink`** — custom runtime image at `target\qfrds-runtime` (JRE + JavaFX + app modules)
2. **`jpackage:jpackage`** — wraps that runtime into `target\dist\QFRDS\` with launcher **QFRDS.exe**

Entry point: `com.railway.qfrds.MainApp`

### Requirements on the build machine

- Windows 10
- JDK **17** or **21** full JDK (not JRE-only) with `jpackage` on `PATH`
- WiX Toolset **3.x** optional (only needed for `.msi` / single-file `.exe` *installers*; `APP_IMAGE` does not require WiX)

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

## Packaging

| Command | Output |
|---------|--------|
| `mvn javafx:run` | Dev launch (module path) |
| `mvn package -Pwindows-jpackage` | Standalone `target\dist\QFRDS\QFRDS.exe` (Windows build machine only) |
