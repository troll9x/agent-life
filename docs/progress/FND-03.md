# FND-03 Dựng kiểm thử và CI tối thiểu

## Kết quả

Implementation: VERIFIED ngày 2026-10-22.
External gate: NOT_REQUIRED.
Commit/version kiểm: `40edbf6` trên `main` (workflow và lệnh tái lập); evidence này được ghi sau khi CI hoàn tất.

## Prerequisite và hợp đồng

- FND-02 đã VERIFIED: Money, UUID, LocalDateRange/TimeWindow và 7 unit test miền. Không đổi public domain contract hoặc Room schema.
- Test fixtures giữ `Clock`, `IdGenerator`, connectivity và coroutine dispatcher có thể điều khiển. `FakeConnectivity` chưa nối vào production boundary vì connector chưa có.
- CI không dùng account, client ID, tenant approval hoặc secret. Room migration test cần database từ DATA-02 nên chưa có kết quả.

## File đã thay đổi

- `.github/workflows/android.yml`: checkout, JDK 25, SDK 37, unit tests, lint và assembleDebug trên GitHub Actions; action tham chiếu SHA cố định, không dùng secrets.
- `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts`, `android/data/build.gradle.kts`, `android/domain/build.gradle.kts`: khóa phiên bản test dependencies và đăng ký unit/instrumentation đúng module. Espresso 3.7.0 được thêm rõ để chạy trên API 37.
- `android/domain/src/test/kotlin/vn/lifeos/domain/common/TestFixtures.kt`: FakeClock, FakeIdGenerator, FakeConnectivity, dispatcher và fixture factory; các test thời gian/UUID hiện dùng fake tương ứng.
- `android/app/src/androidTest/kotlin/vn/lifeos/app/MainActivitySmokeTest.kt`: Compose launcher smoke test trên thiết bị.
- `android/gradlew`: đặt executable bit trong Git để workflow Linux chạy wrapper.
- `docs/engineering/TESTING.md`: lệnh Windows, thiết bị và CI; ghi ranh giới kết quả Room.
- `docs/progress/FND-03.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, `01_PRODUCT_CHECKLIST.md`: evidence và trạng thái.

## Đối chiếu yêu cầu

| Requirement ID | Trạng thái | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FND-03-01 | PASS | Snapshot commit `40edbf6` có workflow và TESTING.md. [CI run thành công](https://github.com/troll9x/agent-life/actions/runs/39746110565) từ checkout mới chạy unit/lint/assembleDebug không cần secret. Lệnh tương ứng local cũng BUILD SUCCESSFUL. | `:app:testDebugUnitTest` và `:data:testDebugUnitTest` hiện `NO-SOURCE`; 7 test thực nằm trong `:domain:test`. |
| RQ-FND-03-02 | PASS | App Compose test chạy thực trên AVD API 37: 1 test, 0 fail. Data/Room migration instrumentation được ghi `NOT_RUN` bên dưới; không lấy việc tạo test APK làm PASS. | Room schema/DAO chưa tồn tại đến DATA-02. |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :data:assembleDebugAndroidTest --console=plain` | Windows, JBR 25, SDK 37 | BUILD SUCCESSFUL. Domain 7/7; app/data unit `NO-SOURCE`; app/data instrumentation APK compile, chưa đồng nghĩa test data đã chạy. |
| `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | AVD `Medium_Phone_API_37.0`, Android 17/API 37 | Lần đầu FAIL: Compose transitive Espresso cũ gọi `InputManager.getInstance`. Sau khi khóa Espresso 3.7.0, BUILD SUCCESSFUL; XML báo `MainActivitySmokeTest` 1 test, 0 failures/errors. |
| `.\gradlew.bat :data:lintDebug :app:lintDebug --console=plain` | Windows | BUILD SUCCESSFUL; data 0 issue, app 0 error/5 warnings (target API, version mới hơn có sẵn, app icon còn thiếu). |
| `.\gradlew.bat :domain:test --tests 'vn.lifeos.domain.common.MoneyTest' --console=plain` | Windows, assertion tạm 999999 thay 1000000 | Exit 1, `MoneyTest` 3 test/1 failed. Test đã được khôi phục đúng. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :data:lintDebug :app:lintDebug :app:assembleDebug --no-daemon --console=plain` | Windows sau khôi phục | BUILD SUCCESSFUL, 7 domain tests PASS, app/data unit `NO-SOURCE`. |
| GitHub Actions run `39745935012` | Checkout mới, commit `4f0d627` | FAIL tại SDK setup vì action cũ mặc định cài gói `tools` không còn được phân phối; đã sửa thành `packages: platform-tools`. |
| [GitHub Actions run `39746110565`](https://github.com/troll9x/agent-life/actions/runs/39746110565) | Checkout mới, commit `40edbf6` | PASS: SDK 37 cài thành công, unit/lint/assembleDebug hoàn tất. |
| [GitHub Actions run `39746480731`](https://github.com/troll9x/agent-life/actions/runs/39746480731) | Nhánh kiểm tra tạm, assertion Money cố tình sai | FAIL đúng ở `MoneyTest.kt:15`: 7 tests, 1 failed; process exit 1. Nhánh tạm đã xóa, `main` giữ test đúng. |

## Kịch bản và dữ liệu

- Input giả: fixture Clock `2026-10-19T01:59:00Z`, UUID canonical và số tiền VND mẫu; không có token hoặc dữ liệu cá nhân.
- Expected: test sai khiến CI fail; test đúng cho CI PASS; Compose launcher hiển thị “Agent Life”. Actual đúng như trên.
- Device: `Medium_Phone_API_37.0` AVD, Android 17/API 37, `adb devices` báo `device`; không dùng account.
- Report: `android/domain/build/reports/tests/test/index.html`, `android/app/build/reports/androidTests/connected/debug/index.html`, `android/app/build/reports/lint-results-debug.html` (local, không commit build output).

## Gate và phần chưa hoàn tất

- `data` Room migration instrumentation: **NOT_RUN** vì chưa có Room database hoặc migration. Cấu hình runner và `room-testing` đã build, DATA-02 sẽ thêm test có schema thực.
- CI không chạy instrumentation vì runner không có emulator; lệnh thiết bị đã ghi trong TESTING.md. Compose smoke test đã chạy local trên AVD.
- App lint còn 5 warnings, 0 errors; các warnings liên quan mục tiêu SDK, phiên bản và launcher icon, không chặn FND-03.

## Bàn giao

- Task tiếp theo sẵn sàng theo execution plan: DATA-01. Không đánh PASS Room migration trước DATA-02.
