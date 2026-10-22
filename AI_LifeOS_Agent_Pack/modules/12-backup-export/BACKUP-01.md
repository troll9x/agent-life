# BACKUP-01 Định dạng sao lưu mã hóa portable

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Xuất backup do người dùng kiểm soát, không phụ thuộc khóa Keystore của máy cũ.

## Nhiệm vụ cần có trước

[DATA-02](../../modules/01-data-security/DATA-02.md), [DATA-04](../../modules/01-data-security/DATA-04.md), [FIN-02](../../modules/04-finance/FIN-02.md), [PLAN-02](../../modules/03-planner/PLAN-02.md), [HEALTH-02](../../modules/08-health-nutrition/HEALTH-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/backup/`
- `android/domain/src/main/kotlin/vn/lifeos/domain/backup/`
- `docs/engineering/BACKUP_FORMAT.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Backup envelope có formatVersion, schemaVersion, createdAt, manifest và authenticated encryption bằng thư viện crypto đã kiểm chứng.
2. Dùng passphrase KDF có salt ngẫu nhiên và tham số đã ghi; không tự chế thuật toán hoặc lưu passphrase.
3. Xuất logical snapshot consistent gồm hồ sơ/ledger/task/health; loại tokens, raw email, Keystore wrapped key và device-only secrets.
4. Dùng Storage Access Framework để người dùng chọn file, ghi temp rồi hoàn tất; lỗi không làm mất DB gốc.
5. Có thông tin nhắc người dùng giữ passphrase; không gửi backup ra dịch vụ khác tự động.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-BACKUP-01-01** Backup mã hóa không đọc được plaintext và có thể giải bằng passphrase trên installation mới.
- [ ] **RQ-BACKUP-01-02** Snapshot khôi phục giữ đúng ID, balance và task/health, không mang token OAuth theo.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Đổi một byte ciphertext phải fail integrity, không import nửa chừng.
- [ ] Sai passphrase không ảnh hưởng DB đang dùng.

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
2. Evidence `docs/progress/BACKUP-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng BACKUP-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
