# UI Conventions (Jetpack Compose)

Dark, glassmorphism aesthetic. Material 3. Accent color is **dynamic** — it comes from the
selected car paint color and animates on change. Follow these so new screens look native.

## Accent theming — the core idea

The whole UI recolors based on `CarColor` (see [`ui/CarColor.kt`](../app/src/main/java/com/example/ex2_phone/ui/CarColor.kt)).
Every screen receives `carColor: CarColor` and derives an **animated** accent:

```kotlin
val accent by animateColorAsState(carColor.accent, tween(450), label = "accent")
```

Use `accent` for highlights, selected states, active borders, primary values. Use `carColor.onAccent`
for text/icons drawn **on** an accent-filled surface. Never hardcode the brand green — always read
from `carColor`.

`CarColor` fields: `swatch` (real paint color in the picker), `accent` (UI highlight, brightened
for readability on dark bg), `onAccent`, `onSwatch`. Default = `CarColor.GEELY_TEAL`.

## Background gradient

Screens use a top-tinted vertical gradient so the accent subtly bleeds into the backdrop:

```kotlin
val bgTop = lerp(Color(0xFF141414), carColor.accent, 0.08f)   // 0.06f on Control/Settings
Column(
    modifier = Modifier
        .fillMaxSize()
        .background(Brush.verticalGradient(listOf(bgTop, Color(0xFF0A0A0A))))
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
) { ... }
```

## Cards

Use [`GlassCard`](../app/src/main/java/com/example/ex2_phone/ui/components/GlassCard.kt) for grouped
content. It draws a rounded (`20.dp` default) vertical-gradient dark surface with a subtle white
border and 16.dp inner padding.

```kotlin
GlassCard(
    modifier = Modifier.fillMaxWidth(),
    borderColor = accent.copy(alpha = 0.15f),   // brighten border to signal "active"
) { /* content */ }
```

Convention: a control that is **on/active** gets a brighter accent border
(`accent.copy(alpha = 0.4f)`); when off, `Color.White.copy(alpha = 0.08f)`.

## Shared widgets (in `components/DashboardComponents.kt`)

Reuse these instead of re-rolling:

- `StatTile(icon, label, value, unit, color, modifier)` — a metric tile (WiFi, outside temp, …).
- `MiniStat(icon, label, value, color, modifier)` — compact inline metric used in the Home hero.
- `RingGauge(percent, color, size, centerLabel)` — circular gauge (battery).
- `SegmentedControl(options, selectedIndex, onSelect, accent, onAccent)` — segmented picker; reused
  for both driving modes and regen levels (options are `SegOption`).
- `ToggleCard(icon, title, onText, offText, checked, onCheckedChange, accent, modifier)` — on/off card
  (AVAS, ambient light, WiFi).
- `SectionHeader(icon, title, accent)`, `Pill(text, color)`, `StatusPill(isConnected, isError)`.

## Switches

Material 3 `Switch` styled to the accent:

```kotlin
Switch(
    checked = state,
    onCheckedChange = { viewModel.setX(it) },
    colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = accent,
        uncheckedThumbColor = Color.Gray,
        uncheckedTrackColor = Color(0xFF2A2A2E),
        uncheckedBorderColor = Color.Transparent,
    ),
)
```

## Section layout pattern

Each Home/Control section is: a `Text` title (`18.sp`, `SemiBold`, `Color.White`,
`Modifier.align(Alignment.Start)`), a small `Spacer`, then the control(s), then a
`Spacer(20.dp)` before the next section.

## Text & language

- User-facing copy is **Vietnamese** (`"Tốc độ"`, `"Chế độ lái"`, `"Điều hòa"`, …).
- Prefer `res/values/strings.xml` for new copy; inline literals are common in existing code but
  new hardcoded strings should trend toward resources.
- Typography via `theme/Type.kt`; colors via `theme/Color.kt` + `Theme.kt` (dark scheme).

## Statelessness

Composables are stateless where possible: they take current values + `on…` lambdas. State
(`carColor`, `ipAddress`) is hoisted to `MainApp`; car telemetry lives in `CarViewModel`. A screen
gets the ViewModel plus whatever hoisted values/callbacks it needs (see `SettingsScreen` signature).
