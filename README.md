# Guardian — parental control system

A two-app Android parental-control system in Kotlin, targeting **Android 10 (API 29) →
Android 15 (API 35)**.

- **Parent app** (`com.example.guardian.parent`) — dashboard to monitor and control the child device.
- **Child app** (`com.example.guardian.child`) — runs on the child's phone, enforces controls, reports activity.
- **Backend** — Firebase (Auth, Firestore, Cloud Messaging, Storage).

---

## ⚠️ Platform reality — what Android actually allows

This system implements the **real** platform APIs and does not fake capabilities. Read this
table before promising features to anyone.

| Requested feature | Feasible? | How it's actually done |
|---|---|---|
| Remotely **lock** the device | ✅ | `DevicePolicyManager.lockNow()` (Device Owner or active Device Admin). |
| Remotely **power off** the device | ❌ | Not available to apps. Locking is the real control. |
| See every incoming/outgoing **notification** | ✅ | `NotificationListenerService` (user must grant Notification Access). |
| Read **WhatsApp/TikTok message content** | ⚠️ Preview only | Only the notification's title/text. Another app's private database is unreadable without root. |
| **Screen time** per app + limits | ✅ | `UsageStatsManager` (monitor) + app-blocking overlay / `DevicePolicyManager` (enforce). |
| **Block/allow** specific apps | ✅ | `setApplicationHidden()` (Device Owner) or an accessibility-based blocker (standard). |
| Child app **fully hidden / invisible** | ⚠️ Partial | A foreground service must show a notification on Android 8+. It can be minimal but not invisible. Device Owner apps can be hidden from the launcher. |

**Legal note.** Monitoring is lawful for a parent's own minor child on a device they own.
The child app shows a visible **consent screen on first launch** regardless, and stays
transparent about what is active (it does not hide itself).

---

## Module layout

```
Guardian/
├── core/            Android library — shared Firestore models, paths, pairing logic (+ unit tests)
├── parent/          Parent app — Compose UI, Firebase Auth, dashboard (Phase 2+)
├── child/           Child app — consent/transparency shell, monitoring & enforcement (Phase 3+)
├── firebase/        firestore.rules (deploy with firebase CLI)
├── docs/            FIREBASE_SETUP.md and other setup docs
└── gradle/          version catalog (libs.versions.toml) + wrapper
```

Package roots: `com.example.guardian.{core,parent,child}`.

## Tech stack

Kotlin 2.0 · Jetpack Compose (Material 3) · Hilt (DI) · Firebase (Auth + Firestore + FCM) ·
Coroutines/Flow · KSP · Gradle version catalog. AGP 8.7 / Gradle 8.9 / JDK 17.

---

## Getting started

1. **Install the Firebase config** — the build fails without it. See
   [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md). Short version: create a Firebase
   project, register both package names above, drop `google-services.json` into `parent/`
   and `child/`, enable Email/Password auth and Firestore.
2. **Open in Android Studio** (Ladybug or newer) and let it sync. If the Gradle wrapper JAR
   is missing, run `gradle wrapper --gradle-version 8.9` once, or let Android Studio restore it.
3. **Run** the `parent` or `child` run configuration on an emulator/device (API 29–35).

### Command line

```bash
./gradlew :core:test           # unit tests (enum/command parsing, pairing codes)
./gradlew :parent:assembleDebug
./gradlew :child:assembleDebug
```

---

## What's implemented so far

### Phase 8 (Hardening & testing) ✅
- **Unit tests** (JUnit + MockK) across `core` logic and both apps' view models / executors —
  policy evaluation, usage roll-up, command/pairing parsing, provisioning payload, `CommandExecutor`,
  `ChildPairViewModel`, `AuthViewModel`, formatting.
- **Instrumented** Compose tests (consent gate, usage chart).
- **Firestore security-rules test suite** (`firebase/test/`, run against the emulator) covering
  ownership isolation and the child-claim path.
- **`docs/TESTING.md`** — how to run every suite, the manual Android 10/12/14+ matrix, the offline
  and Device-Owner procedures, and the per-permission **graceful-degradation checklist**.

> ⚠️ These tests are written but not yet executed here (no Android SDK / `google-services.json` in
> this environment). Running them in Android Studio is the first real compile+run — expect to fix
> issues that only surface at build time. See [docs/TESTING.md](docs/TESTING.md).

### Phase 7 (Device Owner path) ✅
- **QR provisioning** — parent "Device Owner setup" screen generates the provisioning QR from a
  hosted APK URL + signing checksum (tested `ProvisioningPayload` builder in core). Full steps
  (QR **and** adb) in [docs/DEVICE_OWNER_PROVISIONING.md](docs/DEVICE_OWNER_PROVISIONING.md).
- **`DeviceOwnerManager`** wraps the DO-only APIs: **silent app hiding** (`setApplicationHidden`
  reconciled against the blocked list — a blocked app just disappears), **uninstall protection**,
  and **factory-reset/tamper protection** (`DISALLOW_FACTORY_RESET`/`ADD_USER`/`SAFE_BOOT`), plus
  a lock-task allow-list for kiosk.
- `onProfileProvisioningComplete` applies protections and starts monitoring automatically.
- The child reports `ownerMode` + uninstall-blocked in `status/current`; the parent dashboard and
  the child's transparency screen both show whether the device is fully managed.
- Every DO call is a safe no-op on the standard path, so both modes share one codebase.

> Device Owner requires a **factory-reset** device (no accounts) — there is no in-place upgrade.
> The child's own launcher icon is deliberately **not** hidden, so the consent screen stays reachable.

### Phase 6 (Enforcement) ✅
- **Parent policy editor** (dashboard → tune icon): per-app **block** toggles and **daily time
  limits**, a **daily total budget**, and a **bedtime** window (time pickers + day-of-week chips).
  Writes `policies/current`; the child picks it up live.
- **Pure `PolicyEvaluator`** in core (block precedence: explicit → bedtime → budget → per-app
  limit; bedtime wraps midnight and honors day-of-week) — **thoroughly unit-tested**.
- **App blocker** — an AccessibilityService detects the foreground app and launches a full-screen
  **block screen** with the reason. New "App blocking & time limits" grant card on the child.
- **Offline-first** — policy cached in **Room** (`localPolicy()` never hits the network); a
  **WorkManager** job re-applies every 15 min, locking via Device Admin when bedtime/budget hits,
  so time-based enforcement holds with no connection.
- `REFRESH_POLICY` command now triggers an immediate re-sync.

> **Standard-path enforcement is a strong nudge, not a cage:** the child can still reach Home,
> and Settings/System UI are exempt to avoid lockout. Inescapable blocking + app hiding is the
> **Device Owner** path (Phase 7). Also: a per-app limit reached *while the app stays open* is
> caught on the next app-switch or the 15-min worker, not mid-second.

### Phase 5 (Command channel) ✅
- Parent **"Lock device now"** control on the dashboard writes a `commands/{autoId}` doc; a
  live "Last: LOCK — executed/failed" status line reflects the outcome in real time.
- Child drains the command queue via a **live Firestore listener** in the monitor service —
  real-time execution with **no server required** while the app is running. Each command is
  executed once (marked `DELIVERED` → `EXECUTED`/`FAILED` with an error message on failure).
- **`lockNow()`** implemented through a **Device Admin** (`force-lock`): new receiver + an
  enable card on the child ("Remote lock"). If admin is off, the lock command fails *visibly*
  with a reason rather than silently.
- **FCM** wired as the background wake-up: child registers its token (into `status/current`,
  field-merged so it isn't clobbered), `GuardianFcmService` starts the monitor service on a
  data push. An **optional** Cloud Function (`firebase/functions/`) sends that push on command
  creation — deploy it for reliable background delivery; commands still work without it whenever
  the child service is alive.

> `lockNow()` needs an **active Device Admin** on the child (standard path). Full Device Owner
> provisioning — which also enables app-hiding and uninstall protection — is Phase 7.

### Phase 4 (Parent dashboard) ✅
- Tapping a paired device opens a **per-device dashboard** (`device/{deviceId}` route).
- **Monitoring header** — live from `status/current`: active/off, currently-open app, and
  explicit "not granted on device" warnings so permission gaps are visible, not silent.
- **Screen-time chart** — horizontal per-app bar chart with a **Today / Last 7 days** toggle,
  total time, and top-N apps (rolls up daily docs via the tested `UsageAggregator`).
- **Live notification feed** — newest-first stream (app label, title, text, relative time),
  updating in real time.
- All views are real-time Firestore listeners (`DeviceDataRepository`); new `core` logic
  (`UsageAggregator`, `DateKeys.recentDateKeys`) is pure and unit-tested.

### Phase 3 (Child monitoring) ✅
- **Foreground monitor service** (`dataSync`) — minimal, non-hideable notification; polls
  `UsageStatsManager` every 60s, aggregates per-app foreground minutes for the day (with app
  labels resolved on-device), and uploads to `devices/{deviceId}/usage/{today}`. Also writes a
  live `status/current` heartbeat (monitoring active? which permissions granted? foreground app).
- **NotificationListenerService** — forwards notification previews (title + text, app label,
  timestamp) to `devices/{deviceId}/notifications/{autoId}`. Skips group summaries, blank
  notifications, and its own. Runs independently of the foreground service.
- **Permissions/setup UI** — checklist for Usage access, Notification access, and (API 33+)
  POST_NOTIFICATIONS, each with a Settings shortcut and rationale. Re-checks on resume.
  **Graceful degradation**: a denied grant disables just that data stream, never the app.
- **Boot receiver** — resumes monitoring after reboot when paired + consented.
- New `core`: `DeviceStatus` model, `UsageRecord.labels`, `DateKeys` (usage date keys) + tests.

> **Platform limitation (foreground service):** the `dataSync` FGS type is capped at ~6 cumulative
> hours/day on Android 15. For all-day monitoring the real fixes are the **Device Owner** path
> (Phase 7, exempt) or a `specialUse` FGS with a declared subtype. Notification capture is
> **preview-only** by design — see the limitation table above.

### Phase 2 (Pairing) ✅
- Parent: **"Pair a device"** flow — enters child name, generates a unique 6-digit code + QR,
  atomically creates the `devices/{deviceId}` and `pairing/{code}` docs, and live-waits for the
  claim. Dashboard lists paired devices (empty-state + list) with a NavHost (home ⇄ pair).
- Child: **anonymous auth** + pairing screen (type the 6-digit code **or** scan the QR via the
  bundled ZXing scanner). On claim it stamps its `childUid` onto the device doc, marks the
  pairing `CLAIMED`, and persists its identity locally (survives restarts). The transparency
  screen then shows the paired parent link.
- Security rules extended so the anonymous child can read a pairing doc and claim exactly one
  unclaimed device bound to it. Codes expire after 10 minutes (enforced client-side + in rules).
- New `core` logic + tests: `PairingSession`, `PairingStatus`, `PairingPayload` (QR encode/decode).

> Requires **Anonymous** sign-in enabled in Firebase (see [docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md)).

### Phase 1 (Setup) ✅

- ✅ Two Gradle app modules + a shared `core` library, wired with a version catalog.
- ✅ Firebase wiring (Auth + Firestore) via Hilt.
- ✅ Parent **email/password** sign-up / sign-in with validation and friendly errors; a
  placeholder dashboard behind auth; sign-out.
- ✅ Full **Firestore schema as typed models** (`Device`, `UsageRecord`, `NotificationEvent`,
  `Policy`/`Bedtime`, `Command`) + centralized `FirestorePaths`.
- ✅ Child app **consent + transparency** shell (first-launch consent gate, persisted).
- ✅ Starter Firestore security rules.
- ✅ Unit tests (command/enum parsing, pairing-code generation/validation) and one Compose
  UI test (consent gate).

### Firestore schema

```
users/{parentUid}                                    { email, createdAt }
devices/{deviceId}                                   { parentUid, childUid, childName, paired, pairedAt, ownerMode }
devices/{deviceId}/usage/{date}                      { apps: { package: minutes }, updatedAt }
devices/{deviceId}/notifications/{autoId}            { pkg, appLabel, title, text, ts }
devices/{deviceId}/policies/current                  { blockedApps[], limits{}, dailyBudgetMinutes, bedtime{} }
devices/{deviceId}/commands/{autoId}                 { type, payload, status, createdAt, executedAt, error }
pairing/{code}                                       { code, parentUid, deviceId, childName, status, childUid, createdAt, expiresAt }
```

---

## Roadmap (subsequent phases)

2. ~~**Pairing** — QR / 6-digit code linking, device document creation.~~ ✅ done
3. ~~**Child monitoring** — `UsageStatsManager` poll + `NotificationListenerService` → Firestore.~~ ✅ done
4. ~~**Parent dashboard** — usage chart, current foreground app, live notification feed.~~ ✅ done
5. ~~**Command channel** — FCM + Firestore commands; implement `lockNow`.~~ ✅ done
6. ~~**Enforcement** — app-blocking overlay, per-app limits, offline WorkManager re-apply.~~ ✅ done
7. ~~**Device Owner path** — QR provisioning, app-hiding, uninstall protection.~~ ✅ done
8. ~~**Hardening & testing** — permission-denied degradation, Android 10/12/14+ matrix, offline
   enforcement, Device Owner on a factory-reset emulator.~~ ✅ done (see [docs/TESTING.md](docs/TESTING.md))

**All 8 build-plan phases are implemented.** Remaining work is execution: build in Android Studio,
supply Firebase config, and work through the manual matrix — the phase that turns written code into
verified code.

Provisioning strategy: build the **standard permission-based** path first (Device Admin +
Accessibility + Usage Access + Notification Access, each user-granted — testable without a
factory reset), then layer the Device Owner path on top.
