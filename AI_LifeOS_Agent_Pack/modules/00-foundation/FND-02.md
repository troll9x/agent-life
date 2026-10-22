# FND-02 Chuẩn hóa kiểu dữ liệu và quy tắc miền

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Cài các kiểu dùng chung để các module sau không tự định nghĩa Money, ID và thời gian khác nhau.

## Nhiệm vụ cần có trước

[FND-01](../../modules/00-foundation/FND-01.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/common/`
- `android/domain/src/test/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Đọc 03_DOMAIN_RULES.md và 04_DATA_CONTRACTS.md; tạo UUID wrapper, Money(Long, currency), SourceRef, TimeWindow và AppError.
2. Dùng java.time.Instant cho thời điểm, LocalDate/ZoneId cho kỳ địa phương; tiêm Clock và IdGenerator để kiểm thử.
3. Kiểm tra overflow bằng phép toán exact; parse tiền từ chuỗi thập phân theo currency exponent, không đi qua Double.
4. Định nghĩa SourceState: NOT_CONNECTED, OK, PARTIAL, STALE, AUTH_REQUIRED, PERMISSION_DENIED, UNSUPPORTED, ERROR; có timestamp và sanitized error.
5. Viết kiểm thử các quy tắc số tiền và ranh giới múi giờ; không thêm network/UI.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FND-02-01** Money VND 1.000.000 được lưu là 1000000 Long, không có sai số dấu phẩy động.
- [ ] **RQ-FND-02-02** Khoảng ngày địa phương chuyển thành [startInclusive,endExclusive) UTC đúng, kể cả DST của múi giờ khác Việt Nam.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Parse VND 12.500 -> 12500 theo locale nhập; từ chối phân số VND không hợp lệ.
- [ ] Clock cố định 2026-10-19T01:59:00Z hiển thị 08:59 tại Asia/Ho_Chi_Minh.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :domain:test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FND-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FND-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
