# GOOGLE-03 Google Calendar chỉ đọc tùy chọn

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Thêm provider lịch thứ hai vào interface đã có, không viết một UI lịch khác.

## Nhiệm vụ cần có trước

[GOOGLE-01](../../modules/09-google/GOOGLE-01.md), [MS-04](../../modules/06-microsoft/MS-04.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/google/calendar/`
- `android/app/src/main/kotlin/vn/lifeos/app/planner/sources/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Đọc calendar metadata và cho chọn ID cụ thể, chỉ đọc các lịch được chọn.
2. Map event timed/all-day/recurrence/exception/cancelled về hợp đồng CalendarConnector chung.
3. Xử lý pagination, sync token hết hạn và resync hữu hạn theo tài liệu chính thức.
4. Bảo toàn source identity; cùng invite có iCalUID có thể group display nhưng không xóa bản ghi nguồn.
5. Mỗi calendar có last success và quyền riêng; dữ liệu remote vẫn chỉ đọc trong release CONNECTED.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-GOOGLE-03-01** Lịch Google hiển thị cùng Microsoft/local, giữ nguồn và không làm sai all-day.
- [ ] **RQ-GOOGLE-03-02** Thu hồi quyền Google không làm mất lịch cục bộ hoặc kết nối Microsoft.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Sự kiện bị hủy biến khỏi lịch hiển thị nhưng tombstone sync được giữ đúng.
- [ ] Cùng title khác source không tự gộp nếu không có identity chung.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/GOOGLE-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng GOOGLE-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
