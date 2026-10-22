# FIN-03 Ngân sách và báo cáo tháng

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Hiển thị tổng thu, net chi, dòng tiền và ngân sách theo cùng định nghĩa.

## Nhiệm vụ cần có trước

[FIN-02](../../modules/04-finance/FIN-02.md), [FIN-08](../../modules/04-finance/FIN-08.md), [FIN-09](../../modules/04-finance/FIN-09.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/reports/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/reports/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Đặt ngân sách theo category, currency và kỳ tùy profile; category dùng ID ổn định, rename không mất lịch sử.
2. Báo cáo lọc kỳ [start,end), currency và account; loại transfer/adjustment; reversal triệt tiêu khoản gốc, refund giảm chi.
3. Hiển thị budget remaining và cảnh báo vượt ngưỡng có thể bật/tắt; không gộp nhiều currency.
4. Cho drill-down từ tổng đến danh sách giao dịch đúng bộ lọc; xuất CSV không có formula injection.
5. Ghi chính sách kỳ refund hiện tại và so sánh tháng trước dựa trên cùng length-of-period semantics.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-03-01** Số trên dashboard, danh sách và CSV đối chiếu được cùng kỳ/currency.
- [ ] **RQ-FIN-03-02** Chuyển tiền sang tiết kiệm không tiêu ngân sách chi phí; refund giảm expense theo kỳ nhận refund.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Kỳ bắt đầu ngày 15 không gồm giao dịch 14/10 lúc 23:59 địa phương.
- [ ] Tên category =SUM(1,2) xuất CSV như text, không công thức.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FIN-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
