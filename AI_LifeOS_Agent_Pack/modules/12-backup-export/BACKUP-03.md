# BACKUP-03 Xuất JSON CSV và xóa dữ liệu

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Người dùng lấy dữ liệu của mình hoặc xóa có kiểm soát.

## Nhiệm vụ cần có trước

[BACKUP-02](../../modules/12-backup-export/BACKUP-02.md), [FIN-03](../../modules/04-finance/FIN-03.md), [DATA-04](../../modules/01-data-security/DATA-04.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/app/src/main/kotlin/vn/lifeos/app/settings/data/`
- `android/data/src/main/kotlin/vn/lifeos/data/export/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Xuất JSON schema version và CSV có charset/locale/tiền tệ/ISO dates, nêu không phải backup mã hóa.
2. Chặn CSV formula injection cho text bắt đầu =,+,-,@ theo policy export; số tiền xuất là number đã kiểm.
3. Data deletion preview rõ workspace/source, xác nhận bằng hành động người dùng; hủy worker và xóa đúng tệp app-owned.
4. Nếu đang có SaaS, hiển thị xóa local và cloud là hai phạm vi; cloud job do SAAS-04 xử lý.
5. Không báo export thành công nếu file chưa hoàn tất; sharing chỉ mở chooser do người dùng thao tác.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-BACKUP-03-01** JSON/CSV mở được, số tiền/date/currency đối chiếu đúng và không rò token.
- [ ] **RQ-BACKUP-03-02** Xóa local xóa dữ liệu được chọn, không xóa tệp hoặc workspace khác.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] CSV có tên payee =HYPERLINK(...) được xuất như text.
- [ ] Cancel file picker không hiện thông báo đã xuất.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/BACKUP-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng BACKUP-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
