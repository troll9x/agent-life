# FIN-09 Hoàn tiền và điều chỉnh số dư có lý do

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Ghi refund/adjustment chính xác để báo cáo và chart phân biệt được khoản ngoại lệ.

## Nhiệm vụ cần có trước

[FIN-02](../../modules/04-finance/FIN-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/refund/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/refund/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/refund/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Refund link expense gốc, delta dương và category gốc; hỗ trợ partial và ngày refund thực.
2. Tổng refund không vượt gốc còn lại nếu chưa có rule override đã review; concurrency dùng DB transaction.
3. Adjustment có signed delta, lý do bắt buộc và được loại khỏi income/expense nhưng tác động balance.
4. Không tạo adjustment tự động từ màn đối soát; preview tác động trước post.
5. Refund khác kỳ giảm net expense kỳ refund, không âm thầm sửa tháng cũ.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-09-01** Chi 500000, refund 100000 -> net expense 400000 và income 0 trong cùng kỳ.
- [ ] **RQ-FIN-09-02** Adjustment đổi số dư nhưng không làm AI/report coi đó là thu nhập hoặc chi phí tiêu dùng.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Refund 600000 của khoản 500000 bị từ chối hoặc cần quy trình override rõ.
- [ ] Tháng chỉ có refund hiện net expense âm bằng bar/table, không ép pie chart số âm.

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
2. Evidence `docs/progress/FIN-09.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-09 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
