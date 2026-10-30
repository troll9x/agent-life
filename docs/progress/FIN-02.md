# FIN-02 Nhập thu chi, tiền mặt và chuyển khoản

## Kết quả

Implementation: **VERIFIED**. External gate: **NOT_REQUIRED**. Bản kiểm: 30/10/2026, AVD `AgentLife_Isolated_Test_API_37` (API 37).

Khu Tài chính trong app cho tạo tài khoản CASH/BANK/SAVINGS có opening balance và ngày chốt, tạo nhóm thu/chi, chọn ví CASH mặc định, ghi thu/chi/chuyển khoản với số tiền dương, DateTimePicker, payee/note và xem số dư/danh sách giao dịch sau post. Form không cần nhập đơn vị tiền tệ; account dùng currency hồ sơ. Dữ liệu đi qua `ManualFinanceCommands` → `FinanceRepository` → Room mã hóa. Lưu tay tạo SourceEvidence `MANUAL` trong cùng DB transaction. FIN-02 không thêm refund/reversal UI hoặc Google Sheet.

## Prerequisite và hợp đồng

- FIN-01 có ledger Room v5, `Money`, `FinanceRepository`; UX-02 có workspace/profile và app shell offline. Repo không có `AGENTS.md`, áp dụng `06_AGENT_RULES.md`.
- App không gọi DB từ Compose. `FinanceFeature` do `ShellViewModel` sở hữu quan sát Room Flow; form chỉ gửi command. `operationId` và transaction ID được giữ bằng `rememberSaveable` cho cùng form và chỉ đổi sau post thành công.
- Chỉ chuyển giữa hai account khác nhau cùng currency; báo cáo transfer không tăng income/expense.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/commands/ManualFinanceCommands.kt`: parse `Money` theo locale `vi-VN`, validate workspace/account/category/destination/kind, post nguồn MANUAL.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/entry/`: Compose form, trạng thái loading/error/offline qua shell, ví mặc định lưu ID, điều phối Flow và thao tác.
- `android/app/src/main/kotlin/vn/lifeos/app/{AgentLifeScreen,MainActivity,ShellViewModel,AppContainer}.kt`: thay placeholder khu Finance, nối feature/ViewModel/DI.
- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/FinanceContract.kt`, `android/data/src/main/kotlin/vn/lifeos/data/finance/{FinanceDao,RoomFinanceRepository}.kt`: đọc categories/recent transactions và ghi SourceEvidence MANUAL cùng post; không đổi schema.
- `android/data/src/androidTest/kotlin/vn/lifeos/data/finance/RoomFinanceRepositoryTest.kt`, `android/app/src/androidTest/kotlin/vn/lifeos/app/{FinanceEntryUiTest,FinanceEntryEndToEndTest,ShellNavigationTest}.kt`: kiểm offline/reopen, thao tác lặp, Compose, điều hướng.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-02-01 | PASS | App instrumentation: form Compose ghi thu 500.000 vào ví CASH, đóng/mở DB mã hóa và đọc lại balance/transaction. Room test ghi CASH income 1.500.000 và expense 250.000, đóng/mở DB còn balance 1.250.000, report income/net expense đúng và evidence MANUAL. Ca này cũng PASS khi tắt Wi-Fi và mobile data trên AVD. APK thật mở màn Tài chính trên AVD. | Chưa kiểm điện thoại vật lý; form nhập account/category do người dùng tạo. |
| RQ-FIN-02-02 | PASS | Room test chuyển bank 10.000.000→cash 1.000.000 giữ 9.000.000/1.000.000, income/expense 0; post cùng operation ID lần nữa trả Conflict và số transaction giữ 1. Test cash submit lại cả sau reopen trả Conflict, không tăng số giao dịch. Compose test bấm Lưu cùng form hai lần: transaction ID và operation ID giữ nguyên. | Retry trùng hiện báo “đã lưu/kiểm tra danh sách”; không tạo giao dịch mới. |

## Lệnh đã chạy

| Lệnh | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :data:lintDebug :app:lintDebug :app:assembleDebug --console=plain -q` | PowerShell tại `android/` | Exit 0; domain/unit, lint, app và test APK PASS. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (26 tests)`, gồm income/expense CASH, transfer, rollback và migration. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD | Vòng đầu FAIL 2 ca: assertion navigation cũ còn chờ placeholder Finance; DatePicker planner timeout. Đã cập nhật navigation test; hai ca chạy lại riêng `OK (2 tests)`, sau đó toàn suite `OK (28 tests)`. Sau khi thêm test end to end, một vòng suite gặp crash AVD và timeout teardown; đã cô lập DB fixture, chạy lại toàn suite `OK (29 tests)` (các phase probe của PLAN-03 skip có chủ ý). |
| `adb -s emulator-5554 shell am instrument -w -e class 'vn.lifeos.app.FinanceEntryEndToEndTest' vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD, DB fixture riêng | `OK (1 test)`; form → Room mã hóa → đóng/mở → số dư/giao dịch còn. |
| `adb -s emulator-5554 shell svc wifi disable`; `svc data disable`; chạy `RoomFinanceRepositoryTest#manualCashIncomeAndExpenseStayAfterReopenAndDuplicateSubmit`, rồi bật lại | Cùng AVD | `OK (1 test)` khi tắt mạng; Wi-Fi/mobile data đã bật lại. |
| `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; mở `MainActivity`, chạm khu Finance và kiểm UI dump | Cùng AVD | Install `Success`, màn “Tài khoản và số dư” xuất hiện, không có lỗi migration/storage trong logcat. |
| `git diff --check` | Repo root | Exit 0, không có whitespace error. |

## Kịch bản và dữ liệu

- Chỉ dùng UUID/tên fixture; test end to end dùng database mã hóa riêng để không tác động dữ liệu app và test khác. AVD đang có serial `emulator-5554` nhưng command line process xác nhận là `AgentLife_Isolated_Test_API_37`; không chạy instrumentation trên AVD người dùng.
- Form cho chọn source/destination khác nhau; use case từ chối amount không dương, category sai direction, account khác workspace hoặc transfer khác currency. Ví CASH là Account kind CASH, không có tổng tiền mặt độc lập ngoài ledger.
- `rememberSaveable` giữ operation ID cho draft qua tái tạo UI; Flow cập nhật balance/list sau post. Unit/Room test xác nhận retry cùng operation ID sau reopen vẫn một giao dịch.

## Gate và phần chưa kiểm

- Không cần client ID, ngân hàng hay mạng. Chưa kiểm điện thoại vật lý, TalkBack thủ công và hành vi lưu draft khi người dùng force-stop app. Dữ liệu đã post được kiểm sau mở lại DB.
- FIN-02 không có sửa giao dịch đã post, refund, adjustment hoặc chart/budget; các task sau xử lý. UX-01 còn gate xoay form tài chính thủ công; lượt này chưa đánh PASS cho UX-01.

## Bàn giao

- Task tiếp theo sẵn sàng: **FIN-08** và **FIN-09** (cùng phụ thuộc FIN-02); FIN-03 còn chờ cả hai. Không triển khai task khác trong lượt này.
