# Coding Rules

Hard rules for this repo. Auto-loaded into Claude Code via `CLAUDE.md`. Apply to every change.

## Architecture & layering

1. **Respect MVVM boundaries.** UI (`ui/*Screen.kt`, composables) talks **only** to `CarViewModel`.
   Never import `CarRepository`, `java.net.*`/socket types, or serialization DTOs into a composable
   except the plain `CarStatusMessage` value read from the ViewModel's flow.
2. **The Repository owns networking.** All TCP-socket / NDJSON code stays in `data/network/CarRepository.kt`.
3. **Expose immutable state.** ViewModel and Repository expose `StateFlow` via `.asStateFlow()`;
   keep `MutableStateFlow` private. Never expose a mutable flow.
4. **Coroutines belong to `viewModelScope`.** Commands are fire-and-forget `viewModelScope.launch { }`.
   Do not block, do not use `GlobalScope`, do not run network calls on the main thread manually.

## Networking & serialization

5. **Server-authoritative UI.** Render from `carStatus`, not from the command you just sent. Wait
   for the car's next `status` broadcast. (Transient local UI like a slider drag may preview.)
6. **DTO defaults.** Every field in a `@Serializable` DTO must have a default value — frames can be
   partial and JSON is parsed with `ignoreUnknownKeys = true`, `isLenient = true`.
7. **Honor `*Available` flags.** Only show/enable a control when its `xxxAvailable` is `true`.
8. **One command, one method.** Each car command = one `suspend` sender in `CarRepository` + one
   wrapper in `CarViewModel`. Keep names aligned with the protocol `command` string.

## Adding a car control (the canonical 4-step order)

9. Always in this order (see [`adding-a-feature.md`](adding-a-feature.md)):
   1. Add field(s) to `CarStatusMessage` (+ `xxxAvailable`) and/or `CarCommandMessage`.
   2. Add a `suspend fun setX(...)` to `CarRepository` that calls `sendCommand(...)`.
   3. Add `fun setX(...)` to `CarViewModel` wrapping it in `viewModelScope.launch`.
   4. Add the UI in the relevant `*Screen.kt`, reading `carStatus` and calling `viewModel.setX`.

## UI

10. **Dynamic accent, not hardcoded.** Derive `accent` from `carColor` via `animateColorAsState`;
    use it for highlights/active states. Reuse `GlassCard`, `ToggleCard`, `SegmentedControl`, `StatTile`.
    Details in [`ui-conventions.md`](ui-conventions.md).
11. **Vietnamese UI copy.** User-facing strings in Vietnamese; code identifiers in English. Prefer
    `res/values/strings.xml` for new copy.
12. **Stateless composables.** Take values + lambdas; hoist state to `MainApp`/ViewModel.

## Dependencies & build

13. **Version catalog only.** Add libs to [`gradle/libs.versions.toml`](../gradle/libs.versions.toml)
    and reference `libs.*`. No inline version strings in `build.gradle.kts`.
14. **SDK levels are load-bearing:** `minSdk 26`, `targetSdk 35`, Java 17, Kotlin `2.2.x`, Compose BOM.
    Don't use APIs above `minSdk 26` without guarding, and don't bump SDK/toolchain casually.

## General

15. **Match surrounding style.** Follow the naming, comment density, and idioms already in the file.
16. **No `usesCleartextTraffic` needed.** The transport is a raw `java.net.Socket` (TCP `:47800`),
    which is **not** subject to Android's cleartext-HTTP policy — don't add `usesCleartextTraffic`
    or a `network_security_config`. (Only the retired `ws://` transport ever needed it — history in
    [`websocket-protocol.md`](websocket-protocol.md#cleartext).)
17. **No secrets, no hardcoded prod IPs** beyond the existing dev default `10.77.86.7`.
