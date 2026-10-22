# INSIGHT-01 Công thức tổng hợp tài chính nội bộ

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tạo một analytics model dùng chung cho report/chart/AI, có công thức và kiểm số liệu chuẩn.

## Nhiệm vụ cần có trước

[FIN-03](../../modules/04-finance/FIN-03.md), [FIN-10](../../modules/04-finance/FIN-10.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/analytics/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/analytics/`
- `fixtures/finance-analytics/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Đọc 13_FINANCE_ANALYTICS.md; query từ ledger theo category ID/year/period/currency và cùng snapshot revision.
2. Tính income, gross expense, refunds, net expense, budgetRemaining, operatingSurplus; account balance là chỉ số riêng.
3. Year summary luôn gồm 12 tháng đã chọn; năm khác nhau không chung bucket dù cùng tháng.
4. Category share chỉ khi mẫu số hợp lệ; tháng trước 0/không có dữ liệu không cho % tăng trưởng giả.
5. Tạo fixture số liệu giả lấy cấu trúc mẫu và case refund/transfer/adjustment/cross-year; không gọi Sheets API.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-INSIGHT-01-01** Report/chart/AI cùng dùng một kết quả tính từ ledger; số dư thực và ngân sách còn lại được phân biệt.
- [ ] **RQ-INSIGHT-01-02** Tổng năm đủ 12 tháng, chia 0/thiếu tháng/refund âm xử lý rõ và không cần Google account.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] 12 tháng income 15000000 và chỉ tháng 1 expense 1000000 -> năm surplus 179000000.
- [ ] Category expense 400000/total 1000000 -> share 40%; không gộp VND/USD.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :domain:test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/INSIGHT-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng INSIGHT-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
