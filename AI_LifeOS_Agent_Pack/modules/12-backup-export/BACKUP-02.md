# BACKUP-02 Khôi phục an toàn và migration

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Khôi phục bản sao lưu thật sự qua preflight, không thay DB trước khi biết file hợp lệ.

## Nhiệm vụ cần có trước

[BACKUP-01](../../modules/12-backup-export/BACKUP-01.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/backup/restore/`
- `android/app/src/main/kotlin/vn/lifeos/app/settings/backup/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Giải vào staging storage, xác minh manifest/version/foreign keys/schema và tính lại balance.
2. Cho preview số liệu/năm/currency rồi xác nhận replace workspace; giữ recovery copy cho tới khi swap thành công.
3. Không hỗ trợ merge tùy tiện ở bản đầu; replace có bước xác nhận tác động dữ liệu.
4. Sinh khóa DB mới trên máy đích, import transactionally và chỉ swap sau khi kiểm tra xong.
5. Reset device identities/cursors phù hợp, yêu cầu reconnect OAuth và reschedule reminders.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-BACKUP-02-01** Restore sang installation khác đọc được dữ liệu bằng khóa mới, không cần khóa máy cũ.
- [ ] **RQ-BACKUP-02-02** Restore file hỏng/phiên bản chưa hỗ trợ không làm thay đổi workspace hiện tại.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Backup có 10 giao dịch, restore giữ 10 và tổng đúng.
- [ ] Power loss trước swap giữ DB cũ còn dùng được.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/BACKUP-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng BACKUP-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
