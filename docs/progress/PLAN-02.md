# PLAN-02 Màn hình CRUD công việc và lịch

## Kết quả

Implementation: **VERIFIED**. External gate: **NOT_REQUIRED**. Bản kiểm: 27/10/2026.

Khu Công việc có danh sách theo ngày/dự án, lọc đang làm/hoàn tất, form tạo/sửa việc, checklist và trạng thái hoàn tất. Khu Lịch có xem ngày/tuần, form timed/all-day với múi giờ, quy tắc lặp DAILY/WEEKLY/MONTHLY, chọn sửa một lần hoặc toàn chuỗi. Dashboard Hôm nay đọc cùng Room Flow và cập nhật theo việc/lịch. Cảnh báo trùng lịch chỉ gợi ý; người dùng quyết định vẫn lưu hay đổi giờ.

## Prerequisite và hợp đồng

- PLAN-01 cung cấp schema v4, `PlannerRepository`, các entity Room; UX-02 cung cấp workspace/profile và app shell. Repo không có `AGENTS.md`; đã áp dụng `06_AGENT_RULES.md`.
- UI gọi hành động qua `ShellViewModel`/`PlannerFeature`; dữ liệu đi từ Room Flow. App chỉ mở database bằng `EncryptedDatabaseProvider`; không thêm DB plaintext, migration hay Google Sheet.
- Rule lặp cục bộ lưu dạng chuỗi có cấu trúc trong trường `recurrence` hiện có; editor yêu cầu interval 1–30 và đúng một giới hạn đến ngày (trong 5 năm) hoặc count 1–366. Tháng không có ngày 31 bị bỏ, không dời ngày. Occurrence có key từ series ID + ngày gốc, truy vấn trong cửa sổ tối đa 366 ngày. Rule provider phức tạp được báo chưa hỗ trợ thay vì tự diễn giải RRULE.
- Sửa một lần lặp lưu exception hủy occurrence gốc và tạo event thay thế trong cùng Room transaction. Sửa toàn chuỗi yêu cầu chọn phạm vi trước, dùng version check; remote event readonly không có nút sửa và repository từ chối sửa.

## File đã thay đổi

- `android/app/src/main/kotlin/vn/lifeos/app/planner/`: `PlannerFeature`, dashboard, danh sách/form việc và lịch, cảnh báo trùng và chọn phạm vi sửa.
- `android/domain/src/main/kotlin/vn/lifeos/domain/planner/`: rule lặp giới hạn, materialize occurrence, so sánh khoảng thời gian và command sửa.
- `android/data/src/main/kotlin/vn/lifeos/data/planner/{PlannerDao,RoomPlannerRepository}.kt`: Flow quan sát và thao tác sửa với version/readonly check; không đổi schema.
- `android/app/src/main/kotlin/vn/lifeos/app/{AppContainer,ShellViewModel,AgentLifeScreen,MainActivity}.kt`: nối DI, trạng thái và hai route Hôm nay/Công việc.
- `android/{app,data,domain}/src/{test,androidTest}/`: unit, Room instrumentation, Compose fixture/navigation tests liên quan.
- `01_PRODUCT_CHECKLIST.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, evidence này.

## Đối chiếu yêu cầu

| Requirement ID | Kết quả | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-PLAN-02-01 | PASS | AVD APK thật: tạo `Plan02FixtureTask` không hạn ở Công việc, thấy việc đó và số việc mở 1 trên Hôm nay, bấm Hoàn tất rồi dashboard về 0. `PlannerUiTest` kiểm form tạo/hoàn tất và dashboard việc không hạn; `RoomPlannerRepositoryTest` sửa task, tạo lịch, checklist, đóng/mở DB và đọc lại. | Màn hình chưa có xóa task/event; task này yêu cầu tạo/sửa/hoàn tất. Nhắc hệ thống thuộc PLAN-03. |
| RQ-PLAN-02-02 | PASS | Domain test họp 09:00–10:00 với 09:30–10:30 phát hiện chồng, 10:00 tiếp giáp không chồng; Compose test hiển thị cảnh báo trên lịch. Test ngày 31 bỏ tháng 2, key trùng giữa hai cửa sổ được gộp; exception hủy đúng một lần. Room test sửa một lần tạo exception + event thay thế nguyên tử và retry trùng bị Conflict. | Chỉ hỗ trợ rule local DAILY/WEEKLY/MONTHLY; provider RRULE phức tạp chờ connector. |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat :domain:test :app:testDebugUnitTest :data:testDebugUnitTest :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest :data:lintDebug :app:lintDebug :app:assembleDebug --console=plain --quiet` | PowerShell tại `android/`, Medium Phone AVD API 37 | Exit 0; domain 14/14, app unit 3/3, data unit 9/9, app instrumentation 12/12, data instrumentation 18/18; lint và debug APK PASS. |
| `git diff --check` | Repo root | Exit 0, không có whitespace error; chỉ cảnh báo LF/CRLF của Git. |
| `adb install -r android/app/build/outputs/apk/debug/app-debug.apk` và `adb shell am start -n vn.lifeos.app/.MainActivity` | Cùng AVD, ứng dụng fixture | Cài và mở APK PASS. UI quan sát onboarding, Công việc và dashboard bằng `uiautomator dump`; thao tác tạo/hoàn tất fixture có kết quả như trên. |

Report: `android/domain/build/test-results/test/`, `android/app/build/test-results/testDebugUnitTest/`, `android/{app,data}/build/outputs/androidTest-results/connected/debug/`. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

CI GitHub Actions cho commit `86bdf7303cb78002547cd157fc6c27bc3cf98f9b`: [Android run 40437475289](https://github.com/troll9x/agent-life/actions/runs/40437475289) hoàn tất PASS (`gh run watch 40437475289 --exit-status`, exit 0); job `build` đã chạy unit tests, lint và debug APK.

## Kịch bản và dữ liệu

- Test chỉ dùng UUID/tên giả. Không có account, client ID, token, dữ liệu ngân hàng hoặc thông tin cá nhân trong code/evidence.
- App test trên AVD thấy Hôm nay/Công việc và form, định dạng ngày `yyyy-MM-dd` và thời gian `yyyy-MM-dd HH:mm`. Lịch all-day giữ `LocalDate` và `endDateExclusive`; timed event dùng `Instant` với zone sự kiện. UI có loading/empty/error, nút thử lại, và banner offline của shell.
- Test đã gặp lỗi biên dịch smart cast Kotlin và vài assertion/navigation timeout trong quá trình làm; đã sửa và chạy lại vòng cuối PASS. Không ghi PASS dựa trên các lần lỗi.
- **Lưu ý môi trường test:** `connectedDebugAndroidTest` trên AVD này gỡ package app sau khi chạy; dữ liệu app cục bộ trên AVD không còn. Đã cài lại debug APK, hiện mở ở onboarding mới. Không có bản sao lưu AVD trong repo để khôi phục hồ sơ cũ.

## Gate và phần chưa kiểm

- Không cần kết nối cloud/tenant/client ID cho PLAN-02. Chưa chạy thử trên điện thoại vật lý hoặc TalkBack thủ công.
- Quy tắc lặp local chỉ materialize trong phạm vi tối đa 5 năm, cửa sổ tối đa 366 ngày. Event remote readonly có fixture và được chặn sửa; chưa có connector thật nên chưa kiểm luồng provider trên account thật.
- Notification/reminder thực tế thuộc PLAN-03. Dashboard đầy đủ nhiều nguồn và tìm kiếm thuộc UX-03 sau các prerequisite khác.

## Bàn giao

- Task tiếp theo sẵn sàng: **PLAN-03** vì PLAN-02 và DATA-02 đã có code/hợp đồng. Không triển khai PLAN-03 trong lượt này.

## Bổ sung theo phản hồi ngày 27/10/2026

- Thay ô nhập chuỗi ngày/giờ ở bộ lọc việc, hạn việc, lịch ngày, sự kiện cả ngày/có giờ và ngày kết thúc lặp bằng DatePickerDialog/TimePickerDialog của Android. Trường tùy chọn có nút Bỏ chọn. Giá trị chọn vẫn đi qua validation và múi giờ của hồ sơ/sự kiện hiện có; không đổi contract hay schema.
- `PlannerUiTest.taskDuePickerPassesSelectedDateAndTimeToAction` trên emulator thử nghiệm riêng API 37 đã mở cả hộp chọn ngày và giờ, chọn mặc định 19/10/2026 09:00 Asia/Ho_Chi_Minh và xác nhận action nhận `2026-10-19T02:00:00Z`. Nhóm UI liên quan PLAN-02, UX-02 và shell: 12/12 PASS bằng `adb -s emulator-5556 shell am instrument -w -e class 'vn.lifeos.app.PlannerUiTest,vn.lifeos.app.OnboardingPermissionTest,vn.lifeos.app.ShellNavigationTest' vn.lifeos.app.test/androidx.test.runner.AndroidJUnitRunner`.
- `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest` exit 0; app unit 4/4. Test UI chỉ dùng AVD `AgentLife_Isolated_Test_API_37`, không gỡ app hoặc dữ liệu trên AVD thường dùng `emulator-5554`. RQ-PLAN-02-01/02 vẫn PASS; thay đổi này cải thiện thao tác nhập thời gian.
