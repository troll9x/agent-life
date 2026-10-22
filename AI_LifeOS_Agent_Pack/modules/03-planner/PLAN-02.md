# PLAN-02 Màn hình CRUD công việc và lịch

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tạo/sửa/hoàn tất task, checklist và lịch trên điện thoại.

## Nhiệm vụ cần có trước

[PLAN-01](../../modules/03-planner/PLAN-01.md), [UX-02](../../modules/02-app-shell/UX-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/app/src/main/kotlin/vn/lifeos/app/planner/`
- `android/domain/src/main/kotlin/vn/lifeos/domain/planner/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Làm danh sách việc theo ngày/dự án, lọc OPEN/DONE và editor có validation.
2. Làm lịch ngày/tuần tối giản, tạo event có giờ hoặc all-day và múi giờ rõ.
3. Hỗ trợ lặp DAILY/WEEKLY/MONTHLY với interval, ngày kết thúc hoặc count; các rule không hỗ trợ phải báo rõ.
4. Sửa một lần lặp tạo exception, sửa chuỗi có xác nhận phạm vi; không sửa event remote chỉ đọc.
5. Phát hiện lịch chồng nhau và gợi ý; người dùng chọn, không tự dời lịch.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-PLAN-02-01** Có thể tạo/sửa/hoàn tất/checklist/lịch ngoại tuyến; thay đổi hiện trên dashboard.
- [ ] **RQ-PLAN-02-02** Lịch chồng nhau có cảnh báo và lịch lặp không phát sinh occurrence trùng.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Họp 09:00–10:00 và 09:30–10:30 cùng ngày hiển thị xung đột.
- [ ] Lặp ngày 31 có chính sách bỏ tháng thiếu ngày, mô tả rõ trên UI.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/PLAN-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng PLAN-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
