# Notification publisher fixture

APK thử nghiệm với UID riêng `vn.lifeos.notificationfixture`. Không nằm trong dependency của app; không dùng ngân hàng/API thật. Chỉ cài trên AVD riêng `AgentLife_Isolated_Test_API_37` để chạy NOTI-01. Activity nhận chuỗi giả và đăng thông báo, hoặc xóa thông báo khi có `--ez clear true`.

Từ `android/`:

```powershell
.\gradlew.bat :notification-fixture:assembleDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

Sau khi kiểm tra `adb -s emulator-5554 shell getprop ro.boot.qemu.avd_name` đúng AVD trên, cài ba APK. Ghi nhận quyền gửi hiện tại trước khi cấp quyền:

```powershell
adb -s emulator-5554 install -r test-fixtures/notification-publisher/build/outputs/apk/debug/notification-fixture-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell dumpsys package vn.lifeos.app
adb -s emulator-5554 shell pm grant vn.lifeos.app android.permission.POST_NOTIFICATIONS
adb -s emulator-5554 shell pm grant vn.lifeos.notificationfixture android.permission.POST_NOTIFICATIONS
adb -s emulator-5554 shell am instrument -w -e isolatedNotificationTest true -e class vn.lifeos.app.banknotifications.BankNotificationServiceTest,vn.lifeos.app.banknotifications.BankNotificationUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner
```

Test tự khôi phục listener grant và allowlist trong `finally`. Test quyền OS được skip trong suite thường, chỉ chạy với flag opt-in và đúng tên AVD. Sau kiểm: khôi phục quyền POST_NOTIFICATIONS của app nếu trước đó chưa cấp (`pm revoke`), gỡ APK publisher (`adb uninstall vn.lifeos.notificationfixture`). Không thu hồi POST_NOTIFICATIONS trong instrumentation vì Android có thể kết thúc tiến trình test khi quyền bị thu hồi.

Các capture/candidate sinh ra chỉ là fixture, không ghi ledger. Thiết bị vật lý, ngân hàng thật, Android/OEM khác và che nội dung khóa màn hình thật chưa được xác nhận bởi fixture này.
