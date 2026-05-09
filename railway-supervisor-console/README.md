# Railway Supervisor Console Simulator

JavaFX desktop demo that mimics an Indian Railways PRS/UTS supervisor terminal for a **QR Fare Repeater Display System** proof-of-concept. It builds pipe-delimited ticket packets and sends them over **serial COM5** (9600 8N1, UTF-8 text + newline) via **jSerialComm**. If COM5 is unavailable, the app runs in **mock mode** and logs packets only.

## Requirements

- JDK **17**
- Maven **3.8+**
- Windows with **COM5** (or adjust `SerialService.DEFAULT_PORT_NAME` for your OS/port)

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
| `SerialService` | jSerialComm: COM5, 9600 8N1, UTF-8 + LF |
| `TicketData` | Immutable ticket fields |
| `PacketBuilder` | `TYPE|SRC|DST|FARE|TXN|TS` (+ `PNAME` for PRS) |

Styles: `src/main/resources/styles/railway-terminal.css`  
UI: `src/main/resources/fxml/supervisor_console.fxml`
