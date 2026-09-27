# Building the APKs

The project is ready to build. A Gradle **wrapper** (8.9), `local.properties` (pointing at your
SDK), and **placeholder** `google-services.json` files are already in place, and `compileSdk`/
`targetSdk` are set to **34** (the platform installed on this machine, so no SDK download is
needed).

## Easiest: Android Studio
1. **File → Open** → select `D:\Android`.
2. Let it sync (it uses its bundled JDK and offers to install anything missing).
3. **Build → Build App Bundle(s) / APK(s) → Build APK(s)** — or pick the `parent` / `child` run
   configuration and press Run.

## Command line (from a normal terminal — PowerShell or cmd)
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd D:\Android
.\gradlew.bat :parent:assembleDebug :child:assembleDebug
```

Output APKs:
```
parent\build\outputs\apk\debug\parent-debug.apk
child\build\outputs\apk\debug\child-debug.apk
```

Install on a connected device/emulator:
```powershell
$adb = "$env:USERPROFILE\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb install -r parent\build\outputs\apk\debug\parent-debug.apk
& $adb install -r child\build\outputs\apk\debug\child-debug.apk
```

## Before it actually *works* at runtime
The bundled `google-services.json` are **placeholders** — the apps compile and install, but
Auth/Firestore/FCM will not function until you replace them with your real Firebase config
(see [FIREBASE_SETUP.md](FIREBASE_SETUP.md)) and enable Email/Password + Anonymous auth.

## Notes
- **compileSdk 34 vs 35:** set to 34 to match the installed SDK platform. To restore the API-35
  target, change `compileSdk`/`targetSdk` to 35 in the three `build.gradle.kts` files; Android
  Studio will offer to install the android-35 platform.
- First build downloads AGP/Kotlin/dependencies from Maven — needs internet and a few minutes.
- Run the unit tests with `.\gradlew.bat test` (see [TESTING.md](TESTING.md)).
