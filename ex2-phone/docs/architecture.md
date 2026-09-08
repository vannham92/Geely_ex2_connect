# Architecture

Single-Activity Jetpack Compose app, MVVM, one TCP NDJSON socket to the car.

## Layers

```
┌─────────────────────────────────────────────────────────────┐
│  UI (Compose)                                                │
│  MainApp → HomeScreen / ControlScreen / SettingsScreen       │
│  - collectAsState() on ViewModel StateFlows                  │
│  - calls viewModel.setX(...) for actions                    │
└───────────────▲───────────────────────────┬─────────────────┘
                │ StateFlow (state down)     │ method call (event up)
┌───────────────┴───────────────────────────▼─────────────────┐
│  CarViewModel (androidx.lifecycle.ViewModel)                 │
│  - stable StateFlows: carStatus, connected, connectionError, │
│    transportMode, btDevice, activeTransport(+Label)         │
│  - UI-only logic: optimistic updates, window latch, snackbar│
│  - one method per command, each a fire-and-forget launch{}  │
└───────────────▲───────────────────────────┬─────────────────┘
                │ StateFlow                  │ method call
┌───────────────┴───────────────────────────▼─────────────────┐
│  CarConnectionManager (object) — retain(UI) / retain(SERVICE)│
│  - buildLinks(): BT first, then WiFi (mode-dependent)       │
│  - reconnect loop, alive while ≥1 owner holds it            │
│  - CarConnectionService retains it so the link survives the │
│    app being closed (started by CarBluetoothReceiver)       │
└───────────────▲───────────────────────────┬─────────────────┘
                │ StateFlow                  │ suspend call
┌───────────────┴───────────────────────────▼─────────────────┐
│  CarRepository (data/network) — ONE instance per process    │
│  - transport-agnostic: setLink(CarLink) + connect()         │
│  - reads \n-lines, updates flows; idle watchdog 35s         │
│  - sendCommand(): serializes + writes one NDJSON line       │
│  - DTOs: CarStatusMessage / CarCommandMessage / …Response   │
└───────────────▲───────────────────────────┬─────────────────┘
                │ CarStreams (in/out)         ▼
        ┌───────┴─────────────┐   ┌──────────────────────────┐
        │ BtSppCarLink        │   │ TcpCarLink               │
        │ RFCOMM/SPP, UUID    │   │ TCP <carIp>:47800        │
        └───────┬─────────────┘   └───────────┬──────────────┘
                │      NDJSON (giống hệt)     │
        ┌───────▼─────────────────────────────▼──────────────┐
        │ Head unit: BtSppServerLink / TcpServerLink          │
        │            → CarTcpServer (business logic)          │
        └─────────────────────────────────────────────────────┘
```

## Key rules of the layering

- **Unidirectional data flow.** State flows *down* as `StateFlow`; events flow *up* as method calls.
- **The ViewModel is the only thing the UI knows about.** Composables receive a `CarViewModel`
  plus plain values/lambdas. They never import `CarRepository` or `java.net`/networking types.
- **The `CarLink` owns the socket; the Repository owns the framing.** `TcpCarLink` /
  `BtSppCarLink` only open and close a socket and hand back `CarStreams`. Everything above them
  is transport-agnostic — adding a transport means adding a `CarLink`, nothing else.
- **Exactly ONE `CarRepository` per process**, owned by `CarConnectionManager`. The collectors in
  `CarViewModel.init {}` bind to its flows; creating a second instance would leak them. Switching
  transport = `setLink()` + reconnect.
- **The connection outlives the ViewModel.** `CarViewModel` and `CarConnectionService` each
  `retain()` the manager; the loop runs while at least one owner holds it and stops when the last
  one releases. Opening the app while the service runs therefore reuses the existing socket.
- **Why the ViewModel re-exposes flows** instead of surfacing the repository's flows directly:
  the UI keeps collecting *stable* `carStatus`/`connected` flows across reconnects and transport
  switches.

## Connection lifecycle

1. `MainApp` `LaunchedEffect(Unit)` calls `carViewModel.connect(ipAddress)` on first composition.
2. `connect(ip)` → `CarConnectionManager.setCarIp(ip)`: persists the IP, then restarts the loop
   **only** if the IP changed or nothing is connected — otherwise opening the app would cut a link
   the background service is holding. `setTransportMode()` / `setBtDevice()` always restart.
3. `restart()` cancels the old job, `repository.disconnect()` (closes the socket —
   coroutine cancel alone can't unblock `readLine()`), then loops **forever**:
   - `buildLinks()` — rebuilt every pass, never cached (Bluetooth may have just been enabled):
     `BLUETOOTH` → `BtSppCarLink`, `WIFI` → `TcpCarLink`. There is no auto/fallback mode; the user
     picks one in Settings. The list comes back empty when the link is unusable (no BT permission /
     BT off / no device picked / blank IP), and the loop just retries.
   - the link is held until the stream drops, then sleeps 3s and starts over.
4. `CarRepository` runs an **idle watchdog**: >35s with no line (car pings every 15s) → close the
   link. `BluetoothSocket` has no `soTimeout`, so this is the only thing that catches a silent
   BT drop.
5. `onCleared()` only calls `release(Owner.UI)`. The socket closes only if no service holds it too.

### Running in the background

`CarBluetoothReceiver` (manifest-registered, so it works with the app closed and after a reboot)
watches `ACL_CONNECTED` / `ACL_DISCONNECTED` for the head unit picked in Settings:

- connected → `CarConnectionService.start()` — a `connectedDevice` foreground service that
  `retain(Owner.SERVICE)`s the manager and shows the mandatory ongoing notification (status text
  + a "Dừng" action).
- disconnected → `stop()`, which releases the owner and closes the link.

While the app is open, `CarViewModel` also starts the service once a **Bluetooth** link is up, so
closing the app doesn't drop the connection until the next `ACL_CONNECTED`. WiFi links deliberately
don't trigger this — there's no matching "car left" event to stop the service again.

Both Bluetooth broadcasts require `BLUETOOTH_CONNECT`, which exempts them from the API 26+
implicit-broadcast ban *and* the API 31+ "no starting a foreground service from the background"
restriction. Gate: `TransportPrefs.autoBackground` (Settings → Chạy nền tự động).

## State that lives in the UI (not the ViewModel)

Held in `MainApp` because it is presentation/config, not car telemetry:

- `ipAddress` — seeded from `SharedPreferences("app_prefs")["ip_address"]`, default `10.77.86.7`.
  The ViewModel also reads/writes it via `TransportPrefs` (same key), alongside
  `transport_mode`, `bt_device_address`, `bt_device_name`.
- `carColor` — `CarColor` enum, `rememberSaveable`. Drives the app-wide accent color.
  **Not persisted** across process death (only survives config changes).

## Threading

Everything network-related runs on coroutines in `viewModelScope`. Commands are
"send and forget" — the UI reflects the truth only when the car broadcasts a new `status` line,
so the UI is **server-authoritative**. Don't optimistically mutate local state for a command;
wait for the next status broadcast (except purely local UI like a slider drag preview).
