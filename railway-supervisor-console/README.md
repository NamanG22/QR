# Railway Supervisor Console Simulator

JavaFX desktop app that mimics an Indian Railways PRS/UTS supervisor terminal for a **QR Fare Repeater Display System**. It builds `$<code><Length><Data>^` UTS frames, or PRS `SOH thPRS 02 … ETX` packets, and sends them over **RS232** (9600 8N1, UTF-8) via **jSerialComm**.

**Lab:** USB-to-serial adapter on the console PC — select its COM port in the UI.  
**Production:** direct RS232 from the CRIS terminal to the controller.

If the port is unavailable, the app runs in **mock mode** and logs packets only.

See **`SERIAL_SETUP.md`** in the repo root for wiring and troubleshooting.

## Requirements

- JDK **17**
- Maven **3.8+**
- USB-serial adapter (lab) or RS232 port (production)
- Paired controller app on the thin client (`qfrds-controller-simulator`)

## Run

```bash
cd railway-supervisor-console
mvn clean javafx:run
```

Optional default port:

```bash
QFRDS_SUPERVISOR_PORT=COM5 mvn clean javafx:run
```

## Project layout

| Path | Purpose |
|------|---------|
| `MainApp` | JavaFX entry; loads FXML and CSS |
| `SupervisorController` | Form validation, serial connect, logging, send action |
| `SerialService` | jSerialComm TX: configurable COM port, 9600 8N1, UTF-8 `$...^` frames |
| `TicketData` | Immutable ticket fields |
| `PacketBuilder` | UTS `$<code><Length><Data>^` command sequence; PRS still uses a wrapped field payload |

Styles: `src/main/resources/styles/railway-terminal.css`  
UI: `src/main/resources/fxml/supervisor_console.fxml`
