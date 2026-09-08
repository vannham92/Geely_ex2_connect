# Playbook: Add a new car control

Worked example. Follow the **4-step order** every time. We'll add a **"lock/unlock doors"** control.

The layers touched (in order): DTO → Repository → ViewModel → UI.

---

## Step 1 — DTOs (`data/network/CarRepository.kt`)

Add status field(s) with an `Available` flag (all with defaults), and, if the command needs a new
payload shape, a nullable field on `CarCommandMessage`.

```kotlin
@Serializable
data class CarStatusMessage(
    // ...existing...
    val doorsLocked: Boolean = false,
    val doorsAvailable: Boolean = false,
)

@Serializable
data class CarCommandMessage(
    val command: String,
    val modeValue: Int? = null,     // Int payload (driving mode, regen level)
    val enabled: Boolean? = null,   // Bool payload — reuse for lock/unlock
)
```

Reuse an existing payload field when the shape fits (`modeValue` for Int, `enabled` for Bool). Only
add a new nullable field if none fits — never make a field non-null. Keep the DTO in lock-step with
the car's `CarCommandMessage` (see [`phone-companion-protocol.md`](phone-companion-protocol.md)).

## Step 2 — Repository sender (`CarRepository`)

One `suspend` method that builds a `CarCommandMessage` and calls `sendCommand`. Match the protocol
`command` string exactly.

```kotlin
suspend fun setDoorsLocked(locked: Boolean) {
    sendCommand(CarCommandMessage(command = "set_doors_locked", enabled = locked))
}
```

## Step 3 — ViewModel wrapper (`ui/CarViewModel.kt`)

Fire-and-forget in `viewModelScope`.

```kotlin
fun setDoorsLocked(locked: Boolean) {
    viewModelScope.launch { repository?.setDoorsLocked(locked) }
}
```

## Step 4 — UI (`ui/HomeScreen.kt` or the relevant screen)

Read from `carStatus`, gate on the `Available` flag, call the ViewModel. Reuse `GlassCard` +
accent conventions ([`ui-conventions.md`](ui-conventions.md)).

```kotlin
if (carStatus.doorsAvailable) {
    Text("Khóa cửa", color = Color.White, fontSize = 18.sp,
         fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start))
    Spacer(Modifier.height(10.dp))
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (carStatus.doorsLocked) accent.copy(alpha = 0.4f)
                      else Color.White.copy(alpha = 0.08f),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Cửa xe", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (carStatus.doorsLocked) "Đã khóa" else "Đang mở",
                     color = if (carStatus.doorsLocked) accent else Color.Gray, fontSize = 13.sp)
            }
            Switch(
                checked = carStatus.doorsLocked,
                onCheckedChange = { viewModel.setDoorsLocked(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White, checkedTrackColor = accent,
                    uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color(0xFF2A2A2E),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}
```

---

## Checklist before you're done

- [ ] DTO fields have defaults; new status field has an `xxxAvailable` companion.
- [ ] Repository method name + `command` string match the car firmware's expected command.
- [ ] ViewModel wrapper uses `viewModelScope.launch`.
- [ ] UI reads from `carStatus` (server-authoritative), gated on `xxxAvailable`.
- [ ] Accent comes from `carColor`; reused `GlassCard`/shared widgets; copy is Vietnamese.
- [ ] `./gradlew assembleDebug` builds clean.

## Which screen?

- Vehicle-wide telemetry read-outs → `HomeScreen.kt`.
- Toggles / driving modes / regen / AVAS / ambient / WiFi → `ControlScreen.kt`.
- Connection/appearance/config → `SettingsScreen.kt`.
- New reusable widget → add it to `components/DashboardComponents.kt`.
