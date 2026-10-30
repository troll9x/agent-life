# FIN-01 Sổ giao dịch và số tiền chính xác

## Kết quả

Implementation: **VERIFIED**. External gate: **NOT_REQUIRED**. Bản kiểm: 29/10/2026, AVD `AgentLife_Isolated_Test_API_37` (API 37).

Ledger Room v5 lưu header giao dịch đã post và entry signed `Long`; số dư được tính từ opening balance và các entry sau `openingAsOf`, không lưu một cột balance mutable. Thu/chi có một entry, transfer có hai entry đối ứng cùng currency và được ghi trong một transaction Room. Operation ID unique, refund giới hạn theo expense gốc, reversal ghi entry đối dấu mà giữ nguyên giao dịch gốc. Không dùng Google Sheet, Double hoặc Float để tính tiền.

## Prerequisite và hợp đồng

- DATA-01 cung cấp Room foundation; FND-02 cung cấp `Money`, `CurrencyCode`, `Uuid`, `Outcome`. Repo không có `AGENTS.md`; áp dụng `06_AGENT_RULES.md`.
- Dùng hợp đồng tài chính trong `03_DOMAIN_RULES.md`, `04_DATA_CONTRACTS.md` và định nghĩa balance/report trong `13_FINANCE_ANALYTICS.md`; không dùng schema ledger minh họa cũ.
- Schema v4→v5 additive, được đăng ký trong `EncryptedDatabaseProvider`; không dùng destructive migration.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/`: Account, Category, FinancialTransaction, LedgerEntry, SourceEvidence, ImportCandidate, Budget, Reconciliation, command/repository contract và rule entry/balance thuần Kotlin.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/`: Room entity/DAO, repository, migration v4→v5. DAO chỉ insert header/entry; đường post dùng `withTransaction` và kiểm workspace/account/category/currency.
- `android/data/src/main/kotlin/vn/lifeos/data/db/AgentLifeDatabase.kt`, `.../security/EncryptedDatabaseProvider.kt`, `android/data/schemas/vn.lifeos.data.db.AgentLifeDatabase/5.json`: đăng ký schema/migration.
- `android/app/src/main/kotlin/vn/lifeos/app/AppContainer.kt`: đăng ký repository tối thiểu để FIN-02 dùng; chưa thêm UI tài chính trong FIN-01.
- `android/domain/src/test/.../finance/`, `android/data/src/androidTest/.../finance/`, `fixtures/finance/FIN-01-vectors.csv`: vector, overflow, rollback, validation, reversal, migration.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-01-01 | PASS | Vector opening bank 10.000.000 VND/cash 0, transfer 1.000.000 VND → 9.000.000/1.000.000; report income và expense đều 0. Test ép ID hai ledger entry trùng: entry thứ hai lỗi, header/entry đầu rollback, balance giữ 10.000.000/0. Room instrumentation và migration v4→v5 PASS. Evidence/candidate/budget/reconciliation không làm đổi số dư. | Chưa có form FIN-02; không dùng tiền/account thật. |
| RQ-FIN-01-02 | PASS | `LedgerRules` từ chối cùng account, khác currency, amount không dương; transfer tạo hai delta đối dấu tổng 0. `Money`/ledger dùng `Long` và `Math.*Exact`; `rg` trong hai package finance không thấy `Double`/`Float`. Test overflow và Room validation PASS. | Chưa hỗ trợ FX; cross-currency transfer bị từ chối. |

## Lệnh đã chạy

| Lệnh | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:assembleDebugAndroidTest :data:lintDebug :app:lintDebug :app:assembleDebug --console=plain -q` | PowerShell tại `android/` | Exit 0; domain 17/17, data JVM 9/9, app JVM 5/5, lint và debug APK PASS. |
| `adb -s emulator-5554 shell am instrument -w vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (25 tests)` ở vòng cuối, gồm finance và migration. |
| `adb -s emulator-5554 shell am instrument -w -e class 'vn.lifeos.data.finance.RoomFinanceRepositoryTest,vn.lifeos.data.finance.FinanceMigrationTest' vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | Cùng AVD, sau thay đổi cuối | `OK (6 tests)`. |
| `adb -s emulator-5554 install -r android/app/build/outputs/apk/debug/app-debug.apk`; `adb -s emulator-5554 shell am start -n vn.lifeos.app/.MainActivity` | Cùng AVD có dữ liệu app từ schema trước | `Success`, Activity mở, process chạy; UI dump thấy shell, không có lỗi migration trong logcat. |
| `rg -n '\b(Double|Float)\b' android/domain/src/main/kotlin/vn/lifeos/domain/finance android/data/src/main/kotlin/vn/lifeos/data/finance` | Repo root | Không có match. |
| `git diff --check` | Repo root | Exit 0, không có whitespace error. |

CI cho commit `99841d5`: [Android run 40855605012](https://github.com/troll9x/agent-life/actions/runs/40855605012) hoàn tất **success** ngày 30/10/2026 (`gh run view 40855605012 --json status,conclusion,url,headSha`); job build chạy unit tests, lint và debug APK.

## Kịch bản và dữ liệu

- Vector giả ở `fixtures/finance/FIN-01-vectors.csv`; không có số tài khoản, token hoặc thông tin cá nhân. AVD hiện mang tên `AgentLife_Isolated_Test_API_37` dù serial là `emulator-5554`; đã xác nhận bằng command line process, không chạy instrumentation trên AVD người dùng.
- Test kiểm operationId trùng, category sai direction, account khác workspace, currency khác, refund vượt expense, reversal một lần, opening/entry và overflow. Giao dịch post không có DAO update/delete; reversal là row riêng.
- Reconciliation là snapshot, không tự adjustment. Evidence/candidate không đổi ledger khi chỉ được lưu. Budget không âm; candidate confidence 0..10000 basis points.

## Gate và phần chưa kiểm

- Không cần quyền ngân hàng/client ID cho FIN-01. Chưa kiểm account thật, điện thoại vật lý hoặc thao tác form; UI nhập thu/chi/chuyển khoản thuộc FIN-02.
- Loan principal bị từ chối cho tới khi có `DebtAgreement` để tránh ghi debt ID mồ côi. FX chưa hỗ trợ. Replacement/edit đã post và luồng duyệt source/candidate sẽ thuộc các task tài chính sau; FIN-01 chỉ cấp nền ledger/schema.

## Bàn giao

- Task tiếp theo sẵn sàng: **FIN-02**, vì FIN-01 và UX-02 đã có code/hợp đồng. Không triển khai FIN-02 trong lượt này.
