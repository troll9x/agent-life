# DATA-01 Room và migration nền tảng

## Kết quả

Implementation: **IMPLEMENTED** ngày 2026-10-22.
External gate: **NOT_REQUIRED**. Gate nội bộ còn mở: DATA-02 phải nối database mã hóa trước khi app production dùng repository.
Version kiểm: source commit `1e0474c` trên `main`, dựa trên FND-03 (`565d336`).

## Prerequisite và hợp đồng

- FND-03 VERIFIED; domain trước task chưa có profile/repository interface, `data` chưa có Room source. Không có `AGENTS.md` trong repo.
- Theo `04_DATA_CONTRACTS.md`: UUID canonical, `workspaceId`, version, origin device, soft delete; sáu bảng nền của DATA-01; unique connection theo workspace/provider/account/tenant và checkpoint theo connection/resource. Audit chỉ giữ metadata, không lưu token/body/account thật.
- Domain interface `FoundationRepository` mới là bổ sung tối thiểu vì prerequisite chưa khai báo interface hồ sơ. Domain không import Android/Room; data triển khai adapter Room. Public contract này cần được giữ ổn định khi DATA-02 và UI dùng tới.
- Test dùng dữ liệu giả, database fixture plaintext trong test APK. App production vẫn màn hình placeholder và không mở database plaintext, theo ràng buộc kiến trúc. Vì vậy không coi việc quan sát UI production là đã kiểm.

## File đã thay đổi

- `android/data/src/main/kotlin/vn/lifeos/data/db/`: sáu Room entity, DAO Flow, database v2, migration 1→2, repository ghi audit cùng transaction và map lỗi DB sang `AppError`.
- `android/data/schemas/vn.lifeos.data.db.AgentLifeDatabase/{1,2}.json`: schema history do Room compiler xuất.
- `android/data/src/androidTest/kotlin/vn/lifeos/data/db/FoundationDatabaseTest.kt`: migration, persistence/connection checkpoint, và transaction failure fixtures.
- `android/domain/src/main/kotlin/vn/lifeos/domain/foundation/FoundationRepository.kt`: model, command, result/error và repository interface cần cho data layer.
- `android/build.gradle.kts`, `android/data/build.gradle.kts`, `android/domain/build.gradle.kts`, `android/gradle/libs.versions.toml`: đăng ký Room plugin/compiler, KSP 2.3.12 và coroutines Flow. Room 2.8.5 đã pin từ FND-01; KSP 2.3.12 được đối chiếu với [release chính thức](https://github.com/google/ksp/releases/tag/2.3.12) và [AGP built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin).
- `docs/engineering/{TESTING,TOOLCHAIN}.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, `01_PRODUCT_CHECKLIST.md`, evidence này: cập nhật lệnh/trạng thái.

## Đối chiếu yêu cầu

| Requirement ID | Trạng thái | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-DATA-01-01 | **NOT_RUN đầy đủ** | Instrumentation đóng/mở file DB giữ profile và 2 connection; repository `Flow` trả lại profile, connection, checkpoint sau mở lại. | Chưa có UI production quan sát repository: cần DATA-02 cấp database mã hóa rồi task UI nối ViewModel. Không tick checklist. |
| RQ-DATA-01-02 | **PASS** | Schema v1 và v2 đã export; `migrationKeepsProfileIdCountAndContent` tạo DB v1 bằng schema fixture rồi nâng 1→2, Room validate schema v2; assertion giữ ID, workspace ID, tên, zone, currency, budget day, version và đúng 1 row. | Chỉ có migration 1→2 tại thời điểm này; migration tương lai cần test riêng. |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `.\gradlew.bat :data:assembleDebug --console=plain` | Windows, JBR 25, SDK 37 | BUILD SUCCESSFUL; Room export schema v1. |
| `.\gradlew.bat :data:assembleDebug --console=plain` | Cùng môi trường, sau thêm domain interface | FAIL do thiếu dependency Flow trong `:domain`; đã thêm `api(libs.coroutines.core)`. Lần kế FAIL do suspend DAO gọi trong `also`; đã sửa gọi trực tiếp. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug :data:assembleDebugAndroidTest --console=plain` | Cùng môi trường | FAIL biên dịch fixture `arrayOf` hỗn hợp với Kotlin 2.4; đã đặt `arrayOf<Any>`. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug :data:connectedDebugAndroidTest --console=plain` | AVD `Medium_Phone_API_37.0`, Android 17/API 37 | BUILD SUCCESSFUL; ban đầu 2/2 instrumentation tests PASS. |
| `.\gradlew.bat :data:connectedDebugAndroidTest --console=plain` | Cùng AVD | BUILD SUCCESSFUL; thêm test atomic audit, 3/3 PASS. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :data:connectedDebugAndroidTest :app:assembleDebug :data:lintDebug --console=plain` | Cùng AVD, sau kiểm origin device/workspace | BUILD SUCCESSFUL; domain 7/7, data JVM `NO-SOURCE`, instrumentation 3/3, APK build, lint PASS. |
| `gh run watch 39749726959 --repo troll9x/agent-life --exit-status` | GitHub Actions, checkout mới từ commit `1e0474c` | [Run 39749726959](https://github.com/troll9x/agent-life/actions/runs/39749726959) PASS: unit tests, lint, debug APK. CI không chạy instrumentation. |
| `git diff --check` | Repo root | Không có lỗi whitespace; Git chỉ báo chuyển LF/CRLF ở working copy. |

Report instrumentation: `android/data/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17.xml`: `tests="3" failures="0" errors="0" skipped="0"`. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Kịch bản và dữ liệu

- Fixture UUID cố định giả, `Fixture User`, `account-a`/`account-b`, provider `CALENDAR`, hai cursor giả. Không client ID, tenant, token, số tài khoản hay dữ liệu cá nhân thật.
- Expected/actual: nâng v1→v2 không mất profile; hai connection khác external account giữ hai checkpoint khác nhau sau reopen; duplicate provider/account và stale profile version trả `Conflict`, audit vẫn đúng 2 row và không ghi dở. Cả ba test PASS trên AVD API 37.
- `Room.databaseBuilder` trong instrumentation chỉ là fixture. Production không dùng factory plaintext, không có `fallbackToDestructiveMigration`.

## Gate và phần chưa hoàn tất

- RQ-DATA-01-01: **NOT_RUN** ở UI production. DATA-02 phải chứng minh Room + SQLCipher tương thích và đăng ký encrypted database provider; sau đó UI task mới nối repository Flow và kiểm màn hình thật.
- Chưa kiểm migration trên thiết bị vật lý; AVD API 37 đã chạy. Không cần account/service bên ngoài cho DATA-01.
- KSP/Room source được kiểm theo [Room Gradle plugin và schema export](https://developer.android.com/jetpack/androidx/releases/room); migration test theo [Room testing API](https://developer.android.com/reference/androidx/room/testing/MigrationTestHelper).

## Bàn giao

- Task tiếp theo sẵn sàng: DATA-02 (mã hóa storage và proof of compatibility). Mang theo gate UI production của RQ-DATA-01-01; không đánh PASS yêu cầu đó chỉ từ instrumentation fixture.
