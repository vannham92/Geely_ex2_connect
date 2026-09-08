---
name: use-project-agents
description: Consults project subagents in .cursor/agents before and after writing code in geely_ex2_tools. Use when implementing features, refactoring, fixing bugs, or reviewing Android/Kotlin changes — read agent playbooks and delegate via Task when the task is large.
---

# Use Project Agents

Before writing or changing code in this repo, align with subagents in `.cursor/agents/`.

## Agent map

| Task | Agent | File |
|------|-------|------|
| Implement / modify Android features, UI, Gradle, Bluetooth/OBD/ADB | `android-senior` | [.cursor/agents/android-senior.md](../../agents/android-senior.md) |
| Review diff before commit or PR | `code-reviewer` | [.cursor/agents/code-reviewer.md](../../agents/code-reviewer.md) |

## Workflow

### Before coding

1. Identify which agent owns the task (see table above).
2. **Read** the matching `.md` file — treat its body as the playbook (architecture, conventions, checklist).
3. For isolated, multi-file work → **delegate** via Task tool with `subagent_type` matching the agent name when available, or pass the agent playbook in the prompt.
4. For small edits → follow the playbook directly without delegation.

### While coding

Match project stack from `android-senior`:

- Kotlin, Jetpack Compose, Material 3
- Package `com.geely.ex2.tools`
- Dependencies via `gradle/libs.versions.toml`
- Strings in `res/values/strings.xml`
- Verify with `.\gradlew.bat assembleDebug`

### After coding

For substantive changes, run a review pass per `code-reviewer`:

1. `git diff` + `git status`
2. Output: Critical → Warnings → Suggestions
3. Fix Critical issues before finishing

## Delegation rules

| Situation | Action |
|-----------|--------|
| New screen, feature, or architecture decision | Read `android-senior`, then implement |
| Build/runtime failure | Read `android-senior`, diagnose, fix |
| Large isolated task (new module, major refactor) | Task tool + android-senior playbook |
| Any non-trivial diff | Self-review using `code-reviewer` checklist |
| User asks for review | Delegate or follow `code-reviewer` fully |

## Do not

- Invent conventions that contradict the agents
- Skip agent playbooks for Android/Kotlin work in this repo
- Mix unrelated refactors with the requested change
