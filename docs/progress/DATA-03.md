# DATA-03 Nền HTTP và trạng thái kết nối

## Kết quả

Implementation: **VERIFIED** bằng unit fixture và build Android ngày 2026-10-25. External gate: **EXTERNAL_BLOCKED** cho live Microsoft/Google; chưa có connector hay account thực trong task này. Không gọi API thật.

## Prerequisite và hợp đồng

- DATA-01 đã có Room repository/schema; DATA-02 đã có DB mã hóa. Repo sạch trước task, không có `AGENTS.md`.
- Dùng `SourceRef`, `SourceStatus`, `SourceState`, `AppError` trong domain 2.1. `ConnectionResult.Failure` giữ lý do riêng cho offline, timeout, 429 và dữ liệu sai; không biến lỗi thành danh sách rỗng thành công.
- Token do `AccessTokenProvider` của connector sở hữu. `SourceHttpClient` chỉ nhận token qua interface, không lưu hoặc log token. Endpoint chấp nhận HTTPS và loopback HTTP cho fixture; request và redirect phải cùng scheme/host/port.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/integration/HttpIntegration.kt`: token boundary, kết quả và mã lỗi/trạng thái nguồn.
- `android/data/src/main/kotlin/vn/lifeos/data/network/SourceHttpClient.kt`: OkHttp timeout/cancellation, JSON typed serialization, giới hạn 1 MiB, same-origin auth/redirect, refresh 401, 403/429, GET retry tối đa 3, POST chỉ retry với hợp đồng idempotency được connector khai báo, phân trang có giới hạn.
- `android/data/src/test/kotlin/vn/lifeos/data/network/SourceHttpClientTest.kt`: 8 MockWebServer fixtures, gồm hủy coroutine khi HTTP đang chờ.
- `android/{build.gradle.kts,data/build.gradle.kts,gradle/libs.versions.toml}`: pin OkHttp/MockWebServer 5.5.1, kotlinx.serialization JSON 1.11.0 và Kotlin serialization plugin 2.4.21. [OkHttp changelog](https://github.com/square/okhttp/blob/master/CHANGELOG.md), [Kotlin serialization releases](https://github.com/Kotlin/kotlinx.serialization/releases). Lint có cảnh báo bản 1.12.0 mới hơn; chọn 1.11.0 từ danh sách release ổn định đã kiểm.
- `android/app/src/main/AndroidManifest.xml`: quyền INTERNET để connector tương lai dùng adapter; không thêm cleartext traffic production.
- `docs/engineering/{TOOLCHAIN,TESTING}.md`, `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md` và evidence này: phiên bản, lệnh và trạng thái.

## Đối chiếu yêu cầu

| Requirement ID | Trạng thái | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-DATA-03-01 | **PASS** | `authAndPermissionAreFailures`, `retryAfter60DoesNotLoop`, `offlineAndTimeoutAreNotEmptySuccess`: 401 sau refresh -> AUTH_REQUIRED, 403 -> PERMISSION_DENIED, 429 có `retryAt` +60 giây chỉ 1 request, offline/timeout -> Failure. 8/8 JVM tests PASS. | Chưa có provider thật/UI source list; task connector và UI sẽ tiêu thụ `ConnectionResult`. |
| RQ-DATA-03-02 | **PASS** | `telemetryContainsOnlyStructuralCodes`: sink chỉ nhận `SourceKind` enum và `NetworkEvent`, không nhận URL, headers/body hoặc `sourceId`. Fixture token/email body/bank account giả không xuất hiện. | Chưa kiểm log của SDK connector sau này. |

Tình huống bắt buộc: `unknownRedirectNeverReceivesAuthorization` xác nhận origin trả 302 đến server khác, server đích nhận 0 request; `retryAfter60DoesNotLoop` xác nhận 429 chỉ 1 request. `paginationAndBadResponses` kiểm hai trang, HTML và phản hồi quá lớn. `postRetryRequiresExplicitContract` kiểm POST thường chỉ 1 request khi 503, POST có idempotency key thử lại.

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `.\gradlew.bat :domain:test :data:compileDebugUnitTestKotlin --console=plain` | Windows, Android Studio JBR 25, SDK 37 | FAIL đầu tiên: `kotlin.test.Test` chưa resolve trong Android test; thêm `kotlin-test-junit`. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug --console=plain` | Cùng môi trường | FAIL đầu tiên: 7 fixture chưa `MockWebServer.start()`; sửa fixture. |
| `.\gradlew.bat :data:testDebugUnitTest :app:assembleDebug --console=plain` | Trước test cancellation | BUILD SUCCESSFUL; data 7/7 tests PASS, APK debug tạo thành công. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug :data:lintDebug :app:lintDebug --console=plain` | Trước sửa cancellation | BUILD SUCCESSFUL; domain 7/7, data 7/7, data lint 0 issue, app lint 0 error/7 warnings. APK `android/app/build/outputs/apk/debug/app-debug.apk`. |
| `.\gradlew.bat :data:testDebugUnitTest --console=plain` | Thêm test hủy request | FAIL 1/8: call đồng bộ không dừng nhanh; đổi sang OkHttp enqueue + `invokeOnCancellation { call.cancel() }`. Sau sửa BUILD SUCCESSFUL, 8/8 PASS. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug :data:lintDebug :app:lintDebug --console=plain` | Sau code cuối | BUILD SUCCESSFUL; domain 7/7, data 8/8, data lint 0 issue, app lint 0 error/7 warnings, APK debug đã tạo. |
| `git diff --check` | Repo root | Không có lỗi whitespace; Git nhắc chuyển LF/CRLF trên Windows. |

Report: `android/data/build/test-results/testDebugUnitTest/TEST-vn.lifeos.data.network.SourceHttpClientTest.xml` ghi `tests="8" failures="0" errors="0"`. Test không chạy trên emulator vì task không đổi Room/UI/SDK Android runtime.

## Kịch bản và dữ liệu

- Chỉ dùng `fixture-secret-token`, `fixture-email-body`, `fixture-bank-account-123` giả trong unit test. Không có token, email, tài khoản hoặc client ID thật.
- GET có tối đa 3 lần thử khi lỗi IO/5xx; 429 trả thời điểm cho scheduler tương lai, không ngủ 60 giây trong test hoặc retry ngay. POST chỉ retry khi connector khai báo provider hỗ trợ idempotency key cho endpoint; adapter không tự suy hợp đồng của provider.
- Redirect same-origin có tối đa 3 hop; POST redirect bị chặn. URL phân trang lặp hoặc khác origin trả Failure. JSON phải có content type và deserialize được; HTML/oversize trả Failure.

## Gate và phần chưa hoàn tất

- Microsoft: cần Entra public client ID, redirect signature, loại mailbox, consent user/admin theo `09_EXTERNAL_SETUP.md` ở MS-01. Google: OAuth app/consent tương ứng ở GOOGLE-01. Không thể suy live API PASS từ fixture.
- Chưa kiểm timeout/cancellation trên mạng thiết bị thật hoặc telemetry của connector tương lai. App hiện chưa dùng adapter để gọi endpoint; task này chỉ là nền dùng chung.

## Bàn giao

- Task tiếp theo theo thứ tự kế hoạch: **DATA-04** (retention và xóa dữ liệu theo nguồn), prerequisite DATA-02 và DATA-03 đã có code/test nội bộ. Live connector gate vẫn truyền đến task nghiệm thu tích hợp.
