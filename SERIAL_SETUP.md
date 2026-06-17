# RS232 link — Supervisor console → Controller

Production uses **RS232 → RS232** between the CRIS terminal and the QFRDS controller.  
For lab/demo on two PCs, use **USB-serial on the console PC** wired to **RS232 on the thin client**.

The wireless TCP path is preserved on branch **`backup/wireless-tcp-20260617`** (see `NETWORK_SETUP.md`).

## Wiring

| Side | Hardware | Software |
|------|----------|----------|
| **Supervisor console** (Windows PC) | USB-to-RS232 adapter | Select its COM port in the console UI (e.g. `COM5`) |
| **Controller** (thin client) | RS232 input (or USB-serial if no native port) | Listens on `COM10` by default |

Connect TX/RX/GND per your cable (null-modem or straight-through). Both ends: **9600 baud, 8N1, UTF-8 text + newline**.

## 1. Find COM ports

**Console PC** — Device Manager → Ports (COM & LPT) → note the USB-serial port (e.g. `COM5`).

**Thin client** — PowerShell:

```powershell
mode
```

Or check Device Manager for the RS232 / USB-serial port (default listener: `COM10`).

## 2. Start the controller (thin client)

```powershell
cd qfrds-controller-simulator
mvn clean javafx:run
```

Optional — if RS232 is not on `COM10`:

```powershell
$env:QFRDS_CONTROLLER_PORT = "COM3"
mvn clean javafx:run
```

Engineering log should show:

```text
RS232 listener active on COM10 @ 9600 8N1 UTF-8.
```

Status badge: **LIVE** when the port is open.

## 3. Start the supervisor (console PC)

```powershell
cd railway-supervisor-console
mvn clean javafx:run
```

1. Enter the **USB-serial COM port** from step 1 (e.g. `COM5`)
2. Click **Connect**
3. Log should say: `Connected to COM5 (... 9600 8N1 ...)`
4. Fill ticket fields → **Generate Ticket**

The thin client passenger display should update.

## Same-PC test (optional)

Use **com0com** to create a virtual pair (e.g. `COM10` ↔ `COM11`):

- Controller: `QFRDS_CONTROLLER_PORT=COM10`
- Console: serial port `COM11`

No physical cable required.

## Environment variables

| Variable | App | Purpose |
|----------|-----|---------|
| `QFRDS_SUPERVISOR_PORT` | Console | Default COM port in the UI |
| `QFRDS_CONTROLLER_PORT` | Controller | RS232 listen port (default `COM10`) |
| `QFRDS_PAIR_PORT` | Controller | com0com partner port (same-PC test only) |

## Troubleshooting

| Symptom | Fix |
|--------|-----|
| Console: `COMx not found` | USB adapter not plugged in; check Device Manager |
| Console: `could not open COMx` | Port in use by another app; close PuTTY/HyperTerminal |
| Controller MOCK, no packets | Wrong `QFRDS_CONTROLLER_PORT`; cable not seated |
| Garbled display | Baud mismatch — both sides must be 9600 8N1 |
| Need wireless demo again | `git checkout backup/wireless-tcp-20260617` |

## Packet format

Newline-terminated UTF-8:

```text
TYPE=UTS|SRC=NDLS|DST=AGC|FARE=120|TXN=TX123|TS=2026-05-09 12:30:00
TYPE=PRS|SRC=NDLS|DST=MUM|FARE=2450|TXN=TX555|TS=2026-05-09 12:30:00|PNAME=Rahul
```
