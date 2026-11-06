# FIN-09 Hoàn tiền và điều chỉnh số dư có lý do

## Kết quả

Implementation: **IMPLEMENTED**; ledger/báo cáo/UI đã kiểm, AI runtime còn NOT_RUN. External gate: **NOT_REQUIRED**. Bản kiểm: 06/11/2026, AVD `AgentLife_Isolated_Test_API_37` (API 37).

Refund chọn expense đã post, giữ link `refundOfId` và category của expense gốc, ghi delta dương theo ngày nhận thực. Room transaction kiểm tổng partial refund không vượt khoản chi; hai lệnh cạnh tranh không ghi vượt. Adjustment nhận signed delta và lý do bắt buộc, chỉ đổi balance, không đi vào income/gross expense/refund/net expense. UI có preview số dư trước post, bảng tháng hiển thị net expense âm khi tháng chỉ có refund. Không tạo adjustment tự động từ đối soát.

## Prerequisite và hợp đồng

- FIN-02 VERIFIED: `FinanceRepository`, Room ledger v5, nhập tay và khu Tài chính. FIN-08 VERIFIED: reversal/replacement; refund của expense đã reverse bị từ chối. Repo không có `AGENTS.md`, dùng `06_AGENT_RULES.md`.
- `Money` là Long minor units; `occurredAt` dùng ngày nhận refund, không sửa expense gốc. `refundOfId` và `categoryId` theo `04_DATA_CONTRACTS.md`. Báo cáo dùng cùng `FinanceRepository.report` để tránh phân loại adjustment thành thu/chi.
- AI runtime/charts chưa được triển khai tại task này. Invariant đã kiểm ở báo cáo dùng chung; khi làm AI consumer phải kiểm riêng việc consumer đọc báo cáo này.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/refund/ManualRefundAdjustmentCommands.kt`: parse/validate input refund và signed adjustment, lý do bắt buộc.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/RoomFinanceRepository.kt`: refund giữ category gốc, kiểm entry expense âm; adjustment từ chối link không liên quan. Giới hạn partial refund vẫn kiểm trong Room transaction hiện có, không đổi schema.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/refund/RefundAdjustmentSection.kt`: chọn expense gốc, form partial refund, preview adjustment, bảng tháng signed net expense.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/entry/{FinanceFeature,FinanceEntryScreen}.kt`, `AppContainer.kt`, `ShellViewModel.kt`: Flow báo cáo theo tháng/múi giờ hồ sơ và điều phối command từ UI.
- `android/data/src/androidTest/kotlin/vn/lifeos/data/finance/RoomFinanceRepositoryTest.kt`, `android/app/src/androidTest/kotlin/vn/lifeos/app/FinanceEntryUiTest.kt`: test nghiệp vụ và Compose.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-09-01 | PASS | Room: expense 500.000, refund 100.000 cùng kỳ → income 0, gross 500.000, refund 100.000, net 400.000, balance 9.600.000; refund mang category gốc. Refund 600.000 bị từ chối. Hai refund 300.000 cạnh tranh chỉ một thành công. | Chưa kiểm ngân hàng thật; chỉ nhập tay offline. |
| RQ-FIN-09-02 | NOT_RUN toàn bộ; ledger/báo cáo PASS | Room: adjustment -250.000 và +50.000 → balance 9.800.000, income/gross/net đều 0; thiếu lý do bị từ chối. Compose: preview 9.500.000 → 9.250.000 trước ghi. | AI consumer chưa có; chưa thể kiểm vế AI của requirement. Checklist giữ unchecked. |

## Lệnh đã chạy

| Lệnh | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `./gradlew.bat :domain:test :data:assembleDebugAndroidTest --console=plain -q` | PowerShell trong `android/`, JBR Android Studio | Exit 0. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.data.finance.RoomFinanceRepositoryTest vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (9 tests)`. |
| `./gradlew.bat :app:assembleDebugAndroidTest :app:testDebugUnitTest :data:testDebugUnitTest :app:lintDebug :data:lintDebug :app:assembleDebug --console=plain -q` | Cùng máy | Exit 0; lint report và debug APK được tạo. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceEntryUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD | `OK (3 tests)`. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD | `OK (34 tests)`. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceEntryEndToEndTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD | `OK (1 test)` với DB mã hóa. |
| `./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug --console=plain -q`; `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.DeviceZoneSyncTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Cùng máy/AVD, sau nối cập nhật múi giờ | Gradle exit 0; zone sync `OK (1 test)`. |
| `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; mở Finance, cuộn xuống | Cùng AVD | Install `Success`; UI dump thấy form “Hoàn tiền”, “Điều chỉnh số dư”, “Ghi điều chỉnh” và giá trị “Thực chi” ở bảng tháng. |
| `git diff --check` | Repo root | Exit 0. |

## Kịch bản và dữ liệu

- Chỉ dùng UUID/fixture giả. Serial `emulator-5554` được kiểm command line là `AgentLife_Isolated_Test_API_37`; không chạy instrumentation trên AVD người dùng.
- Refund khác kỳ: expense tháng 10 giữ gross/net tháng 10, tháng 11 chỉ có refund 100.000 cho net expense -100.000, income 0. UI bảng tháng hiển thị dấu âm; không có pie chart ép dữ liệu âm.
- Adjustment không được tạo tự động từ reconciliation; người dùng phải chọn account, thấy preview và nhập lý do trước khi nút Lưu bật.

## Gate và phần chưa kiểm

- Không cần client ID, quyền tenant, mẫu ngân hàng hay mạng. Chưa kiểm điện thoại vật lý, TalkBack thủ công, nguồn refund tự động hoặc AI runtime chưa được xây. Hợp đồng báo cáo đã kiểm, AI consumer tương lai cần test phân loại lại.
- Lịch sử chọn expense trong UI đang giới hạn 50 giao dịch gần nhất, kế thừa FIN-08; lịch sử dài/paging chưa thuộc task này.

## Bàn giao

- Task tiếp theo sẵn sàng về code: **FIN-03** (đã có FIN-08 và FIN-09), hoặc task độc lập khác theo execution plan. RQ-FIN-09-02 cần kiểm bổ sung khi có AI consumer trước khi đổi FIN-09 thành VERIFIED. Dừng ở FIN-09 trong lượt này.
