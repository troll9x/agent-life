# NOTI-01 — Cấp quyền và NotificationListenerService

## Kết quả

Implementation: **VERIFIED trên AVD với nguồn fixture có UID riêng**. External gate: **EXTERNAL_BLOCKED** cho ngân hàng/thiết bị vật lý thật. Kiểm ngày 13/11/2026, base `6951fb8`, commit code **`efc6b51`**; không đánh đồng kiểm fixture với hỗ trợ một ngân hàng thật.

Đã nối NotificationListenerService vào app Kotlin/Compose và DB SQLCipher hiện có. Cài đặt → Thông báo ngân hàng cho phép tìm/chọn ứng dụng, mở quyền Notification access, xem trạng thái quyền/dịch vụ/nguồn thu, thời điểm nhận cuối và ngừng thu. Quyền đọc này tách biệt với POST_NOTIFICATIONS đã có ở onboarding.

## Prerequisite và hợp đồng

- Repo không có `docs/agent-pack/` hoặc AGENTS.md; bộ 2.1 thực tế ở `AI_LifeOS_Agent_Pack/`. Đã đọc README, quy tắc agent, task và phần kiến trúc/hợp đồng/domain/external setup liên quan trước khi sửa. Working tree ban đầu sạch.
- DATA-02 có EncryptedDatabaseProvider/SQLCipher; UX-02 có hồ sơ/settings; FIN-04 có ImportCandidate/SourceEvidence và Hộp thư duyệt. Các gate nguồn ngân hàng thật và AI consumer của FIN-04/FIN-10 vẫn mở.
- Dùng NotificationCapture theo hợp đồng: workspace/connection/device/package, key SHA-256 (không giữ tag gốc), postTime, hash nội dung, revision, processingState. Room **v8→v9**, không DB thứ hai hay destructive fallback.
- Package được kiểm tra trước khi truy cập extras; bỏ group summary và thông báo có dấu hiệu OTP. Bảo thủ: thông báo chứa cả dấu hiệu OTP và tiền cũng bị bỏ. Recheck grant/allowlist/connection/device trong transaction trước khi lưu. Không logging body, network, AI hoặc Sheets trong luồng này.
- Callback chỉ chụp chuỗi giới hạn title 256/body 4096; bridge RAM tối đa 32 thông báo. Hash/filter/transaction chạy trên application IO scope, không phụ thuộc Activity/ViewModel. Chỉ cập nhật số lượng/thời điểm nhận sau commit DB; overflow/lỗi lưu hiện cảnh báo có thể bỏ lỡ thông báo.
- Payload chờ parser được mã hóa; **queue reader `pendingPayload` chỉ trả payload chưa hết hạn 15 phút**. Đây là queue xử lý tạm, không phải diagnostic raw cache. Purge khi capture, qua retention startup/hourly best effort; ack/ngừng chọn/ngắt nguồn/thu hồi quyền xóa title/body. Không hứa xóa byte đúng phút thứ 15 khi Android đình chỉ worker. Raw mặc định không giữ sau parse; parser NOTI-02 chưa triển khai nên hiện chưa có consumer parse. Consumer đó phải kiểm lại quyền/connection/allowlist trước khi đọc và xử lý, không dùng query snapshot phục vụ test để đọc payload.
- Capture adapter v1 tạo candidate metadata-only trong cùng transaction FIN-04: parserVersion=1 đánh phiên bản adapter, mọi trường amount/currency/direction/occurredAt/account/category đều null và có missingFields. Chưa hỗ trợ mẫu ngân hàng nào; postTime không bị dùng thay thời điểm giao dịch, không suy amount=0, không tự ghi ledger. NOTI-02 phải tạo revision parser riêng và clear payload sau xử lý.
- Source luôn PARTIAL khi được phép thu, không suy dịch vụ connected thành đủ giao dịch. Không scan active/history notifications hay khôi phục lịch sử mất. Catalog chỉ ứng dụng có launcher; không dùng QUERY_ALL_PACKAGES, không hardcode package ngân hàng hoặc tự xác nhận danh tính ngân hàng.

Manifest được đối chiếu [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService): `exported=false`, permission BIND_NOTIFICATION_LISTENER_SERVICE và intent action đúng. Trang quyền dùng [Settings](https://developer.android.com/reference/android/provider/Settings) detail API 30+ và fallback trang danh sách. Không tìm cách vượt [Android sensitive notification redaction](https://developer.android.com/about/versions/15/behavior-changes-all#otp-redaction).

## File đã thay đổi

- `android/app/src/main/kotlin/vn/lifeos/app/banknotifications/`: service, filter, runtime và màn hình chọn package/quyền/trạng thái.
- `android/app/src/main/res/values/bank_notifications.xml`: copy tiếng Việt, phạm vi quyền/queue/coverage và các trạng thái loading/empty/error/permission; hoạt động cục bộ ngoại tuyến.
- `android/data/src/main/kotlin/vn/lifeos/data/notifications/`: entities/DAO/repository/migration, dedup, TTL, authorization và atomic capture → review candidate.
- Registration tối thiểu: manifest service + queries launcher; AppContainer/Application khởi động runtime; AgentLifeScreen/ProfileSettingsScreen gắn feature vào Cài đặt, bỏ card NOTIFICATION giả của danh sách connector tổng quát; app build dùng `libs.room.runtime` cùng version đã có để truy cập DAO từ adapter.
- AgentLifeDatabase/EncryptedDatabaseProvider/schema `9.json`: đăng ký entity/DAO/migration. RetentionWorker/RoomPrivacyRepository: nối purge/disconnect/delete-workspace để queue không sót raw/FK mới, không đổi ledger.
- Unit/instrumentation tests ở `app/.../banknotifications/` và `data/.../notifications/`.
- `android/test-fixtures/notification-publisher/` + `settings.gradle.kts`: APK phát fixture riêng UID để kiểm cross-package thực sự. Không là dependency hay thành phần APK production; README có cách chạy/khôi phục quyền.
- TASK_BOARD, checklist bản repo và evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-NOTI-01-01 | **PASS trên AVD/fixture** | Unit chứng minh supplier nội dung không được gọi cho package ngoài allowlist/group summary. Room không tạo capture/candidate cho spoof. OS test đăng SPOOF_FIXTURE có nội dung như ngân hàng từ app không được chọn, không có body này trong DB; lọc logcat không có marker fixture. Production feature không có lệnh log body. | Không kiểm mọi OEM/hệ thống log của ngân hàng thật. |
| RQ-NOTI-01-02 | **PASS trên AVD với listener hệ thống thật** | Không mở Activity Agent Life, gửi HOME; publisher UID riêng gửi hai notification (có body và thiếu body), cả hai vào SQLCipher queue. Summary bị bỏ, tổng tăng đúng 2. Thu hồi listener access → permission/connected false, connection PERMISSION_DENIED, body/title được xóa; post sau revoke không tăng count. | UI đóng khác force-stop. Chưa kiểm reboot/process-kill/OEM background restriction, thuộc nghiệm thu tiếp theo. |

## Lệnh đã chạy

PowerShell, repo `D:\GIT\Agent Life`; Gradle trong `android/`, `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. `adb` là `C:\Users\admin\AppData\Local\Android\Sdk\platform-tools\adb.exe`.

| Lệnh nguyên văn | Kết quả quan sát |
| --- | --- |
| `./gradlew.bat :data:compileDebugKotlin --console=plain -q` | Exit 0, schema 9 được export. |
| `./gradlew.bat :app:compileDebugKotlin --console=plain -q` | Lần đầu FAIL do app thiếu RoomDatabase superclass trên compile classpath; đã nối dependency Room runtime cùng version hiện có. Các build sau thành công. |
| `./gradlew.bat :app:testDebugUnitTest :app:lintDebug :data:lintDebug :data:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | Exit 0. |
| `./gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | Exit 0. |
| `./gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug :notification-fixture:assembleDebug --console=plain -q` | Exit 0. App unit tổng 9/9, gồm NotificationFilterTest 4/4; domain/data unit thành công. App lint 0 errors (28 warnings, 3 hints), data lint No issues found. |
| `./gradlew.bat :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug :app:lintDebug :data:lintDebug --console=plain -q` | Exit 0 sau đổi stats aggregate/privacy test. |
| `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest --console=plain -q` | Exit 0 sau bổ sung lọc OTP viết hoa tiếng Việt/unaccented/one-time code; 4/4 filter tests không lỗi. |
| `adb devices`; `adb -s emulator-5554 shell getprop ro.boot.qemu.avd_name`; `Get-CimInstance Win32_Process` kiểm command line QEMU | Đúng `AgentLife_Isolated_Test_API_37`, API 37, chạy headless. Chỉ thay quyền OS trên AVD này. |
| `adb -s emulator-5554 install -r android/data/build/outputs/apk/androidTest/debug/data-debug-androidTest.apk`; `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | Success; **OK (59 tests)**, gồm migration 8→9 và 3 repository tests mới, regression ledger/debt/scheduled/planner/privacy/SQLCipher. |
| `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; `adb -s emulator-5554 install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | Success, cài lại thành công sau các sửa test. |
| `adb -s emulator-5554 shell dumpsys package vn.lifeos.app`; tương tự `vn.lifeos.app.test` | Trước probe: POST_NOTIFICATIONS app=true, test APK=false. Đã khôi phục quyền test APK về false sau probe thất bại; quyền app giữ true. |
| `adb -s emulator-5554 shell am instrument -w -e isolatedNotificationTest true -e class vn.lifeos.app.banknotifications.BankNotificationServiceTest,vn.lifeos.app.banknotifications.BankNotificationUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Probe đầu **FAIL**: instrumentation chạy dưới UID app, không được giả UID test APK để post/cancel. Compose PASS. Đã bỏ cách giả UID, dùng APK fixture thực sự riêng và khôi phục listener grant trước lần chạy sau. |
| `adb -s emulator-5554 install -r android/test-fixtures/notification-publisher/build/outputs/apk/debug/notification-fixture-debug.apk`; `adb -s emulator-5554 shell pm grant vn.lifeos.notificationfixture android.permission.POST_NOTIFICATIONS` | Success, fixture package độc lập. |
| `adb -s emulator-5554 shell am instrument -w -e isolatedNotificationTest true -e class vn.lifeos.app.banknotifications.BankNotificationServiceTest,vn.lifeos.app.banknotifications.BankNotificationUiTest,vn.lifeos.app.FinanceEntryEndToEndTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | **OK (3 tests)**. Listener hệ thống/cross UID + Compose + regression E2E tài chính trên DB mã hóa. Test tự restore listener grant/allowlist trong finally. |
| `adb -s emulator-5554 install -r android/data/build/outputs/apk/androidTest/debug/data-debug-androidTest.apk`; `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.data.notifications.NotificationMigrationTest,vn.lifeos.data.notifications.RoomNotificationRepositoryTest,vn.lifeos.data.privacy.PrivacyRepositoryTest,vn.lifeos.data.security.EncryptedDatabaseProviderTest vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | **OK (13 tests)** trên APK data cuối: migration, 4 notification repository tests (thêm disconnect), privacy và encrypted provider. |
| `adb -s emulator-5554 logcat -d -v brief` lọc `SPOOF_FIXTURE\|ALLOWED_FIXTURE\|AFTER_REVOKE_FIXTURE` | Không có dòng marker fixture. |
| `rg -n -e 'Log\.' -e println -e printStackTrace android/app/src/main/kotlin/vn/lifeos/app/banknotifications android/data/src/main/kotlin/vn/lifeos/data/notifications` | Không có call logging. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.ShellNavigationTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | **OK (2 tests)**: điều hướng qua Cài đặt có registration mới và xoay màn hình. |
| `adb -s emulator-5554 shell settings get secure enabled_notification_listeners`; `adb -s emulator-5554 shell dumpsys package vn.lifeos.app`; `adb -s emulator-5554 uninstall vn.lifeos.notificationfixture` | Đã restore: không còn listener Agent Life được cấp quyền; POST_NOTIFICATIONS app vẫn true như trước. Gỡ publisher fixture Success. |
| `git diff --check` | Exit 0, chỉ cảnh báo LF/CRLF. |
| `git push origin main`; `gh run watch 43356321574 --exit-status --interval 15` | Commit code `efc6b51` đã đẩy. [Android CI 43356321574](https://github.com/troll9x/agent-life/actions/runs/43356321574) **success**, unit tests/lint/debug APK PASS, exit 0. |
| `adb -s emulator-5554 emu kill` | OK, tắt AVD headless đã khởi động để thử nghiệm sau khi hoàn tất các test và restore quyền/gỡ fixture. |

## Kịch bản và dữ liệu

- Tất cả tên/package/chuỗi body/số tiền là fixture; không có tài khoản ngân hàng thật, token, số tài khoản hay API được gọi giả.
- Unit filter kiểm trước đọc extras, summary, OTP (bao gồm tiếng Việt viết hoa/unaccented), giới hạn chuỗi/hash và hidden placeholder. Candidate thiếu nội dung có amountMinor **null**, missingFields chứa amountMinor, state NEEDS_REVIEW; không amount=0.
- Room kiểm grant/allowlist lại, workspace không tồn tại bị từ chối, dedup cùng identity, revision mới, reopen queue bền, expiry đúng mốc, acknowledge, ngừng chọn, revoke và privacy disconnect. Disconnect xóa queue/allowlist nhưng vẫn giữ candidate/evidence cần duyệt. Ledger count không tăng khi capture.
- OS test dùng NotificationListenerService thật với `cmd notification allow_listener/disallow_listener`, không gọi giả callback service. Group summary và spoof không tăng queue; payload thiếu body tạo bản MISSING_CONTENT. Đây là mô phỏng nội dung đã bị ẩn, **chưa phải kiểm khóa màn hình thật**.
- Compose thực hiển thị giải thích quyền rộng, permission denied, ô tìm ứng dụng, chọn checkbox và ngừng thu; callback mở Cài đặt chỉ chạy khi bấm. Runtime chỉ đưa metadata aggregate cho UI; lỗi lưu không biến thành empty success.

## Gate và phần chưa kiểm

- **EXTERNAL_BLOCKED:** cần người dùng tự cấp Notification access cho app ngân hàng thật trên thiết bị đích và cung cấp mẫu đã che dữ liệu để xác minh parser/coverage. Chưa hỗ trợ template ngân hàng, không hứa đọc lịch sử đầy đủ.
- **NOT_RUN:** thiết bị vật lý, OEM/Android khác API 37, che/redact do khóa màn hình thật. Framework có thể che cả OTP; nếu không thấy dấu hiệu OTP, app không suy nội dung/amount và giữ candidate thiếu dữ liệu. Không bypass redaction.
- **NOT_RUN, chuyển NOTI-03:** force-stop, reboot/process death, burst overflow, retry/crash trước DB commit. Bridge RAM chưa commit có thể mất; callback khi allowlist chưa load lúc cold start được bỏ an toàn. Không claim exactly-once end-to-end. Queue đã commit/reopen/dedup/expiry được kiểm trong task này.
- Gate DATA-04 xóa workspace có ledger/FK và revoke connector thật vẫn kế thừa; thay đổi này chỉ nối cleanup queue mới, không tuyên bố đóng gate toàn sản phẩm.

## Bàn giao

Task tiếp theo sẵn sàng về code/fixture: **NOTI-02 — parser thông báo theo mẫu ngân hàng** (NOTI-01/FND-02 có code). Phải giữ missingFields/review, không chọn số dư làm amount, không đặt amount=0 khi thiếu. Dừng tại NOTI-01, chưa triển khai parser/retry hay nguồn ngân hàng thật.
