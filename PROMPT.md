# chime clock


## Requirements

* Android Sdk version 23 project.
* Main features: hourly and half hourly chime, and settings
* hourly chime is dings of the count of hourly
* half hourly chime is single ding
* optional long chime before the dings for half and full hour
* low volume for night 11 pm to 6 pm
* everything is configurable
* setting UI has a 'Test', where user can select time + half-hour and test.
* absolute minimal, No features planned before this.
* target smallest apk size, and using minimal battery user.

### Secondary requirements

* no tests, do not put any test dependencies


## Plan

1. **Project Cleanup & Optimization**:
    * Remove all test dependencies and generated test files from `app/build.gradle.kts` and `src/`.
    * Enable `minifyEnabled` and `shrinkResources` in `build.gradle.kts` to minimize APK size.
2. **Audio Assets**:
    * Add minimal `ding.ogg` and `long_chime.ogg` files to `app/src/main/res/raw`.
3. **Core Chime Service**:
    * Create a `ChimeReceiver` (BroadcastReceiver) to handle scheduled alarms.
    * Use `AlarmManager` for precise hourly and half-hourly triggers.
    * Implement audio playback logic using `MediaPlayer` or `SoundPool` (SoundPool preferred for minimal latency and battery).
4. **Configuration Management**:
    * Use `SharedPreferences` to store settings: hourly/half-hourly toggles, long chime toggle, night mode hours, and volume levels.
5. **Settings UI**:
    * Implement `MainActivity` with a minimal layout.
    * Add a "Test" feature to simulate chimes for any given time.
6. **System Integration**:
    * Handle `BOOT_COMPLETED` to ensure chimes persist after a device restart.
    * Ensure minimal battery usage by avoiding long-running background services and using efficient wake-locks only when playing audio.
