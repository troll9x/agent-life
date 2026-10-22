# FND-02 Chuẩn hóa kiểu dữ liệu và quy tắc miền

## Kết quả

Implementation: VERIFIED bằng unit test và app build ngày 2026-10-22.
External gate: NOT_REQUIRED.
Commit hoặc phiên bản kiểm: workspace không có `.git`; không có commit.

## Prerequisite và hợp đồng

- FND-01 đã có ba module Gradle, wrapper và APK. User xác nhận Android Studio chạy app trên emulator, ghi ở [FND-01](FND-01.md).
- Theo `04_DATA_CONTRACTS.md`: ID là UUID string canonical; Money giữ `Long` minor units và mã tiền ISO uppercase. `03_DOMAIN_RULES.md` yêu cầu thời điểm `Instant`, ngày địa phương `LocalDate` cùng `ZoneId`, khoảng thời gian nửa mở và source state tách lỗi khỏi danh sách trống.
- Chưa có live gate dependency, account hay thiết bị cần cho unit test JVM.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/common/Uuid.kt`: UUID canonical và `IdGenerator` tiêm được.
- `android/domain/src/main/kotlin/vn/lifeos/domain/common/Money.kt`: mã tiền ISO, số minor `Long`, parse theo locale rõ ràng, toán học exact và kiểm overflow/khác currency.
- `android/domain/src/main/kotlin/vn/lifeos/domain/common/TimeWindow.kt`: khoảng `Instant` nửa mở, dải ngày địa phương chuyển theo zone và hàm thời gian nhận `Clock`.
- `android/domain/src/main/kotlin/vn/lifeos/domain/common/Source.kt`: `SourceRef`, `SourceState`, `SourceStatus` có timestamp và mã lỗi an toàn, không chứa nội dung lỗi provider.
- `android/domain/src/test/kotlin/vn/lifeos/domain/common/`: 7 unit test cho tiền, DST/Clock và UUID.
- `android/domain/build.gradle.kts`: thêm `kotlin("test")`; căn Java/Kotlin bytecode target 17. Đây là registration tối thiểu cho test trong phạm vi task.
- `docs/progress/FND-01.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, `01_PRODUCT_CHECKLIST.md`: cập nhật bằng chứng và trạng thái requirement.

## Đối chiếu yêu cầu

| Requirement ID | Trạng thái | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FND-02-01 | PASS | `MoneyTest`: `1.000.000` VND -> `1_000_000L`, `12.500` -> `12_500L`, VND `12.500,5` bị từ chối, USD 1.23 -> 123 minor, overflow và cộng khác currency bị từ chối. | Currency exponent dựa trên Java ISO `Currency`; chưa có UI nhập tiền. |
| RQ-FND-02-02 | PASS | `TimeWindowTest`: ngày New York 2026-03-08 dài 23 giờ, 2026-11-01 dài 25 giờ; endExclusive không thuộc window. Clock `2026-10-19T01:59:00Z` -> 08:59 Asia/Ho_Chi_Minh. | Unit test JVM; chưa có UI lịch. |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `git status --short` | Gốc workspace Windows | exit 128: không có `.git`; không thể đối chiếu diff. |
| `.\gradlew.bat :domain:test --console=plain` | `android/`, JBR 25 | Lần 1 fail do `compileJava` target 25 khác Kotlin 17; đã đặt Java target 17. Lần 2 chạy 7 test, một test sai vì `12,500` hợp lệ theo locale US; sửa fixture thành `12,50`. Lần cuối BUILD SUCCESSFUL; XML báo 7 tests, 0 failures, 0 errors. Report: `android/domain/build/reports/tests/test/index.html`. |
| `.\gradlew.bat :app:assembleDebug --console=plain` | `android/`, JBR 25, Android SDK API 37 | BUILD SUCCESSFUL; APK `android/app/build/outputs/apk/debug/app-debug.apk` (29,438,077 bytes). |
| `rg -n '^import (android\.|androidx\.|com\.microsoft|com\.google)' android/domain/src -g '*.kt'` | Gốc workspace | Không có import bị cấm trong domain. |

## Kịch bản và dữ liệu

- Input giả: số tiền mẫu 1.000.000/12.500 VND, USD 1.23, ngày DST 2026 tại America/New_York và Clock cố định 2026-10-19T01:59:00Z. Không có dữ liệu cá nhân.
- Expected/actual: các giá trị và từ chối lỗi đúng như bảng requirement; 7/7 test PASS.
- Device/model/API/account type: không cần thiết bị hoặc account cho JVM test. APK build bằng SDK API 37; chưa chạy UI mới vì FND-02 không sửa UI.
- Screenshot: không cần cho test domain; có HTML/XML report Gradle cục bộ.

## Gate và phần chưa hoàn tất

- Không có gate ngoài. FND-02 không tạo Room schema, UI hoặc API connector.
- Workspace không có `.git`; không thể ghi commit.

## Bàn giao

- Task tiếp theo sẵn sàng: FND-03 (kiểm thử và CI tối thiểu), prerequisite FND-02 đã có code và test.
