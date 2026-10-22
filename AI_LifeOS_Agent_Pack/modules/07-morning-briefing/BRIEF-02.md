# BRIEF-02 Engine tổng hợp snapshot bản tin

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tạo AI Morning Briefing bằng dữ liệu thực đã được thu thập và lưu snapshot theo ngày.

## Nhiệm vụ cần có trước

[BRIEF-01](../../modules/07-morning-briefing/BRIEF-01.md), [MS-03](../../modules/06-microsoft/MS-03.md), [MS-04](../../modules/06-microsoft/MS-04.md), [PLAN-02](../../modules/03-planner/PLAN-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [05_BRIEFING_CONTRACT.md](../../05_BRIEFING_CONTRACT.md): nội dung, giới hạn và trạng thái bản tin.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/briefing/`
- `android/data/src/main/kotlin/vn/lifeos/data/briefing/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Định nghĩa BriefingRun và BriefingSection theo 8 phần trong 05_BRIEFING_CONTRACT.md; include source statuses và counts.
2. Lấy P1/P2, thống kê P1..P4, tối đa 5 việc, lịch/deadline; tối đa 10 tin, 3 công cụ và 3 đề xuất có bằng chứng.
3. Không có đủ tin thì ít hơn giới hạn; số đếm là số thư đã kiểm, không số thư đoán từ estimate.
4. Ngày-local+workspace là một bản tin với revision; chạy lại update snapshot, giữ nguồn và không tạo notification trùng.
5. Một nguồn lỗi vẫn tạo PARTIAL từ nguồn khác; checkpoint của nguồn chỉ cập nhật khi nguồn ấy thành công.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-BRIEF-02-01** Snapshot đủ 8 phần hoặc ghi rõ phần thiếu; tổng quan và số đếm khớp dữ liệu nguồn.
- [ ] **RQ-BRIEF-02-02** Bản tin không tự gửi email, đổi trạng thái thư hoặc tạo task/lịch.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Microsoft lỗi nhưng RSS tốt -> bản tin PARTIAL có tin và trạng thái mail lỗi.
- [ ] Bản tin có 0 calendar records và source permission denied không ghi lịch trống.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug
# Khi thay Room/migration và đã có emulator/device:
.\gradlew.bat :data:connectedDebugAndroidTest
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/BRIEF-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng BRIEF-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
