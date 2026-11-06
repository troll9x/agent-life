# FIN-04 Hàng chờ nhập và ghép bằng chứng chống trùng

## Kết quả

Implementation: **VERIFIED bằng fixture offline**. External gate: **EXTERNAL_BLOCKED** đối với sự kiện notification/email ngân hàng thật. Kiểm ngày 06/11/2026 trên JBR Android Studio và AVD riêng `AgentLife_Isolated_Test_API_37` (API 37).

Candidate được lưu riêng khỏi ledger; `ingest` không đổi số dư. Identity evidence gồm workspace, connection/source, event ID, revision; ingest lặp trả candidate đã có. Match ưu tiên bankReference đã xác nhận + account + direction + amount/currency; cùng số tiền/thời gian chỉ là gợi ý. Người dùng chọn `Duyệt và ghi sổ`, `Ghép vào giao dịch này` hoặc `Bỏ qua`. Quyết định và evidence link ghi trong một Room transaction; approve qua ledger hiện có. Hai match khả dĩ giữ `NEEDS_REVIEW`. Revision mới của cùng event thay candidate chưa quyết định, không sửa ledger đã post. UI chỉ hiện metadata đã parse, không lưu raw notification/mail trong bảng finance.

## Prerequisite và hợp đồng

- FIN-01 đã VERIFIED: Room v5 có sẵn `SourceEvidence`, `ImportCandidate`, immutable ledger và unique operation ID. Không cần migration/schema mới, không tạo FinanceRepository/DB thứ hai. Repo không có `AGENTS.md`; áp dụng `06_AGENT_RULES.md`.
- Dùng domain `SourceEvidence`, `ImportCandidate`, `CandidateState`, `PostFinanceCommand`; `Money` là Long minor units. `bankReferenceVerified` buộc parser xác nhận trước khi dùng reference để match chắc chắn. `missingFields` chỉ nhận tên field trong allowlist, ngăn raw text lẫn vào metadata lưu dài hạn.
- Các connector notification/mail và parser mẫu ngân hàng chưa thuộc FIN-04; `InboundFinanceEvidence` là cổng nhận metadata cho task nguồn tiếp theo. Không gọi API thật.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/imports/FinanceInbox.kt`: input/decision contract và matcher thuần Kotlin.
- `android/data/src/main/kotlin/vn/lifeos/data/finance/imports/{FinanceInboxDao,RoomFinanceInboxRepository}.kt`: truy vấn match, idempotent ingest, xử lý revision, quyết định và evidence link atomic.
- `android/app/src/main/kotlin/vn/lifeos/app/finance/inbox/FinanceInboxSection.kt`: hàng chờ, trạng thái loading/empty/error, gợi ý có ngày/số tiền, nút duyệt/ghép/bỏ qua.
- `android/data/src/main/kotlin/vn/lifeos/data/db/AgentLifeDatabase.kt`, `android/app/src/main/kotlin/vn/lifeos/app/{AppContainer,ShellViewModel}.kt`, `android/app/src/main/kotlin/vn/lifeos/app/finance/entry/{FinanceFeature,FinanceEntryScreen}.kt`: registration DAO/DI và state/commands tối thiểu.
- Test domain `EvidenceMatcherTest.kt`, Room `RoomFinanceInboxRepositoryTest.kt`, Compose `FinanceInboxUiTest.kt`, cập nhật `FinanceEntryUiTest.kt`/`FinanceEntryEndToEndTest.kt`.
- Checklist repo, TASK_BOARD và evidence này.

## Đối chiếu yêu cầu

| ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FIN-04-01 | PASS bằng fixture | Room: lương manual 200.000; notification cùng khoản gợi ý fingerprint, quyết định duplicate nối vào manual; email cùng reference gợi ý verified reference, approve bị Conflict, duplicate nối vào manual. Một transaction, ba evidence, balance 1.200.000; ingest email lặp không thêm khoản. | Notification/email thật chưa chạy; connector/parser chưa có. |
| RQ-FIN-04-02 | PASS bằng fixture | Room: hai expense 200.000 cùng phút `REF-A`/`REF-B` được approve thành hai transaction, balance 600.000 từ 1.000.000. Hai manual income cùng phút không ref → 2 gợi ý, `NEEDS_REVIEW`; ignore không đổi balance. Domain matcher 4/4 kiểm ref khác không ghép. | Chưa kiểm mẫu ngân hàng thật. |

## Lệnh đã chạy

| Lệnh | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `./gradlew.bat :domain:test :data:compileDebugKotlin --console=plain -q` | `android/`, JBR | Lần đầu lỗi smart cast Kotlin ở adapter; sửa và lệnh build sau PASS. |
| `./gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | `android/`, JBR | Exit 0, lint report và debug APK được tạo. |
| `./gradlew.bat :domain:test :data:assembleDebugAndroidTest :app:assembleDebugAndroidTest :app:assembleDebug --console=plain -q` | `android/`, JBR | Exit 0 sau chỉnh matcher, revision và fixture; domain matcher XML: 4 tests, 0 failures. |
| `./gradlew.bat :app:assembleDebugAndroidTest --console=plain -q` | `android/`, JBR | Exit 0 sau nối E2E với inbox. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.data.finance.imports.RoomFinanceInboxRepositoryTest vn.lifeos.data.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (3 tests)` sau cài test APK cuối. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceInboxUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (1 test)`; thấy hàng chờ, nguồn, số tiền, reference, gợi ý và ba quyết định. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceEntryUiTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (4 tests)`, regression màn Tài chính. |
| `adb -s emulator-5554 shell am instrument -w -e class vn.lifeos.app.FinanceEntryEndToEndTest vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner` | AVD riêng API 37 | `OK (1 test)` với Room inbox và DB mã hóa. |
| `git diff --check` | Repo root | Exit 0; Git chỉ nhắc LF/CRLF trong working copy. |
| `git push origin main`; `gh run watch 42190374158 --exit-status --interval 10` | GitHub commit `6753a52` | Push thành công; [Android CI run 42190374158](https://github.com/troll9x/agent-life/actions/runs/42190374158) success, unit tests/lint/debug APK PASS. |

## Kịch bản, dữ liệu và gate

- Chỉ dùng UUID, tài khoản và event ID fixture. QEMU của `emulator-5554` được xác nhận là AVD `AgentLife_Isolated_Test_API_37`. Không dùng token, email hay số tài khoản thật.
- Ingest cùng identity không tạo candidate mới. Event revision mới thay candidate pending bằng `NEEDS_REVIEW`; revision cũ lưu audit, không post. Reference chưa được parser xác nhận và `missingFields` chứa raw text bị từ chối.
- **EXTERNAL_BLOCKED:** chưa có mẫu ngân hàng, parser/allowlist notification production hoặc kết nối email/tenant để bơm sự kiện thật. FIN-04 chỉ kiểm cổng metadata và fixture; source tasks tương lai phải kiểm quyền, parse, retention raw và matching bằng sự kiện thực trước nghiệm thu connected.
- Không có Google Sheets, không đối soát balance tự động (thuộc FIN-10).

## Bàn giao

- Task tiếp theo sẵn sàng: **FIN-05** (FIN-03 đã VERIFIED), hoặc task nguồn notification/email độc lập khi có fixture/mẫu tương ứng. Dừng tại FIN-04.
