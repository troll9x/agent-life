# DATA-02 Mã hóa DB và kho bí mật

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Mã hóa dữ liệu người dùng và thiết kế khóa không làm hỏng tác vụ nền.

## Nhiệm vụ cần có trước

[DATA-01](../../modules/01-data-security/DATA-01.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/security/`
- `android/data/src/main/kotlin/vn/lifeos/data/db/`
- `android/app/src/main/res/xml/`
- `docs/engineering/ENCRYPTION.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Kiểm chứng cặp Room/SQLCipher Android chính thức tương thích; ghi tọa độ và phiên bản thật, API open-helper/driver dùng thực tế; không trộn ví dụ Room 2 và Room 3.
2. Sinh khóa DB ngẫu nhiên, bọc bằng Android Keystore; không đặt raw key trong source, DataStore thường hoặc log.
3. Tách app lock UI khỏi khả năng mở DB của background worker: khóa không yêu cầu xác thực sinh trắc học cho mỗi lần đọc nền; ghi tradeoff rõ.
4. Loại token, raw DB và wrapped key khỏi Android Auto Backup không khôi phục được; backup do người dùng xuất sẽ do BACKUP-01 quản lý.
5. Xử lý key unavailable bằng màn hình recovery có hướng dẫn; không tự reset DB.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-DATA-02-01** File DB người dùng không thể đọc bằng SQLite thường mà không có khóa.
- [ ] **RQ-DATA-02-02** Tác vụ nền đọc được DB sau khi thiết bị đã unlock ít nhất một lần; lỗi khóa không gây xóa dữ liệu.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Reopen DB sau app restart vẫn đọc đúng dữ liệu.
- [ ] Mô phỏng mất khóa: app yêu cầu khôi phục bản sao lưu, không tạo DB rỗng thay thế.

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
2. Evidence `docs/progress/DATA-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng DATA-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
