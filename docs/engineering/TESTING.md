# Kiểm thử Android

Mở thư mục `android/` trong Android Studio. Cài JDK hỗ trợ Gradle 9.6, Android SDK Platform 37.0 và Build Tools 36.0.0 theo [TOOLCHAIN.md](TOOLCHAIN.md). Unit tests, lint và build không cần tài khoản, client ID hay API key.

## Windows (PowerShell)

```powershell
Set-Location android
.\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest
.\gradlew.bat :data:lintDebug :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

`app` và `data` hiện chưa có unit test JVM riêng; Gradle báo `NO-SOURCE`. Bảy domain tests kiểm số tiền, overflow, UUID và ranh giới ngày có DST. Room migration, repository và SQLCipher/Keystore được kiểm bằng instrumentation của `data`. Kết quả nằm trong `android/domain/build/reports/tests/test/`, `android/{data,app}/build/reports/androidTests/connected/debug/` và báo cáo lint trong `android/*/build/reports/lint-results-debug.html`.

## Test trên thiết bị

Khởi động emulator hoặc kết nối điện thoại; xác nhận `adb devices` báo `device` thay vì `offline`. Sau đó chạy trong `android/`:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :data:connectedDebugAndroidTest
```

App có 2 Compose tests: launcher mở DB mã hóa rồi hiển thị “Agent Life”, và màn hình hướng dẫn khi khóa không khả dụng. Compose UI test cần Espresso 3.7.0 trên API 37 vì bản cũ gọi `InputManager.getInstance` đã bị bỏ. `data` có 7 instrumentation tests: 3 test Room migration/repository và 4 test SQLCipher/Keystore, gồm SQLite thường từ chối file và mất khóa không xóa DB. DATA-01 vẫn có fixture DB plaintext chỉ trong test APK; app production chỉ mở qua `EncryptedDatabaseProvider`. Nếu không có thiết bị online, ghi `NOT_RUN`, không lấy việc compile test APK làm kết quả instrumentation. Mô hình khóa và giới hạn ở [ENCRYPTION.md](ENCRYPTION.md).

## CI

`.github/workflows/android.yml` chạy khi push hoặc mở pull request. Job Ubuntu cài JDK 25 và SDK Platform 37.0, rồi chạy domain/app/data unit tests, lint và assembleDebug bằng Gradle wrapper. Instrumentation không chạy trong job này; dùng lệnh thiết bị ở trên. Workflow không dùng secrets.

Nguồn phiên bản/thư viện: [AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test), [Compose UI testing](https://developer.android.com/develop/ui/compose/testing), [Room releases](https://developer.android.com/jetpack/androidx/releases/room), [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases).
