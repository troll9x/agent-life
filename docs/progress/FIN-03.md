# FIN-03 Ngân sách và báo cáo tháng

## Kết quả

Implementation: **VERIFIED** cho RQ-FIN-03-01/02 bằng fixture offline. External gate: **NOT_REQUIRED**. Kiểm ngày 06/11/2026 trên JBR Android Studio và AVD riêng `AgentLife_Isolated_Test_API_37` (API 37).

Báo cáo lấy một snapshot Room nhất quán theo workspace, currency, tài khoản tùy chọn và kỳ địa phương `[start,end)`. Cùng `ReportSnapshot` cấp số tổng, danh sách drill-down và CSV. Thu trừ chi ròng là dòng tiền vận hành, không phải số dư tài khoản. Transfer/adjustment đóng góp 0; reversal triệt tiêu loại giao dịch gốc; refund giảm expense ở ngày nhận. Ngân sách lưu bằng category ID, currency và ngày đầu kỳ theo hồ sơ, có ngưỡng cảnh báo bật/tắt ở 80%.

## Prerequisite và hợp đồng

- FIN-02 và FIN-08 đã VERIFIED. FIN-09 có ledger/refund code và kiểm Room/UI, nhưng RQ-FIN-09-02 vẫn chờ AI consumer để kiểm toàn bộ; không đổi trạng thái FIN-09 trong task này.
- Dùng `FinanceRepository`/ledger Room hiện có, `BudgetEntity` schema v5 sẵn có, `Money` Long minor units. Không thêm schema, migration, thư viện hay Google Sheets. Repo không có `AGENTS.md`; dùng `06_AGENT_RULES.md`.
- Các file ngoài thư mục sở hữu task chỉ là DAO query, DI và luồng điều hướng state cần thiết để nối báo cáo. Không đổi public ledger contract.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/reports/FinanceReports.kt`: kỳ theo ngày bắt đầu, kỳ trước cùng độ dài phần đã qua, bộ lọc, tính báo cáo/ngân sách, CSV RFC 4180 có chặn formula.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/reports/RoomFinanceReportsRepository.kt`, `android/data/src/main/kotlin/vn/lifeos/data/finance/FinanceDao.kt`: đọc transaction/entry/category/budget trong một Room transaction; upsert ngân sách sau kiểm workspace và nhóm chi.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/reports/FinanceReportsSection.kt`: số tổng, chọn kỳ/tài khoản, ngân sách, cảnh báo, drill-down và lưu CSV qua Android document picker.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/entry/{FinanceFeature,FinanceEntryScreen}.kt`, `android/app/src/main/kotlin/vn/lifeos/app/{AppContainer,ShellViewModel}.kt`: state/command và registration tối thiểu, lấy ngày đầu kỳ từ hồ sơ.
- Test domain `FinanceReportsTest.kt`, Room `RoomFinanceReportsRepositoryTest.kt`, cập nhật `FinanceEntryUiTest.kt` và `FinanceEntryEndToEndTest.kt`.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-03-01 | PASS | Domain 3/3: tổng 600 = sum dòng = CSV dòng số, category `=SUM(1,2)` thành text; kỳ 15/10 loại 14/10 23:59 địa phương. Room 1/1 dùng cùng snapshot; Compose 4/4 hiển thị tổng/ngân sách; E2E 1/1 thấy thu 500.000 từ DB mã hóa. | Chưa thao tác document picker lưu file CSV thủ công trên thiết bị vật lý. |
| RQ-FIN-03-02 | PASS | Room 1/1: expense 500, transfer sang savings 1000, refund 200 cùng kỳ → net expense/budget spent 300; lọc savings → net 0; ngân sách 600 còn 300 sau reopen DB. Domain kiểm reversal +1000 triệt tiêu expense gốc, refund riêng kỳ nhận → net -200. | Nguồn giao dịch kiểm bằng fixture offline, không phải ngân hàng thật. |

## Lệnh đã chạy

| Lệnh | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `./gradlew.bat :domain:test :data:compileDebugKotlin :app:compileDebugKotlin --console=plain -q` | `android/`, JBR | Lần đầu lỗi smart cast/import ở adapter; sửa rồi chạy lại exit 0. |
| `./gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | `android/`, JBR | Exit 0; tạo lint report và debug APK. |
| `./gradlew.bat :domain:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain -q` | `android/`, JBR | Exit 0 sau chỉnh alert/drill-down; domain report XML: 3 tests, 0 failures. |
| `./gradlew.bat :app:assembleDebugAndroidTest --console=plain -q` | `android/`, JBR | Exit 0 sau cập nhật E2E. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.data.finance.reports.RoomFinanceReportsRepositoryTest vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (1 test)`. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceEntryUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (4 tests)` sau cài APK mới. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceEntryEndToEndTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (1 test)`; báo cáo hiển thị thu 500.000 sau ghi giao dịch và mở DB mã hóa. |
| `git diff --check` | Repo root | Exit 0; Git chỉ nhắc chuẩn hóa LF/CRLF của working copy. |
| `git push origin main`; `gh run watch 42187283884 --exit-status --interval 10` | GitHub commit `40a232b` | Push thành công; [Android CI run 42187283884](https://github.com/troll9x/agent-life/actions/runs/42187283884) success, unit tests/lint/debug APK PASS. |

## Kịch bản và chính sách

- Kỳ 15/10–15/11 không gồm 14/10 23:59 tại `Asia/Ho_Chi_Minh`; end exclusive. So kỳ trước cùng ngày bắt đầu và, khi đang ở kỳ hiện tại, cùng số ngày đã trôi qua (cắt tại cuối kỳ trước nếu cần).
- Refund theo `occurredAt` của khoản refund, không viết lại tháng expense gốc. Kỳ chỉ có refund có net expense âm và UI dùng số có dấu, không ép biểu đồ tròn.
- CSV lấy đúng `ReportSnapshot` đang hiển thị; text bắt đầu bằng `=`, `+`, `-`, `@` hoặc ký tự điều khiển được prefix apostrophe trước khi quote; cột tiền giữ số minor units có dấu để đối chiếu.
- Category dùng ID trong budget và report; đổi tên không làm mất liên kết lịch sử. Không có chức năng rename UI trong phạm vi task này.
- AVD `emulator-5554` được xác nhận tiến trình QEMU dùng `AgentLife_Isolated_Test_API_37`. Fixture UUID, tài khoản và tên nhóm giả, không có token/PII.

## Gate và phần chưa kiểm

- Không cần client ID, tenant approval, mẫu ngân hàng hoặc API live cho FIN-03. Chưa kiểm lưu CSV thủ công qua Android document picker, TalkBack và điện thoại vật lý; các phép tính/chuỗi CSV và UI Compose đã kiểm bằng test.
- RQ-FIN-09-02 còn gate AI consumer riêng; FIN-03 không tuyên bố đã kiểm AI/chart/coaching runtime.

## Bàn giao

- Task tiếp theo sẵn sàng: **FIN-04** (prerequisite FIN-01 đã VERIFIED), hoặc task độc lập khác theo board. Dừng ở FIN-03 trong lượt này.
