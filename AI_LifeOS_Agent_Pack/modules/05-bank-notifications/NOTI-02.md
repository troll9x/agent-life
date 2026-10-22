# NOTI-02 Parser thông báo theo mẫu ngân hàng

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Parser tất định từ notification chuẩn hóa sang candidate và có fixture cho từng mẫu hỗ trợ.

## Nhiệm vụ cần có trước

[NOTI-01](../../modules/05-bank-notifications/NOTI-01.md), [FND-02](../../modules/00-foundation/FND-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/notificationparser/`
- `android/domain/src/test/`
- `fixtures/bank-notifications/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Định nghĩa BankNotificationParser interface và registry theo package đã xác minh; không tự đoán package ngân hàng thật.
2. Trích direction, amountMinor, currency, masked account, occurredAt và bankReference; unknown giữ null/needsReview.
3. Hỗ trợ dấu ngăn cách Việt/Anh theo từng template, không dùng regex chung để lấy số dư làm amount.
4. Parser trả parserVersion, missingFields và confidence có ý nghĩa; AI fallback không tự confirm.
5. Dùng mẫu giả và form để người dùng cung cấp mẫu đã che thông tin; ngân hàng chưa có mẫu hiện UnsupportedTemplate.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-NOTI-02-01** Template đã hỗ trợ trích đúng amount của giao dịch, không lấy nhầm số dư.
- [ ] **RQ-NOTI-02-02** Mẫu thiếu nội dung hoặc không rõ tiền vào/ra luôn chờ xác nhận.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] TK ****1234 +15.000.000 VND; SD 20.000.000 -> amount 15000000, không 20000000.
- [ ] Thông báo OTP không sinh candidate thu/chi.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :domain:test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/NOTI-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng NOTI-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
