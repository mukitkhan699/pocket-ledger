# Pocket Ledger v1.0 - Build Instructions

## Prerequisites
- Java 17 (JDK 17)
- Android SDK (API 34)
- Gradle 8.5+

## Supabase Configuration
Add to `local.properties`:
```properties
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

## Build Commands
```bash
# Generate gradle wrapper (if not present)
gradle wrapper --gradle-version 8.5

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Run lint
./gradlew lintDebug

# Run tests
./gradlew testDebugUnitTest
```

## Expected Output
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk` (unsigned)

## Supabase Setup
1. Create Supabase project
2. Run migration: `supabase/migrations/0001_init.sql`
3. Enable Email auth in Supabase Auth settings
4. Add URL and Anon Key to local.properties

## Version Info
- versionCode: 1
- versionName: 1.0.0
