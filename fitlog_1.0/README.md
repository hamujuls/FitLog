# FitLog

A lightweight **gym workout tracker** for Android. Plan your training days, log
weights, sets and reps, run a built-in rest timer, and watch your progress — all
offline-first, with **no ads and no tracking**. Optional anonymous cloud sync
keeps your data backed up across devices.

- **Package:** `fit.log`
- **Version:** 1.0
- **License:** [GPL-3.0](LICENSE)
- **Languages:** German & English (auto-selected from the device locale)

Contributions are welcome — see [Contributing](#contributing).

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
hosts the WebView, picks the language file from the device locale, and exposes two
small JavaScript bridges.

```
MainActivity (android.app.Activity, framework-only — no AndroidX)
  └── WebView  (JavaScript + DOM storage + database enabled, edge-to-edge)
        ├── German locale  →  file:///android_asset/index.html
        └── otherwise      →  file:///android_asset/index_en.html
```

**JS ↔ native bridges**

| JS object         | Purpose                                                               |
|-------------------|-----------------------------------------------------------------------|
| `AndroidBackup`   | `saveBackup(data, filename)` → write JSON via `ACTION_CREATE_DOCUMENT` |
| `AndroidFeedback` | `vibrate(ms)` → rest-timer haptic cue                                  |

- State is stored client-side in `localStorage` under the key `gym_tracker_state_v2`.
- Cloud sync (when enabled) talks to Firebase/Firestore from inside the WebView.
- `index.html` is the German/canonical build; `index_en.html` is the English copy
  served to non-German locales. `index_de.html` is a localized source kept in sync.

### Tech stack

| Component     | Value                                            |
|---------------|--------------------------------------------------|
| Language      | Java 17                                          |
| Android SDK   | `minSdk` 23 · `targetSdk` 35 · `compileSdk` 35   |
| Build         | Gradle (wrapper included) · Android Gradle Plugin 8.7.3 |
| Dependencies  | none — Android framework only (`WebView`)        |

---

## Project structure

```
FitLog/
├── app/
│   ├── build.gradle                      # module config (namespace, SDKs, version)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/fit/log/MainActivity.java
│       ├── assets/                       # the web app: index.html, index_en.html,
│       │                                 #   index_de.html, plan.json
│       └── res/                          # launcher icon, themes, strings (en/de)
├── build.gradle                          # root project (AGP plugin)
├── settings.gradle
├── gradlew / gradlew.bat / gradle/       # Gradle wrapper
└── LICENSE
```

---

## Building

### Prerequisites

- **JDK 17**
- **Android SDK** (API 35). Either install [Android Studio](https://developer.android.com/studio),
  or set the SDK location for a command-line build.

### Clone & configure the SDK location

```bash
git clone https://github.com/hamujuls/FitLog.git
cd FitLog
```

Tell Gradle where your Android SDK is, by **either**:

- creating a `local.properties` file in the project root:
  ```properties
  sdk.dir=/path/to/your/Android/Sdk
  ```
- **or** exporting `ANDROID_HOME` / `ANDROID_SDK_ROOT` in your environment.

> `local.properties` is intentionally git-ignored — it is machine-specific.

### Build & install

The Gradle wrapper is included, so no separate Gradle install is needed:

```bash
# Build the debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Or build and install onto a connected device/emulator
./gradlew installDebug
```

Then launch FitLog from the app drawer (or `adb shell monkey -p fit.log -c android.intent.category.LAUNCHER 1`).

The easiest path overall is to open the project folder in **Android Studio** and
press **Run** ▶.

---

## Contributing

Pull requests are welcome! The app's behaviour lives almost entirely in the web
layer under `app/src/main/assets/`:

- **`index.html`** — German / canonical source. Make UI/logic changes here first.
- **`index_en.html`** — English build. Keep it in sync with `index.html`.
- **`index_de.html`** — localized German source.

The native side (`MainActivity.java`) is intentionally minimal — touch it only for
platform integration (file picker, vibration, WebView config).

Suggested workflow:

1. Fork the repo and create a feature branch.
2. Make your change; if you edit one `index*.html`, mirror it in the others.
3. Build (`./gradlew assembleDebug`) and test on a device or emulator.
4. Open a pull request describing the change.

Please keep the project's spirit: **offline-first, no ads, no tracking**.

---

## Permissions

| Permission                | Why                                              |
|---------------------------|--------------------------------------------------|
| `INTERNET`                | Optional cloud sync                              |
| `ACCESS_NETWORK_STATE`    | Detect connectivity for sync                     |
| `VIBRATE`                 | Rest-timer haptic cue                            |
| `WRITE_EXTERNAL_STORAGE`  | Legacy compatibility only (`maxSdkVersion="28"`) |

No location, camera, contacts, or advertising permissions are requested.

---

## Privacy

FitLog is **local-first**: your workout data lives on your device. Cloud sync is
**off unless you enable it**, and when enabled it uses an anonymous random user ID
— no account, name, or email is collected.

---

## License

Released under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) for
the full text.
