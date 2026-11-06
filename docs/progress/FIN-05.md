# FIN-05 Mục tiêu và tài khoản tiết kiệm

## Kết quả

**VERIFIED bằng fixture offline** ngày 06/11/2026. Mục tiêu lưu trong Room, liên kết với một hoặc nhiều tài khoản SAVINGS cùng tiền tệ. Tiến độ đọc số dư từ ledger hiện có; không lưu số dư mục tiêu riêng. Chuyển tiền từ BANK/CASH vào SAVINGS dùng `TRANSFER` hiện có. Một tài khoản có thể liên kết với nhiều mục tiêu; tổng số dư tài khoản cộng mỗi tài khoản đúng một lần. Thay target và lưu trữ mục tiêu giữ nguyên tài khoản, giao dịch.

## Prerequisite và phạm vi

- FIN-03 đã VERIFIED. Dùng `FinanceRepository`, `Money` Long minor units, ledger và báo cáo đã có. Không tạo schema ledger khác hay Google Sheets. Repo không có `AGENTS.md`; áp dụng `06_AGENT_RULES.md`.
- File chính: `android/domain/.../finance/goals/SavingsGoals.kt`, `android/data/.../finance/goals/{SavingsGoalEntities,SavingsGoalDao,SavingsGoalMigration,RoomSavingsGoalRepository}.kt`, `android/app/.../finance/goals/SavingsGoalsSection.kt`.
- Registration tối thiểu: Room DB version 6, migration 5→6, AppContainer, FinanceFeature/FinanceEntryScreen, ShellViewModel. Thêm schema JSON v6 và các test domain/Room/Compose; cập nhật fake actions của test cũ.

## Đối chiếu yêu cầu

| ID | Kết quả | Bằng chứng |
| --- | --- | --- |
| RQ-FIN-05-01 | PASS bằng fixture offline | Room test tạo mục tiêu target 10.000.000, chuyển 2.000.000: current 2.000.000, progress 20%; đóng/mở DB vẫn còn target đã sửa và trạng thái archive. Domain test tính từ số dư tài khoản; Compose test hiển thị `2.000.000 / 10.000.000 VND · 20%`. |
| RQ-FIN-05-02 | PASS bằng fixture offline | Room test ghi `TRANSFER` với hai ledger entries, income 0, net expense 0, tài sản tổng 10.000.000 sau chuyển (bank 8.000.000 + savings 2.000.000). Hai mục tiêu dùng chung savings vẫn có tiến độ riêng nhưng tài sản không cộng đôi. Archive giữ account và ledger entries. Compose gửi đúng `TransactionKind.TRANSFER`. |

## Lệnh và kết quả quan sát

Môi trường: `android/`, JBR Android Studio; AVD `emulator-5554` API 37. Chỉ dùng UUID/tài khoản fixture, không có token hay dữ liệu cá nhân.

| Lệnh | Kết quả |
| --- | --- |
| `./gradlew.bat :domain:test :data:compileDebugKotlin :app:compileDebugKotlin --console=plain -q` | Exit 0. |
| `./gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | Exit 0; lint và debug APK tạo thành công. `SavingsGoalProgressTest`: 2 tests, 0 failures. |
| `./gradlew.bat :app:assembleDebugAndroidTest --console=plain -q` | Exit 0 sau khi thêm Compose test. |
| `adb install -r .../data-debug-androidTest.apk`, `adb install -r .../app-debug.apk`, `adb install -r .../app-debug-androidTest.apk` | Cả ba `Success`. |
| `adb shell am instrument -w -e class vn.lifeos.data.finance.goals.SavingsGoalMigrationTest,vn.lifeos.data.finance.goals.RoomSavingsGoalRepositoryTest vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | `OK (2 tests)`: schema 5→6 hợp lệ, lưu/tính tiến độ/transfer/archive/reopen. Lần đầu test repository sai do dựa vào thứ tự hai mục tiêu; đã sửa tra theo ID và chạy lại PASS. |
| `adb shell am instrument -w -e class vn.lifeos.app.FinanceGoalsUiTest,vn.lifeos.app.FinanceEntryUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | `OK (5 tests)`: UI mục tiêu và regression tài chính. |
| `git diff --check` | Exit 0; chỉ cảnh báo Git về LF/CRLF. |
| `git push origin main`; `gh run watch 42192296884 --exit-status --interval 10` | Commit `39c90a2` đã đẩy; [Android CI run 42192296884](https://github.com/troll9x/agent-life/actions/runs/42192296884) success: unit tests, lint, debug APK. |

## Gate còn mở

- Không cần client ID, tenant, mẫu ngân hàng hoặc API ngoài cho FIN-05. Chưa kiểm trên thiết bị vật lý; đã kiểm Room và UI trên emulator API 37.
- FIN-04 vẫn có external gate riêng cho nguồn notification/email thật; FIN-05 không nhận là đã xử lý gate đó.

## Bàn giao

Task tiếp theo sẵn sàng: **FIN-06** (phụ thuộc FIN-03 đã VERIFIED). Dừng tại FIN-05.
