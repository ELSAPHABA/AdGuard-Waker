# AdGuard Waker

[한국어 README](README.md)

A lightweight, native Android automation app that sends the official AdGuard automation broadcast intents without Tasker, root, a continuously running foreground service, or network permissions. Licensed under AGPL-3.0; see [LICENSE](LICENSE).

## Features

- Independently scheduled AdGuard `start` and `update` actions with intervals of **1–720 minutes**, entered using the numeric keyboard or a wheel picker.
- Automatic saving: checkboxes and confirmed wheel changes save immediately; text changes save after a 650 ms debounce. Pending edits are saved when leaving the app. Invalid/incomplete intervals keep the last valid saved value.
- Default intervals: 5 minutes for `start` and 60 minutes for `update`. Automation starts disabled until the user enables it.
- Restoration after device boot, package replacement, and exact-alarm permission changes. One AlarmManager alarm serves both tasks, with a periodic JobScheduler job to assist recovery.
- Optional exact alarm when permission is granted, otherwise an inexact alarm. Missed repetitions are not sent as a backlog.
- No Waker-created notification or continuously running service. AdGuard and Android VPN indicators are independent of Waker.
- Supports the current AdGuard AutomationReceiver and a legacy compatibility path, sending `quiet=true` to suppress AdGuard automation toasts.
- Stores the automation password in app-private SharedPreferences, excluded from backup, **without additional encryption**. The password is sent to AdGuard in the command broadcast when required.
- Status card for automation, receiver availability, next scheduled attempts, last submission attempt, and battery optimization information.
- **English and Korean UI.** A language selector is at the bottom of the main screen: **System default (automatic)**, **한국어**, or **English**. Automatic selection chooses Korean only when the device system language is Korean; all other system languages use English. A manual choice overrides the system and persists across app restarts. Changing the selection immediately recreates the screen. Status, dialogs, validation feedback, and date/time formatting follow the selected language.
- Compatible with Android 12+ (minimum API 31, target/compile API 36). Android 17 and vendor-specific behaviors still require device testing.

## Installation and usage

1. Enable external app automation in AdGuard for Android and obtain the automation password.
2. Open Waker, enter the password, then use **Start protection now** and **Check for updates now** to confirm AdGuard reacts to the broadcasts.
3. Enable automation and configure each interval. Changes are saved automatically; **Save and apply settings** remains available for manual validation.
4. Grant the optional **Alarm and reminder permission** for more precise schedules, and check the device's auto-start and battery restrictions. AdGuard itself may also need exclusion from battery restrictions.
5. At the **bottom** of the Waker screen, choose the app language. The default is automatic.

## Android scheduling limitations

An interval of one minute is a configuration request, **not** a guarantee that Android will execute it every minute. Doze, manufacturer restrictions, forced-stop state, exact-alarm permission revocation, or VPN permission can delay or prevent execution. Waker submits a broadcast request; it cannot independently prove that AdGuard started protection or completed an update.

For Samsung Galaxy / One UI, the **Battery optimization settings** button first attempts a package-specific battery-details settings intent. Because this system action is not a guaranteed public cross-OEM API, it can fall back to the Waker app-information page.

## Build

Requires **JDK 17**, **Gradle 8.13**, **Android SDK Platform 36**, **Build Tools 35.0.0**, **Android Gradle Plugin 8.13.2**, and **Kotlin 2.4.20**.

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleOptimized :app:lintDebug :app:lintOptimized
```

| Variant | APK output | R8 | Signing |
|---|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | Off | Ephemeral debug key |
| Optimized test | `app/build/outputs/apk/optimized/app-optimized.apk` | On | Ephemeral debug key |
| Stable release | `app/build/outputs/apk/release/app-release.apk` | On | Persistent key supplied in private environment variables |

GitHub Actions builds debug and optimized test APKs for pull requests. To create the separately signed **AdGuard-Waker-release-stable** artifact on the `main` branch, configure the four GitHub Actions repository secrets listed below. Do **not** publish your keystore or passwords.

## Stable release signing and in-place updates

Android updates require the same application ID, compatible signing certificate, and a non-decreasing version code (normally increasing). GitHub-hosted debug signing keys may change across workflow runs, so previous debug/optimized test APKs cannot reliably update each other. A newly generated stable signing certificate will also not match an existing debug-signed install, meaning that **one initial reinstall may be necessary**. Uninstalling removes Waker's stored settings; there is currently no in-app export.

Generate a persistent signing key on a trusted computer:

```sh
keytool -genkeypair -v -keystore adguard-waker-release.jks -storetype PKCS12 -alias adguard-waker -keyalg RSA -keysize 3072 -validity 10000
```

Back up the keystore and its password securely, outside the repository. Base64-encode the full keystore (`base64 -w0 adguard-waker-release.jks` on Linux, or `[Convert]::ToBase64String([IO.File]::ReadAllBytes("adguard-waker-release.jks"))` in PowerShell). Under **GitHub → Settings → Secrets and variables → Actions**, create:

| Repository secret | Value |
|---|---|
| `WAKER_SIGNING_KEYSTORE_BASE64` | Base64-encoded keystore bytes |
| `WAKER_SIGNING_STORE_PASSWORD` | Keystore password |
| `WAKER_SIGNING_KEY_ALIAS` | `adguard-waker` |
| `WAKER_SIGNING_KEY_PASSWORD` | Key password |

On the `main` branch, the workflow builds the stable release APK only when all four secrets are configured. The CI version code is `10000 + GITHUB_RUN_NUMBER` and advances for subsequent runs. Local builds use their configured default version unless overridden by environment variables.

**Use only the persistently signed Release APK for later in-place updates.** Losing the signing key prevents signing compatible updates for existing installs.

## Manual test checklist

- Korean system language → Korean UI; English, Japanese, French or other system languages → English UI.
- Override language to Korean or English, relaunch the app, switch back to automatic, and confirm settings/intervals are preserved.
- Verify all labels, dialogs, errors, status card text, and timestamps are localized; check both light and dark themes.
- Validate 1 and 720 minutes, reject 0 and 721+, test wheel cancel/confirm and auto-save.
- Confirm individual start/update toggles, alarm permission changes, Doze behavior, post-reboot recovery, and app update behavior.
- On Galaxy / One UI, verify the battery-settings deep link and fallback.
- Compare release APK certificate fingerprints before/after updating; do not confuse a signing mismatch with a version-code mismatch.

## References

- [AdGuard external automation](https://adguard.com/kb/ko/adguard-for-android/solving-problems/tasker/)
- [Android alarms](https://developer.android.com/develop/background-work/services/alarms)
- [Android Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
