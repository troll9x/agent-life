# Kiểm thử Android

Mở thư mục `android/` trong Android Studio. Cài JDK hỗ trợ Gradle 9.6, Android SDK Platform 37.0 và Build Tools 36.0.0 theo [TOOLCHAIN.md](TOOLCHAIN.md). Unit tests, lint và build không cần tài khoản, client ID hay API key.

## Windows (PowerShell)

```powershell
Set-Location android
.\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest
.\gradlew.bat :data:lintDebug :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

`app` và `data` hiện chưa có unit test riêng; Gradle báo `NO-SOURCE`. Bảy domain tests kiểm số tiền, overflow, UUID và ranh giới ngày có DST. Kết quả nằm trong `android/domain/build/reports/tests/test/` và báo cáo lint trong `android/*/build/reports/lint-results-debug.html`.

## Test trên thiết bị

Khởi động emulator hoặc kết nối điện thoại; xác nhận `adb devices` báo `device` thay vì `offline`. Sau đó chạy trong `android/`:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

App có Compose smoke test mở launcher và xác nhận “Agent Life” hiển thị; đã chạy trên AVD API 37. Compose UI test cần Espresso 3.7.0 trên API 37 vì bản cũ gọi `InputManager.getInstance` đã bị bỏ. `data` đã cấu hình runner và Room testing dependency nhưng chưa có database/DAO; migration test thuộc DATA-02 và được ghi `NOT_RUN`. Nếu không có thiết bị online, ghi `NOT_RUN`, không lấy việc compile test APK làm kết quả instrumentation.

## CI

`.github/workflows/android.yml` chạy khi push hoặc mở pull request. Job Ubuntu cài JDK 25 và SDK Platform 37.0, rồi chạy domain/app/data unit tests, lint và assembleDebug bằng Gradle wrapper. Instrumentation không chạy trong job này; dùng lệnh thiết bị ở trên. Workflow không dùng secrets.

Nguồn phiên bản/thư viện: [AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test), [Compose UI testing](https://developer.android.com/develop/ui/compose/testing), [Room releases](https://developer.android.com/jetpack/androidx/releases/room), [kotlinx.coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases).
