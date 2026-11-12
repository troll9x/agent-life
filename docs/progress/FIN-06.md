# FIN-06 Theo dõi cho vay và đi vay

## Kết quả

Implementation: **VERIFIED bằng fixture offline**. External gate: **NOT_REQUIRED** cho FIN-06. Kiểm ngày **12/11/2026**, JBR Android Studio, AVD riêng `AgentLife_Isolated_Test_API_37` (`emulator-5554`, API 37).

Commit nghiệp vụ: `9ea1ce4`; follow-up bổ sung hiển thị gốc ban đầu và evidence có kiểm app riêng bên dưới.

Người dùng tạo khoản đi vay/cho vay, tên gợi nhớ đối tác, gốc ban đầu và hạn trả tùy chọn. Gốc ban đầu là dư nợ đã có, không tạo dòng tiền. Giải ngân/hoàn trả ghi `LOAN_PRINCIPAL_IN/OUT`; lãi và phí ghi các transaction thu/chi riêng, chọn nhóm riêng. UI có bước review từng thành phần, date/time picker, dư gốc, tổng lãi/phí và lịch sử biến động. Sửa sai bằng đảo toàn bộ biến động có lý do và audit; giữ nguyên ledger gốc.

## Prerequisite và hợp đồng

- FIN-03 đã VERIFIED. Dùng ledger, báo cáo và Room DB hiện có; repo sạch lúc bắt đầu, không có `AGENTS.md`, áp dụng `06_AGENT_RULES.md`. Bộ đặc tả thực tế nằm tại `AI_LifeOS_Agent_Pack/`.
- `DebtAgreement`: role LENDER/BORROWER, currency, counterpartyLabel, openingPrincipalMinor; bổ sung UUID counterpartyId cục bộ để xác minh đối tác trên command và dueDate. Không có dữ liệu liên hệ thật. `DebtMovement`: debtId, principal transaction, delta gốc, interest/fee transaction, thời điểm, liên kết bút toán đảo.
- Money Long minor units, kiểm overflow; không cộng các currency. Outstanding = opening principal + tổng principal delta của mọi movement, kể cả bút toán đảo. Borrower advance tăng tiền/gốc; borrower repay giảm tiền/gốc; lender dùng chiều tiền ngược lại. Lãi/phí borrower là expense, lender là income.
- Movement, tất cả component ledger/evidence và audit cùng một Room transaction. Mọi failure sau khi ghi component đều abort transaction ngoài. Unique operation ID chống lặp; serialize transaction chống hai khoản trả đồng thời vượt gốc.
- Luồng `FinanceRepository.post` thông thường tiếp tục từ chối principal/debt link trực tiếp. Chỉ adapter debt gọi cổng internal khi đang ghi movement; đảo/refund giao dịch nợ qua luồng thông thường bị chặn để tránh lệch principal.
- FIN-04 còn gate notification/email thật; FIN-06 hoạt động nhập tay ngoại tuyến và không đóng gate đó.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/debt/DebtContract.kt`: entity, command, repository, tính/kiểm gốc và chiều ledger.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/debt/{DebtEntities,DebtDao,DebtMigration,RoomDebtRepository}.kt`: schema, quan sát snapshot nhất quán, ghi atomic, kiểm workspace/counterparty/currency/overpayment/time, correction và audit.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/debt/{DebtFeature,DebtSection}.kt`: state/command và Compose; loading/empty/error/retry/offline, review và lịch sử.
- Registration tối thiểu: `AgentLifeDatabase.kt` version 7/DAO/entities, `EncryptedDatabaseProvider.kt` migration 6→7, schema `android/data/schemas/vn.lifeos.data.db.AgentLifeDatabase/7.json`, `RoomFinanceRepository.kt` cổng internal cho debt + chặn sửa qua luồng khác; `AppContainer.kt`, `ShellViewModel.kt`, `FinanceFeature.kt`, `FinanceEntryScreen.kt` nối state/actions/UI.
- Tests: domain `DebtRulesTest`, data `DebtMigrationTest`/`RoomDebtRepositoryTest`, app `FinanceDebtUiTest`/`FinanceDebtEndToEndTest`.
- Checklist bản repo `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-06-01 | PASS offline | Room borrower: vay 5.000.000, trả gốc 1.000.000 → outstanding 4.000.000, balance từ 10.000.000 thành 14.000.000, income/expense 0. Lender: opening principal 1.000.000, giải ngân 5.000.000, thu gốc 1.000.000 → outstanding 5.000.000; phần gốc không vào thu/chi. | Chưa kiểm thiết bị vật lý. |
| RQ-FIN-06-02 | PASS offline | Room: lãi 100.000 → expense 100.000; khoản tiếp theo tách gốc 500.000/lãi 50.000/phí 20.000 thành 3 ledger headers, category lãi/phí riêng. Đảo khoản này khôi phục principal và expense, có audit, giữ gốc. Mỗi principal movement đối chiếu entry; reopen giữ tổng và lịch sử. Compose review chưa confirm không gửi command. E2E DB mã hóa: gốc giảm 5→4 triệu, chi phí chỉ 100.000 và reopen giữ dữ liệu. | Lãi/phí do người dùng nhập, không dự báo khả năng trả nợ. |

## Lệnh đã chạy

Trong `android/`, đặt `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`. Các lệnh adb chạy từ repo root với SDK `C:\Users\admin\AppData\Local\Android\Sdk\platform-tools\adb.exe`.

| Lệnh nguyên văn | Kết quả quan sát |
| --- | --- |
| `./gradlew.bat :domain:test :data:compileDebugKotlin :app:compileDebugKotlin --console=plain -q` | Exit 0, code production biên dịch. |
| `./gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | Lần đầu compile test lỗi import wildcard trùng `AppError`; sửa import cụ thể và chạy lại exit 0. Domain DebtRulesTest XML: 2 tests, 0 failures; lint không có error (app có warning/hint hiện hữu, không có location trong debt). Debug APK tạo thành công. |
| `./gradlew.bat :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest --console=plain -q` | Exit 0 sau bổ sung kiểm đối chiếu movement và E2E. |
| `adb devices` | Thiết bị `emulator-5554` online; QEMU command line xác nhận AVD riêng `AgentLife_Isolated_Test_API_37`. |
| `adb -s emulator-5554 install -r android/data/build/outputs/apk/androidTest/debug/data-debug-androidTest.apk`; `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; `adb -s emulator-5554 install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` | Cả ba `Success`. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | `OK (44 tests)`, gồm 1 debt migration + 3 debt repository, regression ledger/reversal/goals/inbox/reports và SQLCipher. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceDebtUiTest,vn.lifeos.app.FinanceDebtEndToEndTest,vn.lifeos.app.FinanceEntryUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | `OK (6 tests)`: review, E2E mã hóa và regression tài chính. |
| `git diff --check` | Exit 0, chỉ cảnh báo LF/CRLF của Git. |
| `./gradlew.bat :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --console=plain -q` | Exit 0 sau bổ sung hiển thị gốc ban đầu trên thẻ khoản vay. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceDebtUiTest,vn.lifeos.app.FinanceDebtEndToEndTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | Cài lại app/test APK bản cuối (`Success`), `OK (2 tests)`. |
| `git push origin main`; `gh run watch 43144466885 --exit-status --interval 15` | Commit `9ea1ce4` đã đẩy; [Android CI 43144466885](https://github.com/troll9x/agent-life/actions/runs/43144466885) success, unit tests/lint/debug APK PASS. |

## Kịch bản và dữ liệu

- Chỉ dùng fixture UUID/tên đối tác/tài khoản giả. Không API, token, dữ liệu cá nhân hoặc tích hợp Google Sheets.
- Borrower 5 triệu → hoàn gốc 1 triệu → 4 triệu; lãi riêng 100 nghìn → expense 100 nghìn. Lender kiểm chiều tiền ngược và interest/fee income.
- Sai currency, counterparty ID, workspace, account ID, thời điểm tương lai/lùi trước movement gần nhất đều bị từ chối; overpayment và đảo giải ngân làm gốc âm bị từ chối. Domain kiểm amount âm/0/overflow.
- Replay operation và trùng movement ID trả Conflict. Trùng ID sau khi đã ghi component, hoặc nhóm phí sai chiều sau principal/interest, đều rollback: không có header mồ côi và số dư không đổi. Hai lệnh trả 800 nghìn khi còn 900 nghìn: chỉ một lệnh thành công, còn 100 nghìn.
- Migration 6→7 validate schema, giữ nguyên ledger entry 100 và savings account link từ DB v6. Không destructive fallback.
- Màn Compose thực tế trong test hiển thị outstanding 5 triệu, hạn đã nhập, form gốc/lãi/phí, selector nhóm, date/time picker và dialog xác nhận. E2E thấy outstanding cập nhật thành 4 triệu sau confirm và dữ liệu được giữ khi mở lại DB mã hóa.

## Gate và phần chưa kiểm

- FIN-06 không cần client ID, tenant hay nguồn ngân hàng; không có external gate riêng. Thiết bị vật lý **NOT_RUN**; đã kiểm AVD API 37.
- Nhập lịch sử theo thứ tự thời gian: command không được trước movement gần nhất hoặc ở tương lai. Muốn sửa khoản đã ghi thì đảo có lý do và nhập replacement mới; không sửa/xóa posted ledger.
- Gốc opening đã có chỉ đặt mốc dư nợ; người dùng tự bảo đảm opening balance tài khoản tương ứng. Lãi/phí không được tự tính hoặc dự báo.
- Gate nguồn notification/email thật của FIN-04 vẫn mở cho task nguồn tương ứng.

## Bàn giao

Task tiếp theo sẵn sàng: **FIN-07** (FIN-02, FIN-04, PLAN-03 có code/fixture; tiếp tục truyền gate nguồn thật). Dừng tại FIN-06.
