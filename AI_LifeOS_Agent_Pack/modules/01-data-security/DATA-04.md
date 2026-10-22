# DATA-04 Retention và xóa dữ liệu theo nguồn

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Có chính sách lưu dữ liệu tối thiểu và ngắt kết nối thực sự.

## Nhiệm vụ cần có trước

[DATA-02](../../modules/01-data-security/DATA-02.md), [DATA-03](../../modules/01-data-security/DATA-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/privacy/`
- `android/domain/src/main/kotlin/vn/lifeos/domain/privacy/`
- `android/data/src/androidTest/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Lưu PrivacyPreference theo workspace và source, có allowRemoteProcessing=false mặc định cho email/sức khỏe.
2. Raw nội dung email/notification chỉ xử lý tạm; nếu cần chẩn đoán opt-in thì mã hóa và tự xóa tối đa sau 7 ngày; mặc định không lưu toàn văn.
3. Ngắt nguồn hủy worker/đăng ký gắn nguồn, xóa token của app qua SDK và chọn xóa cache nguồn; không xóa sổ đã được người dùng xác nhận.
4. Xóa workspace phải xóa cache, DB rows, outbox và tệp thuộc workspace; kiểm tra đúng đường dẫn app-owned.
5. Audit chỉ chứa ID/hành động, không bản sao dữ liệu nhạy cảm; kiểm thử cleanup idempotent.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-DATA-04-01** Ngắt kết nối ngăn request tiếp theo của nguồn và không xóa giao dịch đã xác nhận ngoài ý muốn.
- [ ] **RQ-DATA-04-02** Cleanup lặp nhiều lần an toàn; dữ liệu raw hết hạn không còn trên ổ.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Disconnect Microsoft A không ảnh hưởng Microsoft B.
- [ ] Candidate còn raw tạm quá hạn bị xóa raw nhưng giữ trạng thái/ID cần chống trùng.

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
2. Evidence `docs/progress/DATA-04.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng DATA-04 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
