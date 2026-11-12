# FIN-10 Đối soát tài khoản và chênh lệch

## Kết quả

Implementation: **IMPLEMENTED**. Snapshot, đối soát, UI, report và advice quality contract đã kiểm offline. **RQ-FIN-10-02 chưa VERIFIED toàn bộ** vì module lời khuyên AI chưa có consumer runtime. External gate: **EXTERNAL_BLOCKED** kế thừa nguồn ngân hàng thật của FIN-04. Kiểm ngày **12/11/2026**, JBR Android Studio, AVD riêng `AgentLife_Isolated_Test_API_37` (`emulator-5554`, API 37).

Commit code được kiểm: `0c57600`.

User nhập observed balance theo account/currency và mốc thời gian bằng picker. Expected được query từ ledger tại cùng mốc, difference = observed − expected. Lưu snapshot và xác nhận đã đối soát không tạo entry. Snapshot gốc giữ nguyên expected/observed/difference; trạng thái RESOLVED chỉ được ghi khi ledger hiện tại tại mốc đó bằng observed. Có xem ledger và candidates quanh ±7 ngày. Điều chỉnh mở form lý do và dialog riêng, gọi flow FIN-09; không tự đánh dấu resolved sau điều chỉnh.

## Prerequisite và hợp đồng

- Repo sạch lúc bắt đầu, không có AGENTS.md. Áp dụng `06_AGENT_RULES.md`, architecture/domain/data contracts và finance analytics 2.1 trong `AI_LifeOS_Agent_Pack/`.
- FIN-04 có inbox/evidence/candidates. FIN-09 có `ManualRefundAdjustmentCommands.adjust` và `FinanceActions.postAdjustment`; vế AI của FIN-09 vẫn NOT_RUN. Không dựng module AI khác để né gate.
- Tái dùng `Reconciliation`, `ReconciliationEntity` và bảng `reconciliations` từ FIN-01. Không cần migration/version mới: DB vẫn **v8**, schema không đổi. Chỉ thêm DAO vào cùng AgentLifeDatabase.
- `ReconciliationView` có expected tính lại để nhận biết ledger đổi; resolved chỉ hợp lệ nếu state RESOLVED và observed vẫn khớp. Report tính unreconciled nếu không có mốc resolved, balance tại mốc thay đổi, hoặc có entry sau mốc kiểm tới cutoff báo cáo. Query/report cùng workspace/currency/account và kỳ [start,end).
- `FinancialDataQuality` mang UNRECONCILED, MISSING_SOURCE, UNKNOWN_SOURCE_COVERAGE, PENDING_CANDIDATE vào `ReportInput` → `FinanceReportUseCase` → `ReportSnapshot`. UI báo cáo hiển thị các cờ; snapshot/candidate/ledger/connection changes làm refresh báo cáo.
- Chỉ trạng thái mailbox/listener không chứng minh đủ giao dịch ngân hàng cho từng account/kỳ. Vì repo chưa có chứng cứ/binding độ phủ này, reader giữ `sourceCoverageKnown=false`; không suy nguồn OK thành thu/chi đầy đủ. Nguồn vắng/từ chối/lỗi giữ MISSING_SOURCE; PENDING/NEEDS_REVIEW/ERROR được tính là khoản cần kiểm tra, kể cả thiếu thời gian.
- `qualityForAdvice()` chỉ chuyển flags/count và boolean `canClaimIncomeExpenseComplete`, không raw evidence, account label hay số tài khoản. Adapter này đã kiểm; consumer AI thực tế chưa có và chưa kiểm.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/reconciliation/ReconciliationContract.kt`: capture/review/repository, view, quality flags và advice gate.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/reconciliation/{ReconciliationDao,RoomReconciliationRepository,RoomFinancialQualityReader}.kt`: snapshot/resolve/audit atomic, ledger/candidate review, quality đọc cùng transaction với report facts.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/reconciliation/{ReconciliationFeature,ReconciliationSection,FinanceQualityNotice}.kt`: form date/time picker, offline/loading/empty/error/retry, review, điều chỉnh có lý do và quality notice.
- Registration tối thiểu: `AgentLifeDatabase.kt` thêm DAO; `AppContainer.kt`, `ShellViewModel.kt`, `FinanceFeature.kt`, `FinanceEntryScreen.kt` nối state/actions và gọi flow FIN-09 có sẵn.
- Nối step 5 vào consumer có thật: domain `finance/reports/FinanceReports.kt` thêm quality vào input/output và giữ qua use case; data `RoomFinanceReportsRepository.kt` đọc quality cùng facts với injected Clock; app `FinanceReportsSection.kt` hiện notice. Không đổi phép cộng thu/chi/CSV hay viết consumer AI giả.
- Tests: domain `FinancialQualityTest`, data `RoomReconciliationRepositoryTest`, app `FinanceReconciliationUiTest`.
- Checklist bản repo, TASK_BOARD và evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-10-01 | PASS offline | Room: opening 10 triệu, expense 600 nghìn đúng mốc → expected 9.400.000; observed 9.300.000 → difference −100.000. Expense 200 nghìn sau mốc không bị tính vào expected; balance hiện tại 9.200.000 không bị thay khi capture. UI/E2E giữ balance 9.400.000 tới khi confirm điều chỉnh riêng; reopen giữ snapshot gốc. | AVD API 37, chưa thiết bị vật lý. |
| RQ-FIN-10-02 | NOT_RUN toàn bộ; report/advice contract PASS | 3 candidates pending → report.quality.pendingCandidateCount=3, PENDING_CANDIDATE/MISSING_SOURCE, advice gate `canClaimIncomeExpenseComplete=false`; report expense vẫn chỉ 600.000. UI thấy 3 khoản chờ nhập và cảnh báo chưa đầy đủ. Backdated entry sau khi resolve làm unreconciled trở lại. | Chưa có AI/coaching runtime để kiểm lời khuyên nhận/dùng cờ. Checklist giữ unchecked; nguồn ngân hàng thật chưa có. |

## Lệnh đã chạy

Trong `android/`, `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. Adb dùng SDK tại `C:\Users\admin\AppData\Local\Android\Sdk\platform-tools\adb.exe`, chạy từ repo root.

| Lệnh nguyên văn | Kết quả quan sát |
| --- | --- |
| `./gradlew.bat :domain:test :data:compileDebugKotlin --console=plain -q` | Exit 0 sau domain/data. |
| `./gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | Lần đầu lỗi test HTTP cũ `coroutineCancellationCancelsInFlightCall`: `server.takeRequest(2 seconds)` trả null. Không sửa code HTTP/test đó; chạy lại cùng lệnh exit 0. XML cuối SourceHttpClientTest: 9 tests, 0 failures; FinancialQualityTest: 2 tests, 0 failures. Lint/APK/test APK thành công. |
| `adb devices`; kiểm QEMU command line bằng `Get-CimInstance Win32_Process` | `emulator-5554 device`; xác nhận AVD riêng `AgentLife_Isolated_Test_API_37`. |
| `adb -s emulator-5554 install -r android/data/build/outputs/apk/androidTest/debug/data-debug-androidTest.apk`; `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; `adb -s emulator-5554 install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | Cả ba `Success`. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | `OK (55 tests)`: 4 reconciliation mới, regression schema v8/ledger/debt/scheduled/goals/inbox/report/planner/privacy/SQLCipher. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceReconciliationUiTest,vn.lifeos.app.FinanceEntryUiTest,vn.lifeos.app.FinanceInboxUiTest,vn.lifeos.app.FinanceScheduledUiTest,vn.lifeos.app.FinanceEntryEndToEndTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | `OK (8 tests)`: reconciliation E2E DB mã hóa, review/FIN-09 confirmation, report quality notice và regression tài chính. |
| `git diff --check` | Exit 0; chỉ cảnh báo LF/CRLF. |
| `git push origin main`; `gh run watch 43170677038 --exit-status --interval 15` | Commit `0c57600` đã đẩy; [Android CI 43170677038](https://github.com/troll9x/agent-life/actions/runs/43170677038) success, unit tests/lint/debug APK PASS. |

## Kịch bản và dữ liệu

- Chỉ UUID/tên ví/evidence fixture; không tài khoản ngân hàng thật, API/token hoặc raw notification/email. Không Google Sheets.
- Cùng mốc có tính entry tại mốc; entry sau đó 1 giây không thay expected snapshot. Capture không thêm transaction; duplicate account/asOf, sai currency/workspace, trước opening, tương lai và overflow bị từ chối.
- Resolve khi còn gap bị từ chối. FIN-09 reason rỗng bị từ chối; điều chỉnh −100.000 có reason tạo ADJUSTMENT, expense vẫn 600.000 và income 0. Sau confirm resolved, expected/difference gốc vẫn 9.400.000/−100.000; current expected 9.300.000. Entry mới backdated làm hiệu lực đối soát mất và report có UNRECONCILED.
- Ba khoản pending chưa post không được cộng thu/chi và giữ gate chưa đầy đủ. Nguồn NOTIFICATION PERMISSION_DENIED giữ missing-source, không đổi thành empty success. Coverage chưa chứng minh được giữ UNKNOWN_SOURCE_COVERAGE.
- UI thực chạy: chọn ví, nhập 9.300.000, lưu snapshot thấy difference −100.000 mà balance 9.400.000. Mở review, form điều chỉnh không cho review khi thiếu reason; mở dialog vẫn không đổi balance; confirm mới 9.300.000. Xác nhận đối soát riêng và reopen DB mã hóa giữ snapshot/resolved.

## Gate và phần chưa hoàn tất

- **AI consumer NOT_RUN:** khi INSIGHT/AI runtime có code, phải kiểm context và lời khuyên thực tế giữ UNRECONCILED/MISSING_SOURCE/PENDING_CANDIDATE/UNKNOWN_SOURCE_COVERAGE, không tuyên bố thu/chi đầy đủ khi 3 candidates còn pending. Chỉ khi đó mới tick RQ-FIN-10-02 và đổi FIN-10 sang VERIFIED toàn bộ.
- **EXTERNAL_BLOCKED kế thừa FIN-04:** cần connector/parser, mẫu ngân hàng và quyền nguồn thật để kiểm coverage tài khoản/kỳ. Không suy kết nối mailbox thành coverage hoàn chỉnh.
- Thiết bị vật lý **NOT_RUN**; đã kiểm AVD API 37 và SQLCipher. Không cần đổi schema/migration cho task này.

## Bàn giao

Task tiếp theo sẵn sàng về code/fixture: **NOTI-01 — cấp quyền và NotificationListenerService** (DATA-02/UX-02/FIN-04 có code). Giữ các gate nguồn thật và AI consumer; dừng tại FIN-10.
