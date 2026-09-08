# WebSocket Protocol (phone ⇄ car) — ⚠️ LEGACY

> **Retired.** The head unit no longer runs this Ktor WebSocket server — it uses a raw
> **TCP `:47800` NDJSON** server (`CarTcpServer`), and the app has been migrated to match
> (`CarRepository` is now a `java.net.Socket` client). This file is kept only for history / to
> understand old commits. For the live protocol use
> [`phone-companion-protocol.md`](phone-companion-protocol.md).

## Endpoint

```
ws://<carIp>:8080/ws/car
```

- `carIp` comes from Settings (default `10.77.86.7`), stored in `SharedPreferences("app_prefs")` key `ip_address`.
- Engine: Ktor `HttpClient(CIO) { install(WebSockets) }`.
- JSON: `Json { isLenient = true; ignoreUnknownKeys = true }`.

## Message routing

The client parses every inbound text frame by `type`:

- `type == "status"` → `CarStatusMessage` → pushed to `carStatus` StateFlow.
- `type == "command_response"` → `CarCommandResponse` → sent to `commandResponses` channel/flow.
- Anything else → ignored (best-effort, wrapped in try/catch).

## Inbound: status (car → app)

`CarStatusMessage` — every field has a default so partial frames parse.

| Field | Type | Meaning |
|-------|------|---------|
| `type` | String | `"status"` |
| `speedKmh` | Float | Current speed (km/h) |
| `speedAvailable` | Boolean | Speed reading valid |
| `batteryPercent` | Float | Battery % |
| `batteryAvailable` | Boolean | Battery reading valid |
| `drivingMode` | Int | `1`=Eco, `2`=Comfort, `3`=Sport, `0`=unknown |
| `drivingModeAvailable` | Boolean | Driving mode valid |
| `acOn` | Boolean | AC on/off |
| `acTemp` | Float | AC target temp (°C), default `24` |
| `fanSpeed` | Int | Fan level (1..n) |
| `estimatedRange` | Float | Remaining range (km) |
| `totalDistance` | Float | Odometer (km) |
| `avasMuted` | Boolean | AVAS pedestrian-warning sound muted |
| `avasAvailable` | Boolean | AVAS control valid |
| `regenLevel` | Int | Energy regen `1`=Low, `2`=Med, `3`=High |
| `regenAvailable` | Boolean | Regen control valid |
| `ambientLightOn` | Boolean | Interior ambient light on/off |
| `ambientLightAvailable` | Boolean | Ambient light control valid |

> **`*Available` rule:** only treat a value as real / enable its control when the matching
> `xxxAvailable` flag is `true`. Otherwise show `—` / disabled.

## Outbound: command (app → car)

`CarCommandMessage` — only `command` is required; the rest are nullable and omitted when null.

| `command` | Payload field used | Repository method |
|-----------|--------------------|-------------------|
| `set_driving_mode` | `modeValue: Int` (1/2/3) | `setDrivingMode(mode)` |
| `toggle_ac` | `enabled: Boolean` | `toggleAc(enabled)` |
| `set_ac_temp` | `temperature: Float` | `setAcTemp(t)` |
| `set_fan_speed` | `speed: Int` | `setFanSpeed(n)` |
| `set_avas_mute` | `enabled: Boolean` (true = muted) | `setAvasMute(muted)` |
| `set_regen_level` | `modeValue: Int` (1/2/3) | `setRegenLevel(level)` |
| `set_ambient_light` | `enabled: Boolean` | `setAmbientLight(enabled)` |

Note the reuse of `modeValue` for both driving mode and regen level. Fields not relevant to a
command stay `null` and are dropped from the JSON.

Example outbound frame:

```json
{"command":"set_ac_temp","temperature":22.0}
```

## Inbound: command response (car → app)

`CarCommandResponse`:

```json
{"type":"command_response","command":"set_ac_temp","ok":true,"error":null}
```

Exposed via `CarRepository.commandResponses` (a `Channel.receiveAsFlow()`). Currently the UI does
not surface these; wire a collector in the ViewModel if you need per-command success/error toasts.

## Semantics: the car is authoritative {#authoritative}

Sending a command does **not** change local state. The car applies it and broadcasts a new
`status`, which updates the UI. Render switches/buttons from `carStatus`, not from the last command
you sent. Exception: transient local UI (a slider being dragged) may preview before the echo.

## Cleartext gotcha {#cleartext}

`ws://` is cleartext HTTP. `AndroidManifest.xml` declares only `INTERNET` — there is no
`android:usesCleartextTraffic` and no `res/xml/network_security_config.xml`. On `targetSdk 35`
Android blocks cleartext by default, which can make the socket fail to connect on a real device
even though the car is reachable.

If connection fails on device, add a network security config scoped to the car's local subnet, e.g.:

```xml
<!-- app/src/main/res/xml/network_security_config.xml -->
<network-security-config>
  <domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">10.77.86.7</domain>
  </domain-config>
</network-security-config>
```

and reference it from `<application android:networkSecurityConfig="@xml/network_security_config">`.
Prefer this over the blanket `usesCleartextTraffic="true"`.
