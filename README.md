# FitLog

A lightweight **gym workout tracker** for Android. Plan your training days, log
weights, sets and reps, run a built-in rest timer, and watch your progress — all
offline-first, with **no ads and no tracking**. Optional anonymous cloud sync
keeps your data backed up across devices.

- **Package:** `fit.log`
- **Version:** 1.0 (`versionCode` 1)
- **Repository:** https://github.com/hamujuls/FitLog
- **Languages:** German & English (auto-selected from the device locale)

---

## Features

- 📋 **Training plans** — organize exercises into workout days (e.g. Push / Pull / Legs).
- 🏋️ **Set logging** — track weight, sets, reps and pauses per exercise, with warm-up flags.
- ⏱️ **Rest timer** — countdown between sets with a vibration cue when time is up.
- 📈 **Progress** — your numbers persist locally so you always pick up where you left off.
- ☁️ **Optional cloud sync** — back up to Firebase/Firestore via an anonymous random ID (no name, no email).
- 💾 **Local backup / restore** — export and import your data as a JSON file via the native file picker.
- 🌙 **Dark, edge-to-edge UI** — clean dark theme tuned for the gym.
- 🔌 **Offline-first** — works fully without a network connection.
- 🚫 **No ads, no analytics, no third-party tracking SDKs.**

---

## Architecture

FitLog is a thin **native Android shell around a WebView**. The UI is a single-page
HTML/CSS/JS app shipped in the APK's `assets/` folder; the native `MainActivity`
just hosts the WebView, picks the language file from the device locale, and exposes
two small JavaScript bridges.

```
MainActivity (android.app.Activity, framework-only — no AndroidX)
  └── WebView  (JavaScript + DOM storage + database enabled, edge-to-edge)
        ├── German locale  →  file:///android_asset/index.html
        └── otherwise      →  file:///android_asset/index_en.html
```

**JS ↔ native bridges**

| JS object         | Purpose                                                    |
|-------------------|-----------------------------------------------------------|
| `AndroidBackup`   | `saveBackup(data, filename)` → write JSON via `ACTION_CREATE_DOCUMENT` |
| `AndroidFeedback` | `vibrate(ms)` → rest-timer haptic cue                      |

- State is stored client-side in `localStorage` under the key `gym_tracker_state_v2`.
- Cloud sync (when enabled) talks to Firebase/Firestore from inside the WebView.
- `index.html` is the German/canonical build; `index_en.html` is the English copy
  served to non-German locales (`index_de.html` is a localized source kept in sync).

### Tech stack

| Component     | Value                                            |
|---------------|--------------------------------------------------|
| Language      | Java 17                                          |
| Android SDK   | `minSdk` 23 · `targetSdk` 35 · `compileSdk` 35   |
| Build         | Android Gradle Plugin 8.7.3 (build with JDK 17)  |
| Dependencies  | none — Android framework only (`WebView`)        |

---

## Project structure

```
FitLog_1.0/
├── app/
│   ├── build.gradle                      # module config (namespace, SDKs, version)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/fit/log/MainActivity.java
│       ├── assets/                       # the web app: index.html, index_en.html,
│       │                                 #   index_de.html, plan.json
│       └── res/                          # launcher icon, themes, strings (en/de)
├── scripts/
│   ├── install-and-run-on-phone.sh       # uninstall → installDebug → launch
│   └── auto-install-hook.sh              # in-place install + launch (Stop-hook helper)
├── build.gradle                          # root project (AGP plugin)
└── settings.gradle
```

---

## Building & running

> **Note:** this project has **no `./gradlew` wrapper**, and on this machine neither
> `gradle` nor a JDK 17 is on the default `PATH`. Set them explicitly first.

```bash
cd FitLog_1.0

# Toolchain (JDK 17 + Gradle)
export JAVA_HOME="/home/ju/.sdkman/candidates/java/17.0.19-tem"
export PATH="$JAVA_HOME/bin:/home/ju/.sdkman/candidates/gradle/current/bin:$PATH"
export GRADLE="/home/ju/.sdkman/candidates/gradle/current/bin/gradle"

# Build + install the debug APK on a connected phone
"$GRADLE" :app:installDebug

# Launch it
adb shell monkey -p fit.log -c android.intent.category.LAUNCHER 1
```

### Helper scripts

- **`scripts/install-and-run-on-phone.sh`** — full reset flow: `adb uninstall`,
  then `:app:installDebug`, then launch. This **wipes app data** and re-triggers
  onboarding, useful for clean test runs.
- **`scripts/auto-install-hook.sh`** — **in-place** `:app:installDebug` + launch
  (no uninstall, so test data survives). Used as a Claude Code `Stop` hook to
  auto-deploy to the phone after each change; safe to run standalone too.

---

## Permissions

| Permission                | Why                                            |
|---------------------------|------------------------------------------------|
| `INTERNET`                | Optional cloud sync                            |
| `ACCESS_NETWORK_STATE`    | Detect connectivity for sync                   |
| `VIBRATE`                 | Rest-timer haptic cue                          |
| `WRITE_EXTERNAL_STORAGE`  | Legacy compatibility only (`maxSdkVersion="28"`) |

No location, camera, contacts, or advertising permissions are requested.

---

## Privacy

FitLog is **local-first**: your workout data lives on your device. Cloud sync is
**off unless you enable it**, and when enabled it uses an anonymous random user ID
— no account, name, or email is collected. The full privacy policy
(Datenschutzerklärung, DE/EN) is published alongside the Play Store assets.

---

## License

Personal project by Julian Hackermüller. See the repository for license details.
