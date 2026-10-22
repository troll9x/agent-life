# FND-01 Android toolchain

Open `android/` (the directory containing `settings.gradle.kts`) in Android Studio. Let Studio sync Gradle with its bundled JDK, or set the Gradle JDK to a JDK 17 or newer supported by Gradle 9.6. Install Android SDK Platform 37.0 and Build Tools 36.0.0. On the command line, set `JAVA_HOME` and `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to your local installations, then run `cd android; .\gradlew.bat :app:assembleDebug`. Do not commit `local.properties`.

| Component | Pinned value | Reason |
| --- | --- | --- |
| Gradle wrapper | 9.6.0 | Required by AGP 9.4.0 |
| Android Gradle plugin | 9.4.0 | Stable release supports API 37 and Gradle 9.6.0 |
| Kotlin JVM and Compose compiler plugin | 2.4.21 | Stable Kotlin release, fixed in version catalog |
| Build JDK used here | Android Studio JBR 25.0.3 | Installed locally; Java/Kotlin bytecode targets 17 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 28 | SDK 37 installed; Play submissions require target API 36; min 28 is the architecture decision |
| Compose BOM / Activity Compose | 2026.09.00 / 1.13.0 | Pinned stable UI dependencies |
| Room | 2.8.5 | Pinned for later data task; no Room schema or runtime added in FND-01 |

AGP 9 provides built-in Kotlin for Android modules. The Kotlin JVM plugin applies only to `:domain`; the Compose compiler plugin applies to `:app`. Dependencies point `app -> data/domain` and `data -> domain`. Domain has no Android dependencies. FND-02 owns the domain types; DATA-02 owns Room schema and encryption compatibility.

Sources checked 2026-10-22:

- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [AGP built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [Compose dependencies and BOM](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)
- [Google Play target API requirement](https://developer.android.com/google/play/requirements/target-sdk)
