# Battery Notifier

Battery Notifier is a simple Android app that sends alerts when your device battery drops to configured levels.

## Features

- Add and remove custom battery warning levels.
- Pause or resume battery monitoring.
- Receive background battery alerts through notifications.

## Requirements

- Android Studio or the Android Gradle plugin toolchain.
- Android SDK 36.
- Android 14 or newer device or emulator.

## Build

```bash
./gradlew :app:assembleDebug
```

## Test

```bash
./gradlew :app:testDebugUnitTest
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
