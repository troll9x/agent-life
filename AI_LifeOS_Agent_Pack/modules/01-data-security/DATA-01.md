# DATA-01 Room và migration nền tảng

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Dựng database gốc và repository hồ sơ theo hợp đồng; schema nghiệp vụ khác được thêm trong task sở hữu module đó.

## Nhiệm vụ cần có trước

[FND-03](../../modules/00-foundation/FND-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/db/`
- `android/data/schemas/`
- `android/data/src/androidTest/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tạo WorkspaceEntity, ProfileEntity, DeviceEntity, IntegrationConnectionEntity, SyncCheckpointEntity, AuditEventEntity theo 04_DATA_CONTRACTS.md.
2. Bật foreign keys và index theo workspace/provider/account; export Room schema vào git.
3. DAO trả Flow; mỗi command ghi DB và audit cần thiết trong cùng transaction.
4. Cài repository interfaces từ domain; xử lý lỗi DB thành AppError, không query DB từ Compose.
5. Tạo migration test bảo toàn hồ sơ; tuyệt đối không dùng fallbackToDestructiveMigration cho dữ liệu người dùng.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-DATA-01-01** Hồ sơ và trạng thái kết nối tồn tại sau đóng/mở tiến trình, UI quan sát qua repository.
- [ ] **RQ-DATA-01-02** Migration giữ nguyên ID, số lượng và dữ liệu đã ghi; schema history được lưu trong repo.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Nâng từ DB fixture phiên bản trước lên phiên bản mới không làm mất profile.
- [ ] Hai connection cùng provider nhưng khác account không dùng chung checkpoint.

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
2. Evidence `docs/progress/DATA-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng DATA-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
