# FIN-04 Hàng chờ nhập và ghép bằng chứng chống trùng

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Một nghiệp vụ ngân hàng có thể có nhiều evidence nhưng chỉ ghi sổ một lần; đối soát số dư là FIN-10.

## Nhiệm vụ cần có trước

[FIN-01](../../modules/04-finance/FIN-01.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/imports/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/imports/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/inbox/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Candidate tách khỏi ledger; SourceEvidence unique theo connection/source event identity, có thể nhiều evidence -> một transaction.
2. Match theo bankReference + account + direction trước; fingerprint amount/time chỉ gợi ý.
3. Approve candidate trong DB transaction cùng evidence link; duplicate decision ghép vào khoản đã có; ignore không thay balance.
4. Hai match khả dĩ luôn NEEDS_REVIEW; không xóa/gộp giao dịch thực vì cùng số tiền.
5. Giữ nguồn notification/email/nhập tay và parserVersion để xem lại; không lưu raw text quá retention.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-04-01** Thông báo/email/nhập tay cùng khoản sau quyết định ghép chỉ làm ghi sổ một lần.
- [ ] **RQ-FIN-04-02** Giao dịch cùng tiền khác reference được giữ riêng; mơ hồ luôn chờ người dùng.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Lương nhập tay rồi notification và email -> một transaction với nhiều evidence.
- [ ] Hai khoản 200000 cùng phút khác ref vẫn là hai khoản.

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
2. Evidence `docs/progress/FIN-04.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-04 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
