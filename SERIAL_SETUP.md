# RS232 link — Supervisor console → Controller

Production uses **RS232 → RS232** between the CRIS terminal and the QFRDS controller.  
For lab work on two PCs, use **USB-serial on the console PC** wired to **RS232 on the thin client**.

## Wiring

| Side | Hardware | Software |
|------|----------|----------|
| **Supervisor console** (Windows PC) | USB-to-RS232 adapter | Select its COM port in the console UI (e.g. `COM5`) |
| **Controller** (thin client) | RS232 or USB-serial adapter | Auto-listens on **all** COM ports (or set `QFRDS_CONTROLLER_PORT`) |

Connect TX/RX/GND per your cable (null-modem or straight-through). Both ends: **9600 baud, 8N1**, UTF-8 command frames `$<code><Length><Data>^`.

## 1. Find COM ports

**Console PC** — Device Manager → Ports (COM & LPT) → note the USB-serial port (e.g. `COM5`).

**Thin client** — PowerShell:

```powershell
mode
```

Or check Device Manager for the RS232 / USB-serial port.

**If Device Manager shows no COM ports on the thin client**, Windows cannot read the serial wire — add a **USB-serial adapter on the thin client** (cable: console USB TX → RS232 wire → thin-client USB RX).

## 2. Start the controller (thin client)

```powershell
cd qfrds-controller-simulator
mvn clean javafx:run
```

Default: listens on **every COM port** Windows reports. Footer should show `LIVE · listening on COM1, COM3` (your ports).

Optional — pin one port in production:

```powershell
$env:QFRDS_CONTROLLER_PORT = "COM3"
mvn clean javafx:run
```

Footer should show **`LIVE`** (not `WAITING`).

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
| `QFRDS_CONTROLLER_PORT` | Controller | Pin one listen port; if unset, listens on **all** COM ports |

## Troubleshooting

| Symptom | Fix |
|--------|-----|
| Console: `COMx not found` | USB adapter not plugged in; check Device Manager |
| Console: `could not open COMx` | Port in use by another app; close PuTTY/HyperTerminal |
| Console connected, wire TX blinks, **display unchanged** | Controller is on the **wrong COM port** — check footer on passenger screen (`WAITING` = not listening). On thin client: Device Manager → Ports → set `QFRDS_CONTROLLER_PORT` to the RS232/USB port the cable is plugged into |
| Footer shows `rx=0` after Generate | TX/RX swapped on cable, or controller COM wrong — try null-modem adapter or swap TX/RX wires |
| Footer shows `rx=1+` but no ticket | Parse error — open `%LOCALAPPDATA%\QFRDS\serial.log` on thin client |
| Footer `WAITING · no COM in Device Manager` | Thin client has **zero** COM ports — plug USB-serial adapter on thin client |
| Footer `WAITING · cannot open any port` | Ports exist but busy — close other apps using COM ports |
| Garbled display | Baud mismatch — both sides must be 9600 8N1 |

### Diagnose on the thin client

Passenger display footer shows: `RS232 COMx · LIVE · rx=N · …`

- **`WAITING`** — app could not open the COM port; set the correct port:
  ```powershell
  $env:QFRDS_CONTROLLER_PORT = "COM3"   # use your thin-client port from Device Manager
  mvn clean javafx:run
  ```
- **`LIVE` but `rx=0`** after Generate on console — cable/wiring issue (data not reaching the thin client COM port).
- **`rx=1+`** — bytes received; display should update. If not, check `serial.log`.

Log file: `%LOCALAPPDATA%\QFRDS\serial.log` (lists available ports at startup and each received line).

## Packet format

Production UTS frame (no trailing newline): `$` + 2-digit code + Length + Data + `^`.
There is **no** colon between code, length, and data. Colon separates fields **inside Data**, and Data ends with `:`. `Length` is the UTF-8 byte length of `Data^` (Data plus EOT).

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

| Code | Meaning | Notes |
|------|---------|--------|
| 00 | Thin Client UTS | `$007thUts:^` |
| 01 / 02 / 17–20 | Station | `code:english:hindi` — variable length, not space-padded. 17–20 reserved |
| 03 | Date (day) | `$03406:^` (data `06:`) |
| 07 | Type of Train | `$073E:^`. Display: O=ORD, E=M/E, S=SUP, T=MMT, C=COM, R=RAJ, D=SHT, M=RMT, H=DHI, J=JAN, P=PRM |
| 12 | Transaction Type | `$1206PLAT:^`. Unknown codes display **INVALID** |
| 13 | Clear display | `$1303:^` — clears every field on the board |
| 09 | Class | `I` or `II` |
| 14 | Cancellation Refund | `CANC` + `RFND` + 5-digit amount |
| 15 | Operator details | `$1534MUKESH KUMAR GARHWAL:NDLS99:99:3:^` (`name:terminal:window:shift:`) |
| 21 | Payment gateway | `$2122SBI PAYMENT GATE WAY:^` |
| 22 | QR Code for payment | UPI string, **no** trailing colon; length is three digits (e.g. `201`) |

Transaction codes: SPLC, PLAT, NI, CANC, ST, BPT, SF, JRNY, CARD, MMQT, RRTT, PART.

Refund amount `790` is padded to `00790`.

Code 22 is sent when the supervisor QR field is filled. QR Data must not contain SOT `$` or EOT `^` (colons in `upi://` are allowed).

Cancellation refund (amount 790): `$1415CANCRFND00790:^`

## PRS packet format

PRS uses a different envelope (control characters, not `$...^`):

```text
SOH (ASCII 1) + thPRS + 02 + <sub> + Q + <3-digit length> + STX (ASCII 2) + body + ETX (ASCII 3)
```

| Sub | Meaning |
|------|---------|
| 110 | Connectivity ping — empty body, length blank. Device replies with `Q` changed to `S` |
| 111 | TDRC + passengers. Inner fields `$01:` train … `$18:` seat/status (`C2 - 43`). Extra passengers `$19:`–`$38:` |
| 112 | QR string, then `$`, then display message |
| 113 | Payment success text |
| 114 | Payment failure text |

New TDRC (111) clears the board first, then fills the PRS board. Select **PRS** on the supervisor, then **Generate Ticket**.


