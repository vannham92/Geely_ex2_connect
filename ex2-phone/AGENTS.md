# AGENTS.md — ex2-phone (Geely EX2 Remote Control)

> Canonical guide for any AI agent (Claude Code, Cursor, Codex, …) working in this repo.
> Read this first. Deeper references live in [`docs/`](docs/). Human-readable, kept in sync with code.

---

## 1. What this app is

Android app that **remote-controls a Geely EX2 electric car** over the local WiFi network.
The phone app is a **TCP socket client**; the car runs a TCP **server** (`CarTcpServer`). The app shows
live telemetry (speed, battery, range) and sends control commands (driving mode, climate,
AVAS, energy regen, ambient light).

- **Platform:** Android, `minSdk 26`, `targetSdk 35`, Java 17.
- **Language / UI:** Kotlin + Jetpack Compose (Material 3), single-Activity.
- **Networking:** raw TCP `java.net.Socket` (**NDJSON**, port 47800), `kotlinx.serialization` JSON.
- **Architecture:** MVVM — `Repository → ViewModel → Composable`.
- **UI language:** All user-facing strings are **Vietnamese**. Code identifiers are English.

---

## 2. Build, run, test

Requires Android SDK. Env (already set locally, see `.claude/settings.local.json`):

```bash
export ANDROID_HOME=/Users/dangthang/Library/Android/sdk
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew assembleDebug` |
| Install on device/emulator | `./gradlew installDebug` |
| Unit tests | `./gradlew test` |
| Instrumented tests | `./gradlew connectedAndroidTest` |
| Lint | `./gradlew lint` |
| Clean | `./gradlew clean` |

Dependencies are managed in the **version catalog** [`gradle/libs.versions.toml`](gradle/libs.versions.toml).
Never hardcode versions in `build.gradle.kts` — add to the catalog and reference `libs.*`.

---

## 3. Package map (`com.example.ex2_phone`)

```
MainActivity.kt              # ComponentActivity, edge-to-edge, sets Ex2phoneTheme { MainApp() }
data/network/
  CarRepository.kt           # TCP NDJSON socket client + all DTOs + command senders  ← network layer
  CarConnectionManager.kt    # process singleton: repository + reconnect loop + transport prefs
  BtSppPocClient.kt          # POC client RFCOMM — gửi {"ping":1}, so echo (xoá sau khi Phase 2 xong)
service/
  CarConnectionService.kt    # foreground service giữ kết nối khi app đóng (notification thường trực)
  CarBluetoothReceiver.kt    # ACL_CONNECTED/DISCONNECTED của head unit → bật/tắt service
navigation/
  Screen.kt                  # sealed Screen: Home / Driving / Control / Hvac / Settings
ui/
  MainApp.kt                 # Scaffold + NavigationBar (bottom tabs) + NavHost. Holds ipAddress & carColor state.
  CarViewModel.kt            # StateFlows, connect()+auto-reconnect, one method per command
  CarColor.kt                # enum of paint colors → drives global accent theming
  HomeScreen.kt              # telemetry hero, driving-mode + temp + WiFi tiles, status detail list
  DrivingScreen.kt           # thông tin lái xe (speed, gear, range)
  ControlScreen.kt           # driving-mode, regen, AVAS, ambient light, WiFi toggles
  HvacScreen.kt              # điều hoà HVAC
  DashboardScreen.kt         # dashboard tổng hợp
  SettingsScreen.kt          # IP address input + connect, car color picker, POC BT
  components/DashboardComponents.kt  # shared small composables (StatTile, MiniStat, RingGauge, ToggleCard, …)
  components/GlassCard.kt    # reusable glassmorphism card
  components/ConnectCard.kt  # card nhập IP + nút connect
  components/BtSppPocCard.kt # UI POC BT (xoá sau Phase 2)
  theme/                     # Color.kt, Theme.kt, Type.kt (Material3 dark theme)
```

Data flows **one way**: `CarRepository` owns the socket and exposes `StateFlow`s →
`CarViewModel` mirrors them into stable `StateFlow`s → Composables `collectAsState()`.
Commands go the other way: Composable → `viewModel.setX()` → `repository.setX()` → TCP NDJSON line.

---

## 4. The car protocol (summary — full spec in [`docs/phone-companion-protocol.md`](docs/phone-companion-protocol.md))

> **Two transports, identical NDJSON payload** — pick one, the app switches transparently:
> **Bluetooth RFCOMM/SPP** (UUID `6f1e2a00-47b8-4c1e-9d3a-c0ffee000001`, `BtSppCarLink`) and
> **raw TCP `:47800`** (`TcpCarLink`). The user picks exactly one in Settings — there is no auto
> mode. Default is `BLUETOOTH`, because the bond already exists from handsfree pairing, so the user
> never has to enable WiFi on the car. `CarRepository` is transport-agnostic (`setLink(CarLink)`); see
> [`docs/bluetooth-transport-plan.md`](docs/bluetooth-transport-plan.md).
> The app was migrated off the old Ktor WebSocket (`ws://:8080/ws/car`). Ground truth + copy-paste
> Kotlin client live in [`docs/phone-companion-protocol.md`](docs/phone-companion-protocol.md).
> The retired WebSocket protocol is kept for history in
> [`docs/websocket-protocol.md`](docs/websocket-protocol.md).

Current car protocol (`CarTcpServer`):

- **Endpoint:** TCP `<carIp>:47800`, **NDJSON** (1 JSON object per line, `\n`-terminated), UTF-8.
- **Inbound status** (car → app), `{"type":"status", ...}` → `CarStatusMessage`.
- **Inbound response** (car → app), `{"type":"command_response", ...}` → `CarCommandResponse`.
- **Inbound heartbeat** (car → app), `{"type":"ping"}` every 15s — no reply needed.
- **Outbound command** (app → car), `{"command":"...", "modeValue":?, "enabled":?, "floatValue":?}` → `CarCommandMessage`.

Known commands: `set_driving_mode`, `set_regen_level`, `set_avas_mute`, `set_ambient_light`,
`set_wifi`, `set_hvac_ac`, `set_hvac_temp` (uses `floatValue`), `set_hvac_fan`, `set_hvac_recirc`,
`set_hvac_eco`, `set_hvac_defrost`, `request_status`. Climate is back under the normalized `set_hvac_*`
names (the old `toggle_ac`/`set_ac_temp`/`set_fan_speed` are gone). Status also carries `rangeKm`,
`gear`, `odometerKm`, `hvac*`, and `tire*` telemetry — full field list in
[`docs/phone-companion-protocol.md`](docs/phone-companion-protocol.md).

> The status DTO uses `*Available` flags — a field is only meaningful/UI-enabled when its
> `xxxAvailable` is `true`. Respect that when rendering.

### Bluetooth SPP transport (planned)

> **Plan chi tiết:** [`docs/bluetooth-transport-plan.md`](docs/bluetooth-transport-plan.md) — phased plan
> với code sketch, gotcha, và checklist theo file. §9 mô tả phụ thuộc phía xe.

Thêm **Bluetooth SPP (RFCOMM)** làm transport thứ hai chạy song song TCP WiFi.
Phone mở kênh SPP trên bond BT đã có sẵn (handsfree/A2DP) → user không phải thao tác gì.
UUID SPP: `6f1e2a00-47b8-4c1e-9d3a-c0ffee000001`. DTO/framing NDJSON **giữ nguyên 1:1**.

Phase 0 (POC echo) đã code xong, chờ test trên xe thật.

---

## 5. Conventions (enforce these)

See [`docs/coding-rules.md`](docs/coding-rules.md) for the full list. The essentials:

1. **Layering.** UI never touches `CarRepository` or the socket directly — go through `CarViewModel`.
2. **State exposure.** ViewModel exposes `StateFlow` (read-only via `asStateFlow()`), never `MutableStateFlow`.
3. **Coroutines.** All socket/command work runs in `viewModelScope`; commands are fire-and-forget `launch {}`.
4. **New control = 4 edits, always in this order:** DTO field → Repository sender → ViewModel method → Compose UI. See [`docs/adding-a-feature.md`](docs/adding-a-feature.md).
5. **Serialization.** JSON is lenient + `ignoreUnknownKeys`. Give every `@Serializable` field a default so partial frames parse.
6. **Theming.** Read the accent from the current `CarColor` (`carColor.accent`, animated). Don't hardcode brand green.
7. **Strings.** User-facing text in Vietnamese. Prefer `res/values/strings.xml` for new copy where practical.
8. **Compose.** Stateless composables take data + lambdas; hoist state to `MainApp`/ViewModel.

---

## 6. Known gotchas

- **Cleartext — no longer an issue.** The old `ws://:8080` was cleartext HTTP, blocked by default on
  `targetSdk 35`. The current transport is a raw `java.net.Socket` on TCP `:47800`, which is **not**
  subject to Android's cleartext-HTTP policy — no `network_security_config` / `usesCleartextTraffic`
  needed. (History: [`docs/websocket-protocol.md`](docs/websocket-protocol.md#cleartext).)
- **Prefs persistence.** `SharedPreferences("app_prefs")` holds `ip_address` and `car_color` (the `CarColor` enum name, written on every color pick in `MainApp`). Both survive app restart.
- **Reconnect loop.** `CarConnectionManager.startLoop()` retries every 3s forever, trying each `CarLink` in priority order per pass. `setCarIp()` / `setTransportMode()` / `setBtDevice()` restart it, cancelling the old job **and** calling `repository.disconnect()` — coroutine cancel alone cannot unblock a blocking `readLine()`, only closing the socket can.
- **One `CarRepository` per *process*, forever** — owned by `CarConnectionManager`, not by the ViewModel. Both `CarViewModel` and `CarConnectionService` `retain()` it; the loop runs while ≥1 owner holds it, so opening the app while the service runs does **not** open a second socket. Switching transport = `repository.setLink(link)` then `connect()`. Creating a second repository leaks the collectors bound in `CarViewModel.init {}`.
- **`commandResponses` is a `Channel`, so single-consumer.** Only `CarViewModel` may collect it — a second collector steals frames.
- **Background running.** `CarBluetoothReceiver` (manifest, survives reboot without `BOOT_COMPLETED`) sees the chosen head unit's `ACL_CONNECTED` → starts `CarConnectionService`; `ACL_DISCONNECTED` stops it. Bluetooth broadcasts requiring `BLUETOOTH_CONNECT` are exempt from both the API 26+ implicit-broadcast ban and the API 31+ background-FGS-start restriction — that exemption is what makes this work, don't switch the trigger to some other broadcast. Toggle: `TransportPrefs.autoBackground`.
- **Blocking Bluetooth IO.** `BluetoothSocket.connect()` / `readLine()` ignore coroutine cancellation. Every timeout is implemented as a watchdog that **closes the socket**: connect timeout in `BtSppCarLink`, 35s idle timeout in `CarRepository` (`BluetoothSocket` has no `soTimeout`).
- **Window percent: protocol says *openness*, the UI shows *closed-ness*.** `set_window` and
  `window*Percent` keep the protocol scale (`0` = đóng, `100` = mở hết). The vertical slider in
  `ControlScreen` puts the thumb at the **top** for closed and drags down to open, and prints
  `100 - percent` so a closed window reads `100%`. That inversion lives in exactly one place
  (`WindowTile.statusText`) — don't push it down into the ViewModel or Repository.
- **`components/DashboardComponents.kt`** is a grab-bag of shared composables, not a navigated screen. Reusable widgets live there.

---

## 7. Where to look for AI-specific help

- **Deep docs:** [`docs/`](docs/) — architecture, protocol, UI conventions, coding rules, feature playbook.
- **Subagent:** `.claude/agents/android-feature-builder.md` — end-to-end car-control feature builder.
- **Skill:** `.claude/skills/add-car-control/SKILL.md` — step-by-step recipe for wiring a new command.
