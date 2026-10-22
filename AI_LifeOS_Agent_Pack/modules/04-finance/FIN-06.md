# FIN-06 Theo dõi cho vay và đi vay

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tách gốc vay, lãi và phí để lời khuyên chi tiêu không dựa trên thu nhập hoặc chi phí giả.

## Nhiệm vụ cần có trước

[FIN-03](../../modules/04-finance/FIN-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/debt/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/debt/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/debt/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tạo DebtAgreement role LENDER/BORROWER, currency, counterparty label và principal movements.
2. Giải ngân/thu hồi gốc dùng loan-principal kinds, ghi debt movement và ledger entry cùng transaction.
3. Lãi/phí là income/expense riêng; khoản trả gồm gốc+lãi phải tách component rõ khi review.
4. Hiển thị principal outstanding và hạn đã nhập; không tự dự báo khả năng trả nợ khi thiếu dữ liệu.
5. Validate overpayment, currency và ID đối tác; audit cho điều chỉnh, không xóa posted ledger.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-06-01** Gốc vay/cho vay và hoàn gốc thay đúng balance/principal, không cộng thu/chi tiêu dùng.
- [ ] **RQ-FIN-06-02** Lãi/phí được thống kê riêng, principal outstanding đối chiếu được từng movement.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Vay 5000000, trả gốc 1000000 -> debt 4000000, income/expense 0.
- [ ] Trả thêm lãi 100000 chỉ tăng expense 100000.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FIN-06.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-06 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
