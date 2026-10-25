# DATA-04 Retention và xóa dữ liệu theo nguồn

## Kết quả

Implementation: **IMPLEMENTED** ngày 2026-10-25, kiểm fixture trên AVD Android 17/API 37. External gate: **EXTERNAL_BLOCKED** đối với revoke SDK Microsoft/Google và dữ liệu ledger/outbox chưa có task sở hữu. Không gọi API thật, không dùng token thật.

## Prerequisite và hợp đồng

- DATA-02 có `EncryptedDatabaseProvider` SQLCipher; DATA-03 có `SourceHttpClient` và `SourceRef`. Repo sạch trước task; không có `AGENTS.md`.
- Theo `04_DATA_CONTRACTS`: preference theo workspace/source, remote processing mặc định false; raw email/notification không lưu mặc định; audit chỉ có ID/hành động. Schema Room v3 giữ v1/v2 và migration 2→3.
- `ConnectionLifecycle` bắt buộc được connector cung cấp để cancel work, unregister và revoke credentials. Fixture ghi các lời gọi; chưa có MSAL/Google SDK trong repo nên không ghi đã revoke thật.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/privacy/PrivacyContract.kt`: preference, cache policy, capture record, access gate, lifecycle và kết quả privacy.
- `android/data/src/main/kotlin/vn/lifeos/data/privacy/`: Room repository/gate, xóa thư mục app-owned theo workspace/connection, worker và lịch WorkManager.
- `android/data/src/main/kotlin/vn/lifeos/data/db/{AgentLifeDatabase,FoundationEntities,FoundationMigrations,PrivacyDao}.kt`, `android/data/schemas/.../3.json`: Room v3, preference và source capture; migration 2→3.
- `android/data/src/main/kotlin/vn/lifeos/data/security/EncryptedDatabaseProvider.kt`: đăng ký migration 2→3 khi mở DB mã hóa.
- `android/data/src/main/kotlin/vn/lifeos/data/network/SourceHttpClient.kt`, `android/domain/src/main/kotlin/vn/lifeos/domain/integration/HttpIntegration.kt`: HTTP yêu cầu connection ID cho authenticated request và hỏi gate trước mỗi attempt; nguồn đã ngắt trả NOT_CONNECTED trước khi lấy token/gửi request.
- `android/app/src/main/kotlin/vn/lifeos/app/AgentLifeApplication.kt`, `android/data/build.gradle.kts`, `android/gradle/libs.versions.toml`: đăng ký dọn raw lúc khởi động và mỗi giờ bằng WorkManager 2.12.0, phiên bản ổn định theo [AndroidX Work release](https://developer.android.com/jetpack/androidx/releases/work).
- `android/data/src/{test,androidTest}/`: HTTP gate JVM, migration và privacy trên AVD. `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, `docs/engineering/{TESTING,TOOLCHAIN}.md` và evidence này: trạng thái và lệnh thật.

## Đối chiếu yêu cầu

| Requirement ID | Trạng thái | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-DATA-04-01 | **BLOCKED** | `disconnectOneAccountLeavesOtherActiveAndCleanupIsIdempotent`: A chuyển NOT_CONNECTED, checkpoint/cache/file A bị xóa, B còn hoạt động; `disconnectedGateBlocksBeforeTokenAndNetwork`: server nhận 0 request; `revocationFailureStillBlocksRequestsAndCanRetry`: revoke lỗi vẫn chặn nguồn và retry được. | Chưa có bảng ledger/giao dịch xác nhận để chứng minh không xóa nhầm; chưa có SDK token thật. Không tick checklist. |
| RQ-DATA-04-02 | **PASS** | `rawIsOptInEncryptedAndExpiredRawKeepsCandidateIdentity`: mặc định không lưu raw, opt-in mới lưu trong SQLCipher, `PRAGMA secure_delete=1`, raw quá hạn bị NULL, `sourceEventId`/state giữ lại, purge lần hai trả 0; tắt opt-in xóa raw ngay. `workspaceDeleteOnlyTouchesNamedWorkspaceAndRepeatsSafely`: xóa lặp an toàn, file workspace khác còn. | WorkManager là best effort theo lịch OS, không thể bảo đảm job chạy đúng phút khi thiết bị tắt/force-stop; raw quá hạn không có API đọc production và được purge khi worker có thể chạy. |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `.\gradlew.bat :domain:test :data:compileDebugKotlin :app:assembleDebug --console=plain` | Windows, Android Studio JBR 25, SDK 37 | BUILD SUCCESSFUL; schema v3 và WorkManager compile, APK tạo. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :data:assembleDebugAndroidTest :app:assembleDebug --console=plain` | Cùng môi trường | BUILD SUCCESSFUL; domain 7/7, data HTTP 9/9, test APK build. |
| `.\gradlew.bat :data:connectedDebugAndroidTest --console=plain` | AVD `Medium_Phone_API_37.0`, API 37 | Lần đầu 11/11 PASS. Sau thêm `sourceKind`, lần chạy kế FAIL 1 migration do AndroidTest assets còn schema v3 cũ trong cùng lần build; chạy lại sau khi KSP xuất schema mới: 11/11 PASS. |
| `.\gradlew.bat :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest --console=plain` | AVD API 37 | BUILD SUCCESSFUL; data 12/12, app 2/2, 0 failures/errors. |
| `.\gradlew.bat :data:connectedDebugAndroidTest --console=plain` | AVD API 37, sau thêm xóa file theo connection | BUILD SUCCESSFUL; data 12/12, 0 failures/errors. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug :data:lintDebug :app:lintDebug --console=plain` | Sau code cuối | BUILD SUCCESSFUL; domain 7/7, data 9/9, APK debug tạo; data lint 0 issue, app lint 0 error/7 warnings. |
| `git diff --check` | Repo root | Không có lỗi whitespace; Git chỉ nhắc LF/CRLF trên Windows. |

Report: `android/data/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17.xml` ghi `tests="12" failures="0" errors="0"`; app report ghi 2/2. Test chỉ dùng UUID/account/raw giả.

## Kịch bản và dữ liệu

- `PrivacyPreference` mặc định MINIMAL, `allowRemoteProcessing=false`, `rawRetentionDays=0` cho email và health. DIAGNOSTIC opt-in cho tối đa 7 ngày; nội dung raw nằm trong DB SQLCipher. [SQLCipher bật secure delete mặc định](https://www.zetetic.net/sqlcipher/performance/).
- Disconnect cập nhật DB trước khi gọi lifecycle ngoài DB. Nếu revoke/cancel thất bại, trả `ExternalCleanupPending`, HTTP gate vẫn đóng và có thể retry. `deleteCache=true` xóa cache/checkpoint và tệp theo connection; `false` vẫn xóa raw nhạy cảm nhưng giữ metadata chống trùng.
- Xóa workspace xóa toàn bộ bảng hiện có theo thứ tự FK, sau đó chỉ xóa đường dẫn `workspaces/<UUID>` trong files/cache/noBackup/externalFiles app-owned, không đi theo symlink. DB/key SQLCipher là tài nguyên app dùng chung nên không xóa nguyên DB/key khi xóa một workspace.
- Audit mới chỉ ghi action, entity type/ID và thời điểm, không raw body, token, email hay bank account.

## Gate và phần chưa hoàn tất

- Ledger `FinancialTransaction`/`LedgerEntry`, outbox và cache connector chưa tồn tại; workspace deletion hiện xử lý tất cả bảng v3, nhưng cần cập nhật chính sách xóa khi các task sở hữu thêm bảng. FK RESTRICT khiến xóa thất bại thay vì âm thầm xóa nhầm dữ liệu mới.
- MSAL/Google OAuth, worker/registration theo connection chưa tồn tại; `ConnectionLifecycle` phải được adapter của task connector thực thi và kiểm trên account/tenant thật. Không đánh RQ-DATA-04-01 PASS trước kiểm giao dịch đã xác nhận.
- WorkManager có thể bị trì hoãn bởi Doze, force-stop hoặc thiết bị tắt. Lịch dọn không phải cam kết chạy đúng thời điểm vật lý; app không cung cấp đường đọc raw hết hạn.

## Bàn giao

- Task tiếp theo theo kế hoạch: **UX-01** App shell Compose và navigation. RQ-DATA-04-01 tiếp tục là gate của các task finance/connector và nghiệm thu tích hợp.
