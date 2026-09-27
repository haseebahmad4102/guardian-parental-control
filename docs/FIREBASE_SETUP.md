# Firebase setup

The project will not build until each app module has a real `google-services.json`. Follow
these steps once; it takes ~10 minutes.

## 1. Create the Firebase project
1. Go to <https://console.firebase.google.com> → **Add project**.
2. Name it e.g. `guardian-family`. Google Analytics is optional.

## 2. Register BOTH Android apps
In **Project settings → General → Your apps → Add app → Android**, register each package
name exactly (they must match the `applicationId` in each module's `build.gradle.kts`):

| App    | Package name                   |
|--------|--------------------------------|
| Parent | `com.example.guardian.parent`  |
| Child  | `com.example.guardian.child`   |

- **Debug signing SHA-1**: not required for Email/Password auth or Firestore. Add it later
  only if you enable Google Sign-In or App Check.

## 3. Add the config file to each module
Download `google-services.json` (it can contain **both** client entries once both apps are
registered) and place a copy at:

```
parent/google-services.json
child/google-services.json
```

`google-services.json` is git-ignored on purpose — every developer supplies their own. The
committed `*.template` files show the expected shape.

## 4. Enable the products
- **Authentication → Sign-in method → Email/Password → Enable.** (Parent accounts.)
- **Authentication → Sign-in method → Anonymous → Enable.** (Child devices authenticate
  anonymously during pairing so the security rules can bind each device to its own uid.)
- **Firestore Database → Create database → Production mode**, pick a region close to you.
- **Cloud Messaging**: enabled by default; used from Phase 5 for the command channel.
- **Storage**: enable when Phase 3 needs it (not required for Phase 1).

## 5. Deploy the security rules
The starter rules live in `firebase/firestore.rules`.

```bash
npm install -g firebase-tools
firebase login
firebase use --add            # select your project, alias it "default"
firebase deploy --only firestore:rules
```

> These Phase 1 rules restrict all device data to the authenticated parent. Child-device
> write access is added in Phase 2/3 (see the comment block in `firestore.rules`).

## 6. (Optional) Local emulator for tests
```bash
firebase init emulators       # select Auth + Firestore
firebase emulators:start
```
The integration tests (Phase 3+) target the Firestore emulator.
