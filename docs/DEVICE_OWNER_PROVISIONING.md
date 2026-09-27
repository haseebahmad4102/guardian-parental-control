# Device Owner provisioning

The **Device Owner (DO)** path gives Guardian full control on the child device: silent app
hiding, uninstall protection, factory-reset protection, and kiosk/lock-task. It is the
"full power" alternative to the standard permission-based path.

A device can only become Device Owner when it has **no accounts and no other owner** — i.e.
right after a **factory reset**, before any Google account is added. You cannot promote an
already-set-up phone to Device Owner.

There are two ways to provision: **adb** (fast, for development) and **QR** (for real devices).

---

## Option A — adb (development / emulator)

On a freshly wiped emulator or device with the child app installed and **no accounts added**:

```bash
adb install child-debug.apk
adb shell dpm set-device-owner com.example.guardian.child/com.example.guardian.child.admin.GuardianDeviceAdminReceiver
```

Success prints `Success: Device owner set to ...`. If it fails with "already has an owner or
accounts", wipe the emulator (**Cold Boot / Wipe Data**) and retry before adding any account.

To remove it again during testing:

```bash
adb shell dpm remove-active-admin com.example.guardian.child/com.example.guardian.child.admin.GuardianDeviceAdminReceiver
```

---

## Option B — QR provisioning (real factory-reset device)

1. **Host the child APK** at an HTTPS URL the device can reach during setup.
2. **Compute the signing-cert checksum** — a URL-safe base64 SHA-256 of the APK's signing
   certificate. From the signed APK:
   ```bash
   # Extract the signer certificate and hash it, then base64url-encode (no padding):
   apksigner verify --print-certs --out /tmp/cert.pem child-release.apk >/dev/null 2>&1 || true
   # Simpler: use the certificate SHA-256 that apksigner prints, or compute from the DER cert:
   openssl x509 -in cert.pem -outform DER 2>/dev/null | openssl dgst -sha256 -binary \
     | openssl base64 | tr '+/' '-_' | tr -d '='
   ```
   The result is the value for **Signing-cert checksum**. (For quick tests you may instead use
   `PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM` from the debug keystore's certificate.)
3. **Generate the QR** in the **parent app** → **Device Owner setup** (QR icon on the dashboard):
   paste the APK URL and checksum (and optional Wi-Fi), tap **Generate provisioning QR**.
4. On the **factory-reset** device: at the "Hi there / welcome" setup-wizard screen, **tap the
   screen 6 times** to launch the QR reader, then scan the QR. Android downloads and installs the
   APK, verifies the checksum, and sets Guardian as Device Owner.

When provisioning completes, `GuardianDeviceAdminReceiver.onProfileProvisioningComplete` applies
the baseline protections and starts the monitor service.

---

## What Device Owner unlocks (implemented)

- **Silent app hiding** — blocked apps are hidden via `setApplicationHidden` (they simply
  disappear) instead of the accessibility overlay. Reconciled whenever the policy changes.
- **Uninstall protection** — `setUninstallBlocked` on the child app itself.
- **Factory-reset / tamper protection** — user restrictions `DISALLOW_FACTORY_RESET`,
  `DISALLOW_ADD_USER`, `DISALLOW_SAFE_BOOT`.
- **Lock-task allow-list** — the child app is permitted to pin itself (kiosk) for future use.

The child reports `ownerMode = DEVICE_OWNER` in `status/current`, which the parent dashboard
shows in the device header.

## Removing Device Owner

The child's `DeviceOwnerManager.releaseOwnership()` calls `clearDeviceOwnerApp` (un-hides apps
first). During development, prefer the adb `remove-active-admin` command above.

## Notes / limitations

- Provisioning requires a **factory-reset** device; there is no in-place upgrade path.
- The QR's checksum must match the **exact** hosted APK's signing certificate, or provisioning
  is rejected.
- Hiding the child app's own launcher icon is intentionally **not** enabled by default, so the
  transparency/consent screen stays reachable.
