# docs/ — AI & developer reference

Start at the root [`AGENTS.md`](../AGENTS.md) for the overview. These files go deeper:

| File | Read it when… |
|------|---------------|
| [architecture.md](architecture.md) | You need the layer diagram, data-flow rules, connection lifecycle. |
| [phone-companion-protocol.md](phone-companion-protocol.md) | **START HERE for the car connection.** Current car protocol: TCP `:47800` NDJSON. Full status/command tables + copy-paste Kotlin client + migration delta. |
| [websocket-protocol.md](websocket-protocol.md) | ⚠️ **Retired.** The old Ktor `ws://:8080/ws/car` protocol. The app is migrated off it; kept only for history. Superseded by the doc above. |
| [bluetooth-transport-plan.md](bluetooth-transport-plan.md) | You work on the **Bluetooth SPP transport** (RFCOMM) that runs alongside TCP WiFi. Phased plan + file checklist. Phase 0 (POC) is coded, not yet verified on the car. |
| [ui-conventions.md](ui-conventions.md) | You build or edit any Compose UI — accent theming, GlassCard, shared widgets. |
| [coding-rules.md](coding-rules.md) | Always. The hard rules; auto-loaded via `CLAUDE.md`. |
| [adding-a-feature.md](adding-a-feature.md) | You add a new car control (the 4-step DTO→Repo→VM→UI recipe, worked example). |

Also: subagent `.claude/agents/android-feature-builder.md`, skill `.claude/skills/add-car-control/SKILL.md`.
