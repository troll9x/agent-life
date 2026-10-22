# HEALTH-01 Nhật ký và chỉ số sức khỏe thủ công

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Ghi cân nặng, giấc ngủ, vận động, tâm trạng và triệu chứng bằng tay.

## Nhiệm vụ cần có trước

[DATA-01](../../modules/01-data-security/DATA-01.md), [UX-02](../../modules/02-app-shell/UX-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/health/`
- `android/data/src/main/kotlin/vn/lifeos/data/health/`
- `android/app/src/main/kotlin/vn/lifeos/app/health/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Schema measurement có metric/value/unit/start/end/source và journal có date/mood/symptoms/note.
2. Form có đơn vị rõ ràng, validation dữ liệu thiếu/sai thời gian; không từ chối số khác thường như chẩn đoán.
3. Nhật ký xem/sửa/xóa theo ngày, manual và imported source có nhãn phân biệt.
4. Không tự suy ra bệnh từ symptom hoặc chỉ số; hiển thị chỉ để theo dõi cá nhân.
5. Mọi thao tác qua repository, timestamp theo Clock và đọc offline.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-HEALTH-01-01** Nhập, sửa, xem lịch sử sức khỏe chạy không mạng và giữ đúng đơn vị.
- [ ] **RQ-HEALTH-01-02** Không có dữ liệu hiển thị thiếu dữ liệu, không dùng số demo làm thống kê.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Ngủ từ 23:00 hôm trước tới 06:00 hôm sau có duration 7h.
- [ ] Weight kg và lb được chuẩn hóa khi so sánh nhưng giữ source unit.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/HEALTH-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng HEALTH-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
