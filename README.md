## LUMANAV

**Find your way. Feel the drive.**

LumaNav is a personal Android automotive cockpit and navigation project built with Kotlin, Jetpack Compose and Material 3.

## Project snapshot

- Snapshot: v1.0
- Android application ID: `com.aistudio.auvionyx.kxvrqm`
- Namespace: `com.example`
- Minimum SDK: 24
- Target SDK: 36
- Compile SDK: 36
- Gradle: 9.3.1
- Kotlin: 2.2.10
- UI: Jetpack Compose + Material 3

See [`PROJECT_INVENTORY.md`](PROJECT_INVENTORY.md) for the detailed source inventory, integrations, setup requirements and known limitations.

## Build prerequisites

- Android Studio with Android SDK 36
- JDK 17 or newer
- Gradle 9.3.1

The Gradle wrapper scripts and wrapper JAR are not included in this source snapshot. Open the project in Android Studio and sync with Gradle, or generate a wrapper locally with:

```bash
gradle wrapper --gradle-version 9.3.1
```

## Firebase setup

The repository intentionally contains `app/google-services.json.example`, not a live Firebase configuration.

For local development, copy the example to `app/google-services.json` and replace placeholders with the configuration for your own Firebase project. Do not commit live credentials or private keys.

## Important implementation notes

- The current map view is a custom Compose canvas; Google Maps SDK tiles are not included in this snapshot.
- OSRM is used for driving-route requests and Open-Meteo for weather, as documented in the inventory.
- Voice uses Android `SpeechRecognizer` and `TextToSpeech` APIs.
- External media control and metadata use Android media-session APIs and require Notification Access.
- Some dashboard/vehicle/traffic content remains demonstration data when live services are unavailable.
- This is a direct Android app project. It is not yet an Android Auto app and does not replace the Android Auto projection interface.

## Brand

- Obsidian Black: `#000000`
- Electric Cyan: `#00D9F5`
- Ice Blue: `#63BFFF`
- Deep Blue: `#1267D5`
- Glass White: `#FFFFFF`

