# Build Instructions

## Prerequisites

- JDK 17 or newer (JDK 21 is used in the reference environment).
- Android SDK with platform 34 and build-tools 34.0.0.
- Gradle 8.7 (the project includes a wrapper; a system Gradle also works).
- Internet access on the first build to download dependencies.

Set `sdk.dir` in `local.properties`:

```properties
sdk.dir=/path/to/android-sdk
```

Optional Supabase configuration (see [SUPABASE_SETUP.md](SUPABASE_SETUP.md)) can also go in
`local.properties`, `gradle.properties`, or environment variables:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_ANON_KEY
```

Without these values the app runs in local-only mode and never contacts the network.

## Build

```bash
cd pocket-ledger
./gradlew assembleDebug
```

Reference environment (Kali aarch64, Gradle 8.7 installed outside the wrapper):

```bash
ANDROID_HOME=/home/kali/android-sdk \
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-arm64 \
/home/kali/gradle-8.7/bin/gradle assembleDebug \
  -Pandroid.aapt2FromMavenOverride=/home/kali/android-sdk/build-tools/34.0.0/aapt2
```

The `aapt2FromMavenOverride` flag is only needed when the Maven `aapt2` binary cannot run on the
host architecture (for example aarch64 without emulation).

Output: `app/build/outputs/apk/debug/app-debug.apk` (debug builds use the
`com.pocketledger.debug` application ID).

## Install and run

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.pocketledger.debug/com.pocketledger.activities.SplashActivity
```

On Android 13+ the app requests notification permission so meal-generation notices can appear.

## Tests and checks

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

Instrumentation tests require a connected emulator/device:

```bash
./gradlew connectedDebugAndroidTest
```

## Release build

Release signing is intentionally not configured. Add a signing config in `app/build.gradle`
before producing a release APK. `minifyEnabled` and `shrinkResources` are disabled by default so
the first release build is predictable.
