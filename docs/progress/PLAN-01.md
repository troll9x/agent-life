# PLAN-01 Schema task, dự án và lịch

## Kết quả

Implementation: **VERIFIED**. External gate: **NOT_REQUIRED**. Bản kiểm: 26/10/2026.

Đã thêm hợp đồng domain và lưu trữ Room ngoại tuyến cho Project, Task, TaskChecklistItem, CalendarEvent, RecurrenceException và Reminder. Bản DB tăng từ v3 lên v4 bằng migration giữ nguyên dữ liệu cũ. Không thay UI; màn hình CRUD thuộc PLAN-02.

## Prerequisite và hợp đồng

- DATA-01 cung cấp `AgentLifeDatabase`, workspace/device và migration; FND-02 cung cấp `Uuid`, `Outcome` và các kiểu thời gian. Repo không có `AGENTS.md`; đã theo `06_AGENT_RULES.md`.
- Mutable entity lưu UUID, workspace, created/updated time, version, origin device, deletedAt. Task/project/checklist dùng khóa ngoại cùng workspace; event nguồn liên kết connection cùng workspace. Dữ liệu cần sync dùng soft delete.
- `ALL_DAY` lưu `LocalDate` dạng ISO và `endDateExclusive`; `TIMED` lưu epoch millis của `Instant` cùng `eventZoneId`. Không đổi all-day thành nửa đêm UTC. Truy vấn ngày dùng khoảng nửa mở.
- Local recurrence hỗ trợ DAILY/WEEKLY/MONTHLY trong cửa sổ tối đa 366 ngày và khóa occurrence dựa trên ngày gốc. Recurrence phức tạp từ provider chỉ lưu/cache, không tự triển khai RRULE. Không có Google Sheet hay schema ledger minh họa.

## File đã thay đổi

- `android/domain/src/main/kotlin/vn/lifeos/domain/planner/PlannerContract.kt`: model, command, repository interface, overdue/occurrence rule.
- `android/data/src/main/kotlin/vn/lifeos/data/planner/`: sáu Room entity, DAO, repository và migration v3→v4.
- `android/data/src/main/kotlin/vn/lifeos/data/db/AgentLifeDatabase.kt`, `android/data/src/main/kotlin/vn/lifeos/data/security/EncryptedDatabaseProvider.kt`: đăng ký DAO/entity và migration trong đường mở DB mã hóa.
- `android/data/schemas/vn.lifeos.data.db.AgentLifeDatabase/4.json`: schema history do Room xuất.
- `android/domain/src/test/kotlin/vn/lifeos/domain/planner/`, `android/data/src/androidTest/kotlin/vn/lifeos/data/planner/`: unit/instrumentation fixture.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-PLAN-01-01 | PASS | `PlannerMigrationTest` nâng DB v3 chứa profile lên v4, Room validate schema và profile vẫn nguyên; `RoomPlannerRepositoryTest` lưu project/task/checklist/event/reminder/exception, đóng/mở DB rồi đọc lại; truy vấn task chưa deadline và task DONE; checklist soft delete theo task. | Kiểm bằng DB fixture trên AVD; UI CRUD thuộc PLAN-02. |
| RQ-PLAN-01-02 | PASS | `RoomPlannerRepositoryTest` lưu all-day 19/10/2026 với end exclusive 20/10; query cùng ngày ở Asia/Ho_Chi_Minh, America/Los_Angeles, Pacific/Auckland đều trả ngày 19, ngày 18 không có. | Chưa có màn hình lịch để kiểm hiển thị, thuộc PLAN-02. |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :domain:test :data:compileDebugKotlin :app:assembleDebug --console=plain --quiet` | PowerShell tại `android/` | Lần đầu exit 1 vì Kotlin không smart cast thuộc tính từ module khác; đã sửa. Lần hai exit 0, Room xuất schema v4. |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :domain:test :data:testDebugUnitTest :data:connectedDebugAndroidTest :data:lintDebug :app:lintDebug :app:assembleDebug --console=plain --quiet` | Medium Phone AVD API 37 | Exit 0; domain 10/10, data unit 9/9, data instrumentation 16/16, lint và APK PASS. |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :domain:test :data:connectedDebugAndroidTest :app:assembleDebug --console=plain --quiet` | Cùng AVD, sau sửa version của Project | Exit 0; domain 10/10, data instrumentation 16/16, APK PASS. |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :data:connectedDebugAndroidTest :app:assembleDebug --console=plain --quiet` | Cùng AVD, sau bổ sung hủy reminder khi soft delete task | Exit 0; data instrumentation 16/16, APK PASS. |
| `gh run watch 40283021131 --exit-status` | GitHub Actions, commit `683ff8a` | Exit 0; Android job hoàn tất sau 3m0s, unit test/lint/debug APK PASS. |

Report: `android/domain/build/test-results/test/`, `android/data/build/test-results/testDebugUnitTest/`, `android/data/build/outputs/androidTest-results/connected/debug/`. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Kịch bản và dữ liệu

- Fixture UUID cố định và tên giả `Fixture User`/`Fixture Project`; không có tài khoản, token hoặc dữ liệu cá nhân. AVD `emulator-5554`, API 37. Test Room dùng file DB riêng, không sửa DB người dùng của app.
- Task deadline null: expected không quá hạn; actual hàm domain trả false và query `tasksDueBefore` bỏ qua. Task có hạn trước mốc kiểm xuất hiện, sau trạng thái DONE không còn trong query.
- All-day 19/10 đến trước 20/10: expected không dịch sang 18/10 khi đổi zone; actual đúng ở ba zone. Timed event băng qua nửa đêm địa phương xuất hiện ở cả hai ngày liên quan.
- Archive project giữ task; soft delete task ẩn task/checklist khỏi query, hủy reminder đang chờ nhưng không xóa vật lý. Duplicate reminder cùng target/occurrence/slot bị từ chối. Recurrence tuần cho khóa ổn định giữa hai cửa sổ giao nhau; cửa sổ trên 366 ngày bị từ chối.

## Gate và phần chưa kiểm

- Không cần client ID, tenant, mẫu ngân hàng hay kết nối mạng cho task này.
- **NOT_RUN:** UI tạo/sửa/lịch trên app vì đó là PLAN-02; RRULE phức tạp từ provider chưa có connector nên chỉ cache theo hợp đồng, không giả sinh occurrence.
- Chưa kiểm nâng cấp file DB mã hóa của người dùng thật; migration v3→v4 đã được Room validate trên AVD với profile fixture, và production opener đã đăng ký migration. Không dùng destructive fallback.

## Bàn giao

- Task tiếp theo sẵn sàng: **PLAN-02**, prerequisite PLAN-01 và UX-02 đã có code/hợp đồng.
