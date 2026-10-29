# PLAN-03 Engine nhắc việc Android

## Kết quả

Implementation: **VERIFIED**. External gate: **NOT_REQUIRED**. Bản kiểm: 29/10/2026, AVD `AgentLife_Isolated_Test_API_37` (API 37).

App lập alarm một lần cho từng occurrence task/lịch trong cửa sổ 30 ngày. Alarm chính xác chỉ dùng khi hệ thống cho phép; khi thiếu quyền, app lập alarm không chính xác và báo rõ giới hạn. Receiver chỉ giao việc cho WorkManager; worker đọc Room mã hóa để gửi thông báo, hoãn 10 phút hoặc hoàn tất task. Khi sửa giờ, hoàn tất, reboot, đổi giờ/múi giờ và khi quyền exact được cấp lại, lịch được đối soát từ DB. Nội dung màn hình khóa dùng public notification chung; chạm thông báo mở đúng task hoặc ngày lịch.

## Prerequisite và hợp đồng

- PLAN-02 và DATA-02 đã có code/hợp đồng; repo không có `AGENTS.md`, áp dụng `06_AGENT_RULES.md`.
- Dùng `reminders` trong Room schema v4 và `PlannerRepository` hiện có. Không thay schema/migration, không dùng ledger TypeScript hoặc Google Sheet.
- Không cần client ID, tài khoản cloud hoặc dữ liệu cá nhân cho task offline này.

## File đã thay đổi

- `android/data/src/main/kotlin/vn/lifeos/data/reminders/`: DAO/store với key occurrence ổn định, transaction đối soát, claim/snooze/complete idempotent; `AgentLifeDatabase` đăng ký DAO.
- `android/app/src/main/kotlin/vn/lifeos/app/reminders/`: tính lịch, AlarmManager, receiver/worker, notification và bảng cảnh báo quyền; `res/values/reminders.xml` chứa chuỗi UI.
- `android/app/src/main/AndroidManifest.xml`: quyền notification/boot/exact alarm và receiver; `app/build.gradle.kts`: WorkManager và Room test dependency.
- `AgentLifeApplication`, `AppContainer`, `ShellViewModel`, `MainActivity`, `AgentLifeScreen`, `PlannerScreen`, `CalendarPanel`: đăng ký engine, đối soát khi dữ liệu đổi, mở deep link và hiện trạng thái hạn chế.
- `android/{app,data}/src/{test,androidTest}/`: test plan, Room, engine, quyền, deep link và probe reboot/múi giờ/nhận notification.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-PLAN-03-01 | PASS | Room test sửa 09:00→10:00 làm trigger cũ không claim được; complete hai lần chỉ tăng version task một lần. App engine test thấy PendingIntent cũ được hủy khi hoàn tất. AVD reboot thật: sau boot receiver được giao, worker `SUCCESS`, probe xác nhận alarm khôi phục trước khi mở Activity. Đổi múi giờ AVD Asia/Bangkok→America/Los_Angeles: profile và trigger task lặp đổi, alarm mới tồn tại; đã trả múi giờ về Asia/Bangkok. Probe delivery: alarm bắn notification thật, action hoãn tạo SNOOZE PendingIntent, action hoàn tất hủy nó. | AVD API 37; chưa kiểm máy vật lý/OEM. Android có thể trì hoãn alarm không chính xác khi thiếu special access. |
| RQ-PLAN-03-02 | PASS | UI instrumentation kiểm trạng thái tắt notification; ép `SCHEDULE_EXACT_ALARM deny` trên AVD và kiểm cảnh báo “không cam kết đúng phút” cùng nút mở quyền. Code dùng `canScheduleExactAlarms`, fallback `setAndAllowWhileIdle`, không khai báo `USE_EXACT_ALARM`. | Chưa kiểm trực quan trên mọi bản Android/OEM. |

## Lệnh đã chạy

| Lệnh | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :data:assembleDebugAndroidTest --console=plain -q` | `android/` | Exit 0; app unit 5/5, lint và APK/test APK build PASS. |
| `adb -s emulator-5556 shell am instrument -w vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (25 tests)` ở vòng suite trước hai probe delivery bổ sung; 4 phase probes bị skip có chủ ý ở suite, các phase liên quan được chạy riêng. Probe delivery bổ sung chạy riêng 2/2 PASS sau đó. |
| `adb -s emulator-5556 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD | `OK (19 tests)`, gồm RoomReminderStoreTest. |
| `adb -s emulator-5556 reboot`, đợi `sys.boot_completed=1`, kiểm `dumpsys activity broadcasts` và log worker, chạy `ReminderRebootProbeTest#verifyAndCleanUp` | Cùng AVD | Receiver `DELIVERED`, worker `SUCCESS`, verify `OK (1 test)`; lần đầu chưa qua vì boot broadcast của AVD bị skip khi receiver ở priority 0, đã sửa priority 100 và chạy lại PASS. |
| `adb -s emulator-5556 shell cmd alarm set-timezone America/Los_Angeles`; chạy `ReminderRebootProbeTest#verifyTimezoneAndCleanUp` | Cùng AVD | `ZONE_AFTER=America/Los_Angeles`; `OK (1 test)`, rồi khôi phục Asia/Bangkok và auto time zone. |
| `adb -s emulator-5556 shell cmd appops set vn.lifeos.app SCHEDULE_EXACT_ALARM deny`; chạy `ReminderUiTest#deniedExactAlarmAccessShowsInexactLimitation` | Cùng AVD | `OK (1 test)`; trả app-op về `default`. |
| `adb -s emulator-5556 shell pm grant vn.lifeos.app android.permission.POST_NOTIFICATIONS`; chạy `ReminderRebootProbeTest#prepareDelivery`, đợi alarm, rồi `#verifyDeliveryActionsAndCleanUp` | Cùng AVD | 2/2 PASS; notification thật, snooze và complete được xác nhận qua DB/PendingIntent. |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :app:assembleDebug --console=plain -q` | `android/` | Exit 0 ở vòng cuối; data unit 9/9, app unit 5/5, domain/lint/debug APK PASS. |
| `git diff --check` | Repo root | Exit 0; không có whitespace error. |

CI cho commit `29b9594`: [Android run 40839862387](https://github.com/troll9x/agent-life/actions/runs/40839862387) PASS (`gh run watch 40839862387 --exit-status`, exit 0); job `build` chạy unit tests, lint và debug APK.

## Kịch bản và dữ liệu

- Chỉ dùng tên/UUID fixture. Test Room dùng DB thử riêng; app test chạy trên AVD `emulator-5556`, không chạy instrumentation trên AVD người dùng `emulator-5554`.
- Khi task đổi 09:00 sang 10:00, stable ID giữ nguyên nhưng trigger cũ không thể claim; alarm được ghi lại với thời điểm mới. Khi complete từ notification, retry không tạo completion lần hai. Reminder quá hạn hơn 5 phút không được phát hàng loạt sau reboot.
- Deep link có target type/id/occurrence/trigger; thông báo khóa màn hình hiển thị “Agent Life / Có lời nhắc”, không hiện tiêu đề task/lịch.

## Gate và phần chưa kiểm

- Không có gate tài khoản ngoài hệ thống cho PLAN-03. Chưa kiểm trên điện thoại vật lý, chế độ tiết kiệm pin của OEM, TalkBack và permission UI ở nhiều phiên bản Android. Những phần này không được ghi là PASS.
- Không cam kết đến đúng phút khi Android từ chối special access hoặc tự trì hoãn alarm; UI nêu rõ trạng thái này.

## Bàn giao

- Task tiếp theo sẵn sàng: **FIN-01** (DATA-01 và FND-02 đã có code). PLAN-04 còn chờ MS-03 và AI-02.
