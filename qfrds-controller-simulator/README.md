# QFRDS Controller Simulator

JavaFX application that runs the **Remote Data Acquisition Intelligent Controller** for an Indian Railways **QFRDS** (QR Fare Repeater Display System): listens on **RS232** (default **COM1**) for UTF‑8 ticket lines from the [Railway Supervisor Console](../railway-supervisor-console/), parses packets, builds payment QR codes (**ZXing**), and drives the passenger-facing display.

**Production:** RS232 from CRIS terminal. **Lab:** USB-serial on the console PC wired to controller RS232.

See **`SERIAL_SETUP.md`** in the repo root for wiring and port configuration.

## Requirements

- JDK **17**
- Maven **3.8+**
- RS232 port on the thin client (default `COM10`; override with `QFRDS_CONTROLLER_PORT`)

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
mvn clean verify -Pwindows-jpackage -DskipTests
```

**Output:** `target\dist\QFRDS\QFRDS.exe` (plus runtime files in the same folder — copy the whole `QFRDS` directory to the target PC).

### What the build does

1. **Stage JavaFX Windows jars** from Maven Central into `target\javafx-jmods\`
2. **`jlink`** — builds `target\qfrds-runtime\` (JDK 17 + JavaFX modules)
3. **Stage app jars** — `target\jpackage-input\` (app + ZXing + jSerialComm)
4. **`jpackage`** — wraps runtime + app into `target\dist\QFRDS\QFRDS.exe`

No external download — JavaFX comes from Maven Central (`org.openjfx:*:win` jars, which `jlink` accepts).

Entry point: `com.railway.qfrds.Launcher` (starts `MainApp`).

If **QFRDS.exe** exits immediately, open `%LOCALAPPDATA%\QFRDS\startup.log` for the error. Rebuild with a visible console:

```bat
mvn clean verify -Pwindows-jpackage -DskipTests -Djpackage.win.console=true
```

### Requirements on the build machine

- Windows 10
- JDK **17** or **21** full JDK (not JRE-only) with `jpackage` on `PATH`
- WiX Toolset **3.x** optional (only needed for `.msi` / single-file `.exe` *installers*; `APP_IMAGE` does not require WiX)

## Wiring with the Supervisor simulator

| Supervisor (TX) | Controller (RX) |
|-----------------|-----------------|
| USB-serial COM port in UI (lab) or CRIS RS232 (production) | **COM10** (default) |

Physical null-modem or straight-through cable between console TX and controller RX. Same-PC test: use **com0com** pair — see `SERIAL_SETUP.md`.

Packet format (`$<code><Length><Data>^`, UTF‑8). Colon is a field separator **inside Data**; Data ends with `:`; Length counts `Data^`.

```text
$007thUts:^
$0126NDLS:NEW DELHI:NEW DELHI:^
$03406:^
$073E:^
$1206PLAT:^
$1303:^
$1534MUKESH KUMAR GARHWAL:NDLS99:99:3:^
$2122SBI PAYMENT GATE WAY:^
```

Train type display: O=ORD, E=M/E, S=SUP, T=MMT, C=COM, R=RAJ, D=SHT, M=RMT, H=DHI, J=JAN, P=PRM. Unknown transaction codes show **INVALID**. Codes **17–20** are reserved. Clear (`$1303:^`) blanks every field. Refund: `$1415CANCRFND00790:^`. QR (code 22) has a 3-digit length and no trailing colon.

QR payload embedded in the QR matrix:

```text
TXN=<txn>|FARE=<fare>|SRC=<src>|DST=<dst>|TS=<timestamp>[|PNAME=<name>]
```

## Behaviour

- Listener **starts automatically** on launch.
- If the RS232 port is unavailable, the app stays in **mock listening mode** (logs reconnect attempts; UI works).
- **Auto-reconnect** after disconnect or read failure.

## Project layout

| Class | Role |
|-------|------|
| `MainApp` | Dual-stage launcher |
| `MultiSerialListenerService` | RS232 listener (default COM1), 9600 8N1, line reader, reconnect |
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
| `mvn verify -Pwindows-jpackage` | Standalone `target\dist\QFRDS\QFRDS.exe` (Windows build machine only) |
