# Testing & hardening

## Automated tests

### Unit (JVM — JUnit + MockK)
```bash
./gradlew :core:test :parent:testDebugUnitTest :child:testDebugUnitTest
```
Covers the framework-free logic:

| Area | Test |
|------|------|
| Policy evaluation (block precedence, per-app limits, budget, bedtime incl. midnight wrap + days) | `core` `PolicyEvaluatorTest` |
| Usage roll-up (daily/weekly, label merge) | `core` `UsageAggregatorTest` |
| Command / enum parsing (unknown → UNKNOWN) | `core` `CommandParsingTest` |
| Pairing code gen/validation + QR payload encode/decode | `core` `PairingCodeTest`, `PairingPayloadTest` |
| Pairing session status + expiry | `core` `PairingSessionTest` |
| Date keys (usage doc ids, day boundaries) | `core` `DateKeysTest` |
| Device Owner provisioning payload | `core` `ProvisioningPayloadTest` |
| Command execution (LOCK w/ + w/o admin, refresh, unknown) | `child` `CommandExecutorTest` |
| Child pairing view model (validation, claim, errors) | `child` `ChildPairViewModelTest` |
| Parent auth view model (loading/error/success) | `parent` `AuthViewModelTest` |
| Minute formatting | `parent` `FormattingTest` |

### Instrumented (Espresso / Compose — needs a device/emulator)
```bash
./gradlew :child:connectedDebugAndroidTest :parent:connectedDebugAndroidTest
```
- `child` `ConsentScreenTest` — first-launch consent gate (button disabled until acknowledged).
- `parent` `UsageChartTest` — usage bar chart renders top apps and collapses the rest.

### Firestore security-rules (emulator)
```bash
cd firebase/test && npm install && npm test
```
Requires the Firebase CLI. `firebase.json` points the emulator at `../firestore.rules`. The suite
asserts: profile isolation, device ownership reads, the parent-only device create, the child claim
(own uid only, parent immutable), and bound-child vs. stranger sub-collection access.

---

## Manual test matrix

Run the key flows on **Android 10 (API 29)**, **12 (API 31/32)**, and **14/15 (API 34/35)**.

| Flow | Steps | Pass criteria | 10 | 12 | 14+ |
|------|-------|---------------|----|----|-----|
| Pair (code) | Parent → Pair; type code on child | Parent shows "Paired"; device appears | ☐ | ☐ | ☐ |
| Pair (QR) | Scan parent QR on child | Same as above | ☐ | ☐ | ☐ |
| Usage upload | Use apps on child ~2 min | Bars appear on parent dashboard w/ labels | ☐ | ☐ | ☐ |
| Notification feed | Trigger a notification on child | Preview appears in parent feed | ☐ | ☐ | ☐ |
| Remote lock | Parent → Lock device now | Child screen locks; parent shows "executed" | ☐ | ☐ | ☐ |
| App block | Parent blocks an app | Block screen shows on child when opened | ☐ | ☐ | ☐ |
| Per-app limit | Set 1-min limit; exceed it | App blocked after limit | ☐ | ☐ | ☐ |
| Bedtime | Set window covering now | Device locks within 15 min (worker) | ☐ | ☐ | ☐ |
| Device Owner | Provision per DEVICE_OWNER_PROVISIONING.md | Blocked app hidden; uninstall blocked | ☐ | ☐ | ☐ |

### Offline enforcement
1. Set a per-app block/limit and a bedtime window; confirm the child caches the policy.
2. Put the child in **airplane mode**.
3. Open the blocked app → still blocked. Enter the bedtime window → device locks within 15 min
   (the `PolicyReapplyWorker` runs offline off the Room cache).

### Device Owner
Follow [DEVICE_OWNER_PROVISIONING.md](DEVICE_OWNER_PROVISIONING.md) on a **factory-reset**
emulator (`adb shell dpm set-device-owner …`). Verify: blocked apps disappear from the launcher,
the child app can't be uninstalled, and the parent header shows "Device Owner — full control".

---

## Graceful-degradation checklist (deny each, verify no crash)

| Denied | Expected behavior |
|--------|-------------------|
| Usage access | No usage bars; parent header shows "not granted: usage access"; app runs | 
| Notification access | No notification feed; parent shows the gap; app runs |
| POST_NOTIFICATIONS (13+) | Foreground service still runs; status notification simply not shown |
| Accessibility (app blocking) | Blocking/limits don't fire; monitoring + lock still work |
| Device Admin (remote lock) | Lock command **fails visibly** ("Device admin isn't enabled"), not silently |
| Not paired yet | All upload/command paths no-op cleanly |
| Offline | Reads served from Room cache; uploads retried by Firestore SDK on reconnect |

Every capability is independent — denying one disables exactly its feature and nothing else.
