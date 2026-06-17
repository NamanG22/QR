# Windows → Thin Client (Network Demo) — archived

> **Current `main` uses RS232/USB serial.** See **`SERIAL_SETUP.md`** for the active wiring guide.
>
> This document describes the **wireless TCP** demo. That code is preserved on branch **`backup/wireless-tcp-20260617`**.

Use this when the **supervisor console runs on your Windows PC** and the **controller + passenger display runs on the HP thin client**. No com0com, no env vars.

## 1. Connect both machines to the same network

- Plug both into the same router/switch, **or**
- Connect PC ↔ thin client with Ethernet (may need static IPs).

## 2. Find the thin client IP address

On the **thin client**, open PowerShell:

```powershell
ipconfig
```

Note the **IPv4 Address**, e.g. `192.168.1.50`.

From **Windows**, verify reachability:

```powershell
ping 192.168.1.50
```

## 3. Allow port 9000 on the thin client (Windows Firewall)

On the **thin client** (Run as administrator):

```powershell
New-NetFirewallRule -DisplayName "QFRDS Controller TCP" -Direction Inbound -Protocol TCP -LocalPort 9000 -Action Allow
```

Or: Windows Security → Firewall → Allow an app → allow **Java** on Private networks.

## 4. Start the controller on the thin client

```powershell
cd qfrds-controller-simulator
mvn clean javafx:run
```

Engineering log must show:

```text
TCP listener active on 0.0.0.0:9000
```

Status badge: **LIVE** (not MOCK). No env vars needed.

Press **F11** on the passenger display for fullscreen.

## 5. Start the supervisor on Windows

```powershell
cd railway-supervisor-console
mvn clean javafx:run
```

In the **Controller link** section at the top:

1. Enter the **thin client IP** (e.g. `192.168.1.50`)
2. Port **9000** (default)
3. Click **Connect**
4. Log should say: `Connected to controller at 192.168.1.50:9000`
5. Fill ticket fields → **Generate Ticket**

The thin client passenger display should update.

## Troubleshooting

| Symptom | Fix |
|--------|-----|
| Console: `Connection refused` | Controller not running on thin client, or wrong IP |
| Console: `timed out` | Firewall blocking 9000, or not on same network |
| Controller MOCK, no TCP line | Port 9000 in use; close other Java apps |
| `ping` fails | Network cabling / Wi‑Fi / VLAN — fix before debugging Java |

## Same PC test (optional)

Set controller IP to `127.0.0.1` on the supervisor console.

## Production note

Real deployment uses **RS232** between CRIS and controller. This TCP path is only for **lab / mock** when two PCs replace serial.
