# FIN-08 Sửa khoản đã post bằng reversal

## Kết quả

Implementation: **VERIFIED**. External gate: **NOT_REQUIRED**. Bản kiểm: 06/11/2026, AVD `AgentLife_Isolated_Test_API_37` (API 37).

Người dùng mở lịch sử Tài chính, chọn giao dịch thu/chi/chuyển đã post, nhập số tiền, tài khoản, nhóm và ngày sửa. App ghi một reversal đối dấu toàn bộ entry gốc và một replacement liên kết gốc trong cùng Room transaction. Không xóa hay cập nhật amount/entry cũ. Form giữ hai operation ID và transaction ID ổn định qua retry; DB chặn reversal thứ hai. Report tiếp tục cộng cả gốc và reversal theo ngày ghi nhận, nên sửa khác kỳ không âm thầm đổi kỳ cũ.

## Prerequisite và hợp đồng

- FIN-02 VERIFIED: ledger Room v5, `FinanceRepository`, `ManualFinanceCommands`, UI Tài chính. Repo không có `AGENTS.md`; áp dụng `06_AGENT_RULES.md`.
- Giữ `FinancialTransaction.reversesId`, `replacementOfId`, unique index `reversesId` và `workspaceId+operationId` đã có; không đổi Room schema. `expectedPostedAt` kiểm phiên bản nguồn. Giao dịch có refund hoặc đã reverse trả Conflict với hướng dẫn kiểm lịch sử; không tự đảo dư.
- `RoomReversalRepository` gọi hai post trong một `withTransaction`; lỗi replacement ném abort để rollback reversal và evidence. Compose chỉ gửi command qua `FinanceFeature`/ViewModel.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/reversal/ReplacePostedTransaction.kt`: command, kết quả và interface sửa giao dịch; `FinanceContract.kt` thêm link replacement; `ManualFinanceCommands.kt` tách chuẩn bị command để tái dùng validation.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/reversal/RoomReversalRepository.kt`: ghi hai khoản nguyên tử, kiểm phiên bản/refund/reversal; `FinanceDao.kt` và `RoomFinanceRepository.kt` đọc link replacement, giữ reversal đúng và ghi link.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/history/FinanceHistorySection.kt`: chuỗi lịch sử, tác động entry và form sửa; `finance/entry/FinanceEntryScreen.kt`, `FinanceFeature.kt`, `AppContainer.kt`, `ShellViewModel.kt`: nối UI, trạng thái và service.
- `android/data/src/androidTest/kotlin/vn/lifeos/data/finance/RoomReversalRepositoryTest.kt`, `android/app/src/androidTest/kotlin/vn/lifeos/app/FinanceEntryUiTest.kt`: kiểm nghiệp vụ và Compose.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-08-01 | PASS | Room test: opening 10.000.000, expense 500.000, sửa 600.000 cùng kỳ → balance 9.400.000, net expense 600.000, 3 header bất biến. Test khác: sửa khác kỳ để tháng gốc 500.000, tháng sửa 100.000. Compose hiện preview thay đổi số dư -100.000. | Lịch sử UI đang đọc 50 giao dịch gần nhất. |
| RQ-FIN-08-02 | PASS | Room test transfer đảo đúng cả hai entry; balance bank/cash = 9.400.000/600.000 và net expense = 0. Room test hai repository cùng post cạnh tranh: 1 Success, 1 Conflict, tổng 3 header. Submit lại operation cũ trả Conflict. Test replacement lỗi accountId xác nhận reversal rollback và sửa hợp lệ sau đó vẫn được. Compose giữ ID giữa hai lần bấm Lưu. | Race được kiểm trên hai client repository cùng Room database trên một AVD; chưa kiểm sync nhiều thiết bị. |

## Lệnh đã chạy

| Lệnh | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `./gradlew.bat :domain:test :data:compileDebugKotlin :app:compileDebugKotlin --console=plain -q` | PowerShell trong `android/`, JBR Android Studio | Vòng đầu lỗi Kotlin smart cast, đã sửa; vòng sau exit 0. |
| `./gradlew.bat :data:assembleDebugAndroidTest :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :app:assembleDebug --console=plain -q` | Cùng máy | Exit 0; lint report và debug APK được tạo. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.data.finance.RoomReversalRepositoryTest vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (5 tests)`. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD | `OK (31 tests)`. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceEntryUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD | Vòng đầu test fixture thiếu scroll semantics; đã sửa fixture, vòng sau `OK (2 tests)`. |
| `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; mở `MainActivity`, vào Finance và cuộn xuống | Cùng AVD | Install `Success`; UI dump thấy “Tài khoản và số dư”, “Lịch sử giao dịch”, “Sửa bằng giao dịch mới”. |
| `git diff --check` | Repo root | Exit 0. |

## Kịch bản và dữ liệu

- Chỉ dùng UUID/fixture giả. AVD `emulator-5554` được kiểm command line là `AgentLife_Isolated_Test_API_37`; không chạy test trên AVD của người dùng.
- Kịch bản rollback cố tình dùng tài khoản chưa lưu để replacement lỗi sau khi reversal đã được post trong transaction; sau abort, cả reversal và replacement đều không tồn tại.
- Refund gốc 100.000 rồi thử sửa expense gốc trả Conflict; không tạo reversal. Sửa khác kỳ giữ contribution kỳ gốc và tạo -500.000+600.000 trong kỳ sửa.

## Gate và phần chưa kiểm

- Không cần client ID, quyền tenant, template ngân hàng hoặc mạng. Chưa kiểm điện thoại vật lý, TalkBack thủ công và sync nhiều thiết bị. Lịch sử UI hiện giới hạn 50 giao dịch gần nhất; điều hướng lịch sử dài/paging cần xử lý riêng nếu mở rộng phạm vi.
- UI chỉ sửa amount/account/category/ngày cho thu/chi/chuyển. Refund/adjustment thuộc FIN-09, không được đánh PASS ở đây.

## Bàn giao

- Task tiếp theo sẵn sàng: **FIN-09**. FIN-03 còn chờ FIN-09.
