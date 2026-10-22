# FIN-10 Đối soát tài khoản và chênh lệch

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Người dùng so sánh số dư thực với ledger và biết dữ liệu nào còn thiếu.

## Nhiệm vụ cần có trước

[FIN-04](../../modules/04-finance/FIN-04.md), [FIN-09](../../modules/04-finance/FIN-09.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/reconciliation/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/reconciliation/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/reconciliation/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Nhập observed balance và asOfInstant theo account/currency; tính expected balance cùng mốc.
2. Lưu snapshot expected/observed/difference và trạng thái chưa đối soát/đã đối soát.
3. Cho xem ledger và candidates quanh kỳ để tìm thiếu/trùng; không tự thêm khoản cho khớp.
4. Nếu người dùng chọn adjustment, mở flow FIN-09 có lý do và xác nhận riêng.
5. Cung cấp data quality flags cho module phân tích/AI: unreconciled, missing-source, pending candidate.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-10-01** Đối soát cùng mốc thời gian ra đúng difference và không tự thay balance.
- [ ] **RQ-FIN-10-02** Trạng thái thiếu nguồn/chưa đối soát được truyền vào báo cáo và lời khuyên AI.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Expected 9400000, observed 9300000 -> difference -100000, ledger không đổi.
- [ ] Có 3 candidates pending không được AI coi số thu/chi đã đầy đủ.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FIN-10.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-10 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
