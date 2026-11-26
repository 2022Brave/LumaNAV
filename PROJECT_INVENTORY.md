# LumaNAV — Project Inventory & Source Recovery Guide

**Snapshot Identifier:** `LumaNAV v1.0` (`versionCode = 1`, `applicationId = com.aistudio.auvionyx.kxvrqm`)  
**Milestone:** Visual Reconstruction (1280×800 Landscape Cockpit) + Voice-First Architecture (`Auvionyx Voice`) + ColorOS "Living Flow" UI/Motion Integration  
**Export Date:** 2026-11-17  
**Platform:** Native Android (Kotlin + Jetpack Compose + Material 3)

---

## 1. Complete Directory Tree (Exported Archive)

```text
auvionyx-source-export/
├── .env.example
├── .gitignore
├── PROJECT_INVENTORY.md
├── build.gradle.kts
├── gradle.properties
├── metadata.json
├── settings.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    ├── .gitignore
    ├── build.gradle.kts
    ├── google-services.json.example          # Sanitized template replacing live google-services.json
    ├── proguard-rules.pro
    └── src/
        ├── androidTest/
        │   └── java/com/example/
        │       └── ExampleInstrumentedTest.kt
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/
        │   │   ├── MainActivity.kt
        │   │   ├── core/
        │   │   │   ├── AppCheckInit.kt
        │   │   │   └── AuvionyxNotificationListener.kt
        │   │   ├── data/
        │   │   │   ├── ai/
        │   │   │   │   └── AuvionyxAiRepository.kt
        │   │   │   ├── maps/
        │   │   │   │   └── DestinationSearchRepository.kt
        │   │   │   ├── media/
        │   │   │   │   └── MediaSessionBridge.kt
        │   │   │   ├── model/
        │   │   │   │   ├── AuvionyxModels.kt
        │   │   │   │   └── ServiceState.kt
        │   │   │   ├── preferences/
        │   │   │   │   └── AuvionyxPreferences.kt
        │   │   │   ├── sensor/
        │   │   │   │   └── TelemetryProvider.kt
        │   │   │   └── voice/
        │   │   │       ├── AuvionyxVoiceEngine.kt
        │   │   │       ├── VoiceCommandParser.kt
        │   │   │       └── VoiceModels.kt
        │   │   ├── ui/
        │   │   │   ├── components/
        │   │   │   │   ├── AlbumArtworkArt.kt
        │   │   │   │   ├── AuvionyxVoiceOverlay.kt
        │   │   │   │   ├── CockpitGauges.kt
        │   │   │   │   ├── InteractiveMapCanvas.kt
        │   │   │   │   ├── LiquidGlass.kt
        │   │   │   │   ├── LivingFlowMotion.kt
        │   │   │   │   └── VelocityABranding.kt
        │   │   │   ├── screens/
        │   │   │   │   ├── AodScreen.kt
        │   │   │   │   ├── AppsDrawerScreen.kt
        │   │   │   │   ├── BootSplashScreen.kt
        │   │   │   │   ├── DriveDashboardScreen.kt
        │   │   │   │   ├── HudScreen.kt
        │   │   │   │   ├── MapsScreen.kt
        │   │   │   │   ├── MusicScreen.kt
        │   │   │   │   ├── PulseScreen.kt
        │   │   │   │   ├── SettingsScreen.kt
        │   │   │   │   ├── VehicleScreen.kt
        │   │   │   │   └── VoiceScreen.kt
        │   │   │   └── theme/
        │   │   │       ├── Color.kt
        │   │   │       ├── Theme.kt
        │   │   │       └── Type.kt
        │   │   └── viewmodel/
        │   │       └── AuvionyxViewModel.kt
        │   └── res/
        │       ├── drawable/
        │       │   ├── auvionyx_launcher_fg.png
        │       │   ├── auvionyx_velocity_a.png
        │       │   ├── ic_launcher_background.xml
        │       │   └── ic_launcher_foreground.xml
        │       ├── font/
        │       │   ├── inter.ttf
        │       │   └── space_grotesk.ttf
        │       ├── mipmap-anydpi-v26/
        │       │   ├── ic_launcher.xml
        │       │   └── ic_launcher_round.xml
        │       ├── mipmap-hdpi/
        │       │   ├── ic_launcher.png
        │       │   └── ic_launcher_round.png
        │       ├── mipmap-mdpi/
        │       │   ├── ic_launcher.png
        │       │   └── ic_launcher_round.png
        │       ├── mipmap-xhdpi/
        │       │   ├── ic_launcher.png
        │       │   └── ic_launcher_round.png
        │       ├── mipmap-xxhdpi/
        │       │   ├── ic_launcher.png
        │       │   └── ic_launcher_round.png
        │       ├── mipmap-xxxhdpi/
        │       │   ├── ic_launcher.png
        │       │   └── ic_launcher_round.png
        │       ├── values/
        │       │   ├── colors.xml
        │       │   ├── firebase_applet_config.xml    # Sanitized placeholder inside export ZIP
        │       │   ├── strings.xml
        │       │   └── themes.xml
        │       └── xml/
        │           ├── backup_rules.xml
        │           └── data_extraction_rules.xml
        └── test/
            └── java/com/example/
                ├── ExampleRobolectricTest.kt
                └── ExampleUnitTest.kt
```

---

## 2. Full List of Included Files & Excluded Items

### Included Files (76 total entries in archive)

**Root & Gradle Configuration**
- `.env.example` — Environment variable guidance for non-Gemini secrets
- `.gitignore` — Standard Android/Gradle/keystore/env ignore rules
- `PROJECT_INVENTORY.md` — This inventory & recovery guide
- `build.gradle.kts` — Top-level Gradle plugin aliases
- `gradle.properties` — JVM heap, parallel/config cache, in-process Kotlin compiler, `googleServices.missing.passthrough=true`
- `metadata.json` — AI Studio application metadata
- `settings.gradle.kts` — Plugin/dependency repositories and root project name (`AUVIONYX`)
- `gradle/libs.versions.toml` — Gradle Version Catalog
- `gradle/wrapper/gradle-wrapper.properties` — Gradle `9.3.1` distribution definition
- `app/.gitignore` — App module build ignore rule
- `app/build.gradle.kts` — App module configuration (`namespace = "com.example"`, `applicationId = "com.aistudio.auvionyx.kxvrqm"`)
- `app/google-services.json.example` — Redacted Firebase client configuration template (replaces live `app/google-services.json`)
- `app/proguard-rules.pro` — ProGuard/R8 rules placeholder

**Kotlin Source (`app/src/main/java/com/example/`) — 35 files**
- `MainActivity.kt`
- `core/AppCheckInit.kt`
- `core/AuvionyxNotificationListener.kt`
- `data/ai/AuvionyxAiRepository.kt`
- `data/maps/DestinationSearchRepository.kt`
- `data/media/MediaSessionBridge.kt`
- `data/model/AuvionyxModels.kt`
- `data/model/ServiceState.kt`
- `data/preferences/AuvionyxPreferences.kt`
- `data/sensor/TelemetryProvider.kt`
- `data/voice/AuvionyxVoiceEngine.kt`
- `data/voice/VoiceCommandParser.kt`
- `data/voice/VoiceModels.kt`
- `ui/components/AlbumArtworkArt.kt`
- `ui/components/AuvionyxVoiceOverlay.kt`
- `ui/components/CockpitGauges.kt`
- `ui/components/InteractiveMapCanvas.kt`
- `ui/components/LiquidGlass.kt`
- `ui/components/LivingFlowMotion.kt`
- `ui/components/VelocityABranding.kt`
- `ui/screens/AodScreen.kt`
- `ui/screens/AppsDrawerScreen.kt`
- `ui/screens/BootSplashScreen.kt`
- `ui/screens/DriveDashboardScreen.kt`
- `ui/screens/HudScreen.kt`
- `ui/screens/MapsScreen.kt`
- `ui/screens/MusicScreen.kt`
- `ui/screens/PulseScreen.kt`
- `ui/screens/SettingsScreen.kt`
- `ui/screens/VehicleScreen.kt`
- `ui/screens/VoiceScreen.kt`
- `ui/theme/Color.kt`
- `ui/theme/Theme.kt`
- `ui/theme/Type.kt`
- `viewmodel/AuvionyxViewModel.kt`

*(Total Kotlin files in `app/src/main`: 35; plus 3 test files = 38 Kotlin files total.)*

**Android Manifest & Resources (`app/src/main/`)**
- `AndroidManifest.xml`
- `res/drawable/auvionyx_launcher_fg.png`
- `res/drawable/auvionyx_velocity_a.png`
- `res/drawable/ic_launcher_background.xml`
- `res/drawable/ic_launcher_foreground.xml`
- `res/font/inter.ttf`
- `res/font/space_grotesk.ttf`
- `res/mipmap-anydpi-v26/ic_launcher.xml`
- `res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `res/mipmap-hdpi/ic_launcher.png`, `ic_launcher_round.png`
- `res/mipmap-mdpi/ic_launcher.png`, `ic_launcher_round.png`
- `res/mipmap-xhdpi/ic_launcher.png`, `ic_launcher_round.png`
- `res/mipmap-xxhdpi/ic_launcher.png`, `ic_launcher_round.png`
- `res/mipmap-xxxhdpi/ic_launcher.png`, `ic_launcher_round.png`
- `res/values/colors.xml`, `firebase_applet_config.xml` (sanitized in ZIP), `strings.xml`, `themes.xml`
- `res/xml/backup_rules.xml`, `data_extraction_rules.xml`

**Tests**
- `app/src/test/java/com/example/ExampleUnitTest.kt`
- `app/src/test/java/com/example/ExampleRobolectricTest.kt`
- `app/src/androidTest/java/com/example/ExampleInstrumentedTest.kt`

### Deliberately Excluded from Export Archive
- **Build & Tooling Caches:** `.gradle/`, `.kotlin/`, `build/`, `app/build/`, `.build-outputs/`, `.aistudio/`
- **Credentials, Signing Keys & Secrets:**
  - `debug.keystore` and `debug.keystore.base64` (never export signing keystores)
  - `.env` (if present locally)
  - Live `app/google-services.json` (replaced with sanitized `app/google-services.json.example`)
  - Live `recaptcha_site_key` value inside `app/src/main/res/values/firebase_applet_config.xml` (replaced with `YOUR_RECAPTCHA_ENTERPRISE_SITE_KEY` inside the ZIP copy)
- **Unreferenced Chat Prompt Media:** `public/assets/aistudio/` (~127 MB of AI Studio chat reference images and `.mp4` screen recordings, including a 112 MB video exceeding GitHub's 100 MB file limit; none of these files are referenced by the Android app).

---

## 3. Framework & Technology Versions

| Component | Version |
| :--- | :--- |
| Gradle Distribution | `9.3.1` |
| Android Gradle Plugin (AGP) | `9.1.1` |
| Kotlin & Compose Compiler Plugin | `2.2.10` |
| KSP (`com.google.devtools.ksp`) | `2.3.5` |
| Foojay Toolchain Resolver | `1.0.0` |
| Google Services Plugin | `4.5.0` |
| Secrets Gradle Plugin | `2.0.1` |
| Java Compatibility | Java `11` (`JavaVersion.VERSION_11`) |
| Android `compileSdk` | `36` (`minorApiLevel = 1`) |
| Android `targetSdk` | `36` |
| Android `minSdk` | `24` |
| Jetpack Compose BOM | `2024.09.00` |
| Firebase BOM | `34.17.0` |

---

## 4. Complete Dependency List (`gradle/libs.versions.toml` & `app/build.gradle.kts`)

### Active Implementation Dependencies
- `androidx.core:core-ktx:1.18.0`
- `androidx.activity:activity-compose:1.10.1`
- `androidx.lifecycle:lifecycle-runtime-ktx:2.8.7`
- `androidx.lifecycle:lifecycle-runtime-compose:2.8.7`
- `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7`
- `androidx.navigation:navigation-compose:2.8.9`
- `androidx.datastore:datastore-preferences:1.1.7`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
- `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2`
- **Compose BOM `2024.09.00`:**
  - `androidx.compose.ui:ui`
  - `androidx.compose.ui:ui-graphics`
  - `androidx.compose.ui:ui-tooling-preview`
  - `androidx.compose.material3:material3`
  - `androidx.compose.material:material-icons-core`
  - `androidx.compose.material:material-icons-extended`
- **Firebase BOM `34.17.0`:**
  - `com.google.firebase:firebase-ai`
  - `com.google.firebase:firebase-appcheck-recaptcha`
  - `com.google.firebase:firebase-appcheck-debug`

### Active Test & Debug Dependencies
- **Unit / Robolectric (`testImplementation`):**
  - `junit:junit:4.13.2`
  - `androidx.test:core:1.6.1`
  - `androidx.test.ext:junit:1.3.0`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2`
  - `org.robolectric:robolectric:4.16.1`
  - `androidx.compose.ui:ui-test-junit4` (via Compose BOM)
- **Instrumented (`androidTestImplementation`):**
  - `androidx.test:runner:1.6.2`
  - `androidx.test.ext:junit:1.3.0`
  - `androidx.test.espresso:espresso-core:3.7.0`
  - `androidx.compose.ui:ui-test-junit4` (via Compose BOM)
- **Debug (`debugImplementation`):**
  - `androidx.compose.ui:ui-tooling`
  - `androidx.compose.ui:ui-test-manifest`

### Pre-Configured Commented Optional Dependencies in `libs.versions.toml` (Not Active in APK)
Defined in `gradle/libs.versions.toml` and commented out in `app/build.gradle.kts`:
- `accompanist-permissions` (`0.37.3`), `camera-*` (`1.5.0`), `room-*` (`2.7.0`), `coil-compose` (`2.7.0`), `retrofit` / `converter-moshi` (`2.12.0`), `okhttp` / `logging-interceptor` (`4.10.0`), `moshi-kotlin` / `moshi-kotlin-codegen` (`1.15.2`), `play-services-location` (`21.3.0`), `firebase-firestore`, `firebase-auth`, `androidx-credentials` (`1.5.0`), `googleid` (`1.1.1`).

---

## 5. Installation & External Setup Instructions

1. **Prerequisites**
   - **JDK 11+** (JDK 17 or 21 recommended for AGP 9.1.1 / Gradle 9.3.1)
   - **Android Studio** (Narwhal / Meerkat or newer with Android SDK 36 installed)
   - **Gradle 9.3.1** (note: `gradlew` scripts are not bundled; either use Android Studio's Gradle sync, install Gradle 9.3.1 locally, or generate the wrapper once with `gradle wrapper --gradle-version 9.3.1`)

2. **Restore Firebase & App Check Configuration**
   - Copy `app/google-services.json.example` to `app/google-services.json` and replace placeholders with your Firebase project's real `google-services.json` downloaded from Firebase Console for package `com.aistudio.auvionyx.kxvrqm`.
   - Enable **Firebase AI Logic** in your Firebase project.
   - Update `app/src/main/res/values/firebase_applet_config.xml` (`recaptcha_site_key`) with your reCAPTCHA Enterprise site key for release builds (debug builds use `DebugAppCheckProviderFactory` — register the debug token printed in Logcat in Firebase Console → App Check).
   - *(Optional independent compile check)*: Because `app/build.gradle.kts` sets `MissingGoogleServicesStrategy.WARN` and `gradle.properties` sets `googleServices.missing.passthrough=true`, the project compiles even before `app/google-services.json` is added, though live `Firebase.ai` calls require valid Firebase config.

3. **Restore Debug Signing Keystore**
   - `app/build.gradle.kts` configures `signingConfigs.debugConfig` pointing to `${rootDir}/debug.keystore` (`storePassword = "android"`, `keyAlias = "androiddebugkey"`, `keyPassword = "android"`).
   - Generate a standard local debug keystore at the repo root if missing:
     ```bash
     keytool -genkey -v -keystore debug.keystore -storepass android \
       -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
       -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
     ```

4. **Initialize Git & Push Outside AI Studio**
   ```bash
   unzip AUVIONYX_SOURCE_EXPORT.zip -d auvionyx
   cd auvionyx
   git init
   git add .
   git commit -m "Initial import of AUVIONYX v1.0"
   ```

---

## 6. Development, Build & Test Commands

Run from the project root (using `gradle` or `./gradlew` after generating the wrapper):

- **Assemble Debug APK:**
  ```bash
  gradle assembleDebug
  ```
- **Compile Kotlin Debug Sources:**
  ```bash
  gradle :app:compileDebugKotlin
  ```
- **Run Local Unit & Robolectric Tests:**
  ```bash
  gradle :app:testDebugUnitTest
  ```
- **Run Android Lint:**
  ```bash
  gradle :app:lintDebug
  ```
- **Assemble Release APK / Bundle (requires release keystore env vars):**
  ```bash
  gradle assembleRelease
  gradle bundleRelease
  ```

---

## 7. Required & Optional Environment Variables

Defined across `.env.example`, `app/build.gradle.kts`, and `AppCheckInit.kt`:

| Variable | Required? | Purpose |
| :--- | :--- | :--- |
| `ANDROID_SDK_ROOT` / `ANDROID_HOME` | Required to build | Path to local Android SDK (or set via Android Studio `local.properties`, ignored by `.gitignore`) |
| `FIREBASE_APPCHECK_DEBUG_TOKEN` | Optional (Debug runtime) | Ignored in `BuildConfig` (`secrets.ignoreList`), optionally passed as Intent extra to `MainActivity` for pre-registered Firebase App Check debug token |
| `KEYSTORE_PATH` | Optional (Release build) | Path to upload `.jks` keystore (`defaults to ${rootDir}/my-upload-key.jks`) |
| `STORE_PASSWORD` | Required for `release` signing | Keystore store password for `signingConfigs.release` |
| `KEY_PASSWORD` | Required for `release` signing | Key password for alias `upload` in `signingConfigs.release` |

*Note:* No `GEMINI_API_KEY` is needed or used; Firebase AI Logic authenticates via Firebase project config + Firebase App Check.

---

## 8. API & Android System Integrations

1. **Firebase AI Logic (`com.google.firebase:firebase-ai`) + App Check**
   - File: `app/src/main/java/com/example/data/ai/AuvionyxAiRepository.kt`, `core/AppCheckInit.kt`
   - Model: `gemini-3.8-flash` with strict JSON `Schema` (`category`, `action`, `parameter`, `confirmationMessage`, `requiresNativeBridge`) used strictly as a natural-language fallback when deterministic local voice command matching does not match.
2. **OSRM Public Driving Route API**
   - File: `DestinationSearchRepository.kt`
   - Endpoint: `https://router.project-osrm.org/route/v1/driving/{lon1},{lat1};{lon2},{lat2}?overview=full&geometries=geojson&steps=true`
   - Fetches real driving polylines, distance, duration, ETA, and upcoming turn maneuvers.
3. **Open-Meteo Current Weather API**
   - File: `DestinationSearchRepository.kt`
   - Endpoint: `https://api.open-meteo.com/v1/forecast?latitude=...&longitude=...&current=temperature_2m,weather_code`
   - Fetches live temperature and WMO weather conditions for the current GPS fix.
4. **Android Platform Services**
   - **Location & Geocoding:** `LocationManager` (GPS + Network providers) in `TelemetryProvider.kt`; `Geocoder` (forward + reverse) in `DestinationSearchRepository.kt`.
   - **Voice & Audio:** `SpeechRecognizer`, `TextToSpeech`, and `AudioManager` in `AuvionyxVoiceEngine.kt`.
   - **External Navigation & Media Mirroring:** `NotificationListenerService` (`AuvionyxNotificationListener.kt`) mirrors live Google Maps turn-by-turn notifications and connects `MediaSessionManager` / `MediaController` (`MediaSessionBridge.kt`) to active media apps (Spotify, YouTube Music, etc.).
   - **Intent Receivers:** `MainActivity` handles `geo:`, `google.navigation:`, and `ACTION_SEND` (`text/plain`) links shared from external map apps.

---

## 9. Known Limitations

- **Custom Vector Map Canvas vs. Google Maps SDK Tiles:** `InteractiveMapCanvas.kt` draws a high-contrast 3D perspective AMOLED vector map on Compose `Canvas` (rendering live OSRM road polylines when GPS/route coordinates are active, and falling back to a labeled demo vector corridor when idle). No Google Maps SDK API key or map tile SDK is bundled.
- **Sample/Demo Data Fallbacks:** When no live GPS fix, OSRM route, or vehicle telemetry bridge is active, Traffic Pulse corridor alerts/predictions (`AuvionyxSampleData`), EV battery/range/tire values, and showcase media tracks operate in demo/sample mode (badged in the UI as Demo Telemetry).
- **Privileged Android Permissions Needed at Runtime:** Live Google Maps notification mirroring and cross-app `MediaSessionManager` control require the user to grant **Notification Access** to `AuvionyxNotificationListener` in Android System Settings.
- **No `gradlew` Wrapper Binary Included:** Only `gradle/wrapper/gradle-wrapper.properties` (`9.3.1`) is present; run `gradle wrapper` or open in Android Studio to generate `gradlew`/`gradlew.bat` and `gradle-wrapper.jar` locally.

---

## 10. Snapshot & Architectural Summary

- **Design System:** AMOLED Obsidian Black (`#000000`), Electric Cyan (`#00D9F5`), Ice Blue (`#63BFFF`), Deep Blue (`#1267D5`), Glass White (`#FFFFFF`), custom Space Grotesk + Inter fonts (`res/font/`), adaptive launcher icon (`res/mipmap-*`).
- **UI & Motion Layer:** `LiquidGlass.kt` + `LivingFlowMotion.kt` (ColorOS Living Flow laminar spring curves, Fluid Cloud dynamic alert/turn pill, container morphing, water-drop rail indicator, touch-activated quick dock, and idle-fade cockpit transitions).
- **Modules:** Home Cockpit (`DriveDashboardScreen`), Maps (`MapsScreen`), Music (`MusicScreen`), HUD (`HudScreen`), Traffic Pulse (`PulseScreen`), Auvionyx Voice (`VoiceScreen` + `AuvionyxVoiceOverlay`), AOD (`AodScreen`), Vehicle (`VehicleScreen`), Apps (`AppsDrawerScreen`), Settings (`SettingsScreen`).
