# FIN-07 Giao dịch định kỳ và khoản dự kiến

## Kết quả

Implementation: **VERIFIED offline/bằng fixture**, kiểm ngày **12/11/2026**. External gate: **EXTERNAL_BLOCKED** đối với nguồn email/notification ngân hàng thật kế thừa FIN-04. Không gọi API thật.

Commit code được kiểm: `193b1de`.

Rule hỗ trợ một lần, ngày, tuần, tháng, năm; lưu nextDueDate và template thu/chi. Khi đến ngày, engine sinh occurrence có unique workspace/ruleId/dueDate và bản chụp title/amount/currency/account/category. Tạo dự kiến, chạy engine lại, nhắc hoặc skip không ghi ledger. Chỉ confirm qua review mới post hoặc ghép giao dịch đã có. Khi sửa rule chỉ đổi lần chưa sinh; các snapshot/history đã sinh được giữ nguyên. Có tạm dừng/bật lại. Tiền dùng Long minor units và Money theo locale, không Sheets.

## Prerequisite và hợp đồng

- Repo sạch lúc bắt đầu, không có AGENTS.md; áp dụng `AI_LifeOS_Agent_Pack/06_AGENT_RULES.md`. Đọc FIN-07 và phần architecture/domain/data contracts/analytics liên quan trong bộ 2.1.
- FIN-02 có manual ledger; FIN-04 có parsed evidence/inbox/approve/duplicate; PLAN-03 có RoomReminderStore, ReminderEngine, AlarmManager và worker reschedule. Dùng lại tất cả, không tạo DB hay ledger mới.
- `ScheduledTransaction` chứa recurrence, anchorDate, nextDueDate, template và enabled/version. Monthly/yearly giữ ngày mốc qua tháng ngắn/năm nhuận. `PlannedTransaction` giữ snapshot và PENDING/POSTED/SKIPPED, transactionId.
- Confirm + post/link ledger/evidence + cập nhật occurrence + audit cùng transaction Room. Unique occurrence key, transaction link và operation ID; decision yêu cầu PENDING. Confirm đồng thời chỉ một lệnh thành công. Lỗi link sau khi đã ghép evidence rollback toàn bộ.
- Gợi ý ngân hàng yêu cầu cùng tài khoản, chiều, currency, amount, ngày trong ±3 ngày quanh hạn. Không tự xác định cùng hóa đơn. Người dùng chọn ghép evidence/giao dịch; nếu là khoản độc lập cùng số tiền thì phải đánh dấu đã kiểm và xác nhận thanh toán riêng. Ngày ghi nhận thực tế có thể muộn hơn hạn, không được ở tương lai.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/scheduled/ScheduledContract.kt`: entity/interface/decision và lịch ngày thuần Kotlin.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/scheduled/{ScheduledEntities,ScheduledDao,ScheduledMigration,RoomScheduledFinanceRepository,ScheduledReminders}.kt`: Room v8, sinh occurrence durable, snapshot, approve/skip/link/evidence atomic và reminder candidates.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/scheduled/{ScheduledFeature,ScheduledSection}.kt`: coordinator, form date/time picker, rule tương lai, review, loading/empty/error/retry/offline và trạng thái quyền nhắc chung.
- Registration tối thiểu: `AgentLifeDatabase.kt`, schema `android/data/schemas/vn.lifeos.data.db.AgentLifeDatabase/8.json`, `EncryptedDatabaseProvider.kt` migration 7→8; `AppContainer.kt`, `ShellViewModel.kt`, `FinanceFeature.kt`, `FinanceEntryScreen.kt` nối state/actions.
- Tích hợp reminder theo bước 5 của task: thêm target FINANCE trong `PlannerContract.kt`, kiểm target trong `RoomPlannerRepository.kt`; `RoomReminderStore.kt` kiểm active/title/claim/snooze tài chính; `ReminderEngine.kt` sinh due và hợp nhất candidate tài chính với planner. Worker reschedule hiện có gọi engine này khi startup/periodic/reboot/đổi giờ. `ReminderWorkers.kt` thêm thông điệp tài chính, `MainActivity.kt`/`AgentLifeScreen.kt` mở tab Finance khi chạm notification. Notification không có action approve tài chính.
- Tests: domain `FinanceScheduleTest`, data `RoomScheduledFinanceRepositoryTest`/`ScheduledMigrationTest`, app `FinanceScheduledUiTest`/`FinanceScheduledReminderTest`.
- Checklist bản repo, TASK_BOARD, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-07-01 | PASS offline | Room: tiền nhà ngày 1 tới hạn sinh một PENDING, generate lại 0, số dư 10 triệu không đổi. Skip và reminder claim/snooze không ghi tiền. UI/E2E DB mã hóa: trước confirm và tại dialog review vẫn 10 triệu; sau confirm mới 9 triệu, expense 1 triệu. | Không tự post từ worker/notification. |
| RQ-FIN-07-02 | PASS bằng fixture | Room: hai confirm đồng thời chỉ 1 thành công; reopen/generate lại không thêm occurrence. Notification rồi mail cùng reference/amount/account được ghép một occurrence/một transaction với 2 evidence, balance 9 triệu. Giao dịch đã có được link; link trùng sang occurrence khác bị chặn và rollback evidence. | Chưa nhận nguồn ngân hàng thật; gate FIN-04 vẫn mở. |

## Lệnh đã chạy

Môi trường Windows, JBR Android Studio tại `C:\Program Files\Android\Android Studio\jbr`, SDK adb tại `C:\Users\admin\AppData\Local\Android\Sdk\platform-tools\adb.exe`. Gradle chạy trong `android/`; adb từ repo root.

| Lệnh nguyên văn | Kết quả quan sát |
| --- | --- |
| `./gradlew.bat :data:compileDebugKotlin :app:compileDebugKotlin --console=plain -q` | Build đầu lỗi smart cast nextDueDate và when chưa có FINANCE trong planner; đã sửa. |
| `./gradlew.bat :domain:test :data:compileDebugKotlin :app:compileDebugKotlin --console=plain -q` | Phát hiện receiver NumberFormat che khuất currency của plan và smart cast transactionId ở UI; đã sửa bằng biến local. |
| `./gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | Exit 0. Domain FinanceScheduleTest XML: 2 tests, 0 failures. Lint không có error; debug APK và test APK tạo thành công. Chạy lại sau thêm test alarm/thanh toán riêng cũng exit 0. |
| `./gradlew.bat :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | Exit 0 bản cuối sau điều chỉnh ngày thanh toán muộn và thời điểm trong dialog. |
| `adb devices`; kiểm QEMU command line bằng `Get-CimInstance Win32_Process` | `emulator-5554 device`, AVD riêng `AgentLife_Isolated_Test_API_37`, API 37. |
| `adb -s emulator-5554 install -r android/data/build/outputs/apk/androidTest/debug/data-debug-androidTest.apk`; `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; `adb -s emulator-5554 install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | Cả ba `Success`. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | `OK (51 tests)`: 6 scheduled repository + 1 migration mới, regression ledger/debt/goals/inbox/reports/planner/reminders/SQLCipher. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceScheduledUiTest,vn.lifeos.app.FinanceScheduledReminderTest,vn.lifeos.app.FinanceEntryUiTest,vn.lifeos.app.FinanceInboxUiTest,vn.lifeos.app.ReminderEngineTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | `OK (8 tests)`: E2E Compose/DB mã hóa, alarm tài chính, regression tài chính/inbox/reminder planner. |
| `git diff --check` | Exit 0, chỉ cảnh báo LF/CRLF. |
| `git push origin main`; `gh run watch 43154795033 --exit-status --interval 15` | Commit `193b1de` đã đẩy; [Android CI 43154795033](https://github.com/troll9x/agent-life/actions/runs/43154795033) success, unit tests/lint/debug APK PASS. |

## Kịch bản và dữ liệu

- UUID, tên ví, nhóm, reference và event ID đều là fixture. Không token, account ngân hàng thật hay raw notification/email.
- Tiền nhà ngày 1 không tự trừ ví. Generate hai lần và đóng/mở DB không sinh lần thứ hai. Hai lệnh approve cùng occurrence chỉ một lệnh post.
- Lương dự kiến chưa confirm không tăng balance; confirm tăng balance và income 1 triệu. Hai hóa đơn cùng số tiền: lần thứ hai mặc định bị yêu cầu kiểm gợi ý; người dùng xác nhận khoản riêng tạo transaction khác, không ghép sai.
- Đổi amount rule từ 1 lên 2 triệu giữ occurrence tháng cũ 1 triệu; pause không sinh tháng sau, enable lại sinh đúng snapshot 2 triệu. Version cũ, sai account/currency/workspace bị từ chối.
- Migration 7→8 validate schema, giữ ledger entry và opening debt từ v7. Không destructive fallback.
- Reminder data test: target FINANCE, claim chỉ một lần, snooze được, sau skip không snooze/complete, balance không đổi. App engine test: reconcile hai lần chỉ một reminder/alarm PendingIntent; trigger 09:00 theo zone; tạm dừng rule tương lai hủy alarm và chuyển row CANCELLED.
- UI thực chạy: thấy rule, khoản đến hạn có nhãn chưa ghi sổ, date/time picker và dialog review. Ledger không đổi khi chỉ mở dialog. Confirm cập nhật nhãn đã ghi/ghép; đóng/mở DB mã hóa vẫn một POSTED occurrence.

## Gate và phần chưa kiểm

- **EXTERNAL_BLOCKED kế thừa FIN-04:** chưa có connector/parser/mẫu ngân hàng production để nhận notification/email thật. Ghép evidence chỉ PASS bằng parsed metadata fixture; không nhận là đã gọi API hay xử lý ngân hàng thật.
- Thiết bị vật lý/OEM **NOT_RUN**. Đã lập/hủy AlarmManager PendingIntent trên AVD và kiểm store claim; chưa chờ thông báo tài chính tự bắn đúng 09:00, chưa thao tác chạm notification tài chính thực tế trong lượt này. Reminder planner delivery/reboot có evidence cũ tại PLAN-03; không thay thế kiểm finance mới bằng lời khẳng định đã chạy.
- Reminder dùng fallback và quyền chung; không cam kết đúng phút khi thiếu exact access/Doze/force-stop. Các khoản quá hạn vẫn xuất hiện trong app dù bỏ lỡ notification.
- Mỗi lần generate tối đa 256 occurrence để giới hạn catch-up; nextDueDate còn lại được giữ cho lần chạy tiếp. ONCE hoàn tất sinh có nextDueDate null. Gợi ý evidence ±3 ngày không phải policy tự match; ngoài cửa sổ có thể xử lý qua inbox chung.
- Form chỉ hỗ trợ template INCOME/EXPENSE cùng currency tài khoản; principal/transfer thuộc nghiệp vụ riêng đã có, không nhét vào chi dự kiến.

## Bàn giao

Task tiếp theo sẵn sàng: **FIN-10 — đối soát tài khoản và chênh lệch**, dựa trên FIN-04/FIN-09 đã có code, giữ gate nguồn thật và AI consumer của dependency. Dừng tại FIN-07.
