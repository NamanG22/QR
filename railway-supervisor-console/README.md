# Railway Supervisor Console Simulator

JavaFX desktop demo that mimics an Indian Railways PRS/UTS supervisor terminal for a **QR Fare Repeater Display System** proof-of-concept. It builds pipe-delimited ticket packets and sends them over **serial COM10** by default (9600 8N1, UTF-8 text + newline) via **jSerialComm**, paired with the QFRDS controller on **COM11** (com0com). If the port is unavailable, the app runs in **mock mode** and logs packets only.

## Requirements

- JDK **17**
- Maven **3.8+**
- Windows with **COM10** virtual pair to controller **COM11** (or adjust `SerialService.DEFAULT_PORT_NAME`; use COM3/COM4 if those are free on your machine)

## Run

```bash
cd railway-supervisor-console
mvn clean javafx:run
```

Alternative after compile:

```bash
mvn clean compile
mvn javafx:run
```

## Package note

Running from a plain `java -jar` fat JAR requires extra JavaFX module configuration on the classpath/module path. Prefer **`mvn javafx:run`** for development demos.

## Project layout

| Path | Purpose |
|------|---------|
| `MainApp` | JavaFX entry; loads FXML and CSS |
| `SupervisorController` | Form validation, alerts, logging, send action |
| `SerialService` | jSerialComm: COM10 (default), 9600 8N1, UTF-8 + LF |
| `TicketData` | Immutable ticket fields |
| `PacketBuilder` | `TYPE|SRC|DST|FARE|TXN|TS` (+ `PNAME` for PRS) |

Styles: `src/main/resources/styles/railway-terminal.css`  
UI: `src/main/resources/fxml/supervisor_console.fxml`
