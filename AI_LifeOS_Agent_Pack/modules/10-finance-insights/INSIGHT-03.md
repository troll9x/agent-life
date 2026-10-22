# INSIGHT-03 Cảnh báo chi tiêu và so sánh kỳ

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tạo nhận xét tất định và cảnh báo có lý do trước khi dùng LLM.

## Nhiệm vụ cần có trước

[INSIGHT-01](../../modules/10-finance-insights/INSIGHT-01.md), [FIN-07](../../modules/04-finance/FIN-07.md), [PLAN-03](../../modules/03-planner/PLAN-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/insights/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/insights/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Rule overspend theo ngân sách user đặt, category trend và khoản định kỳ sắp tới; tránh áp tỷ lệ ngân sách chung cho mọi người.
2. So kỳ đang chạy với cùng số ngày ở kỳ trước hoặc ghi rõ full period comparison; không so nửa tháng với cả tháng mà gọi tăng/giảm tuyệt đối.
3. Run-rate chỉ là ước tính có coverage/asOf/assumptions; thiếu dữ liệu hoặc pending/unreconciled thì hạ độ tin cậy hoặc không dự báo.
4. Người dùng cấu hình threshold/tần suất; notification dedup theo rule/category/period/threshold vượt, không spam mỗi giao dịch.
5. Mỗi insight giữ metric IDs, số gốc, period và đường mở report; không tự cắt ngân sách hoặc xóa giao dịch.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-INSIGHT-03-01** Cảnh báo vượt ngân sách/biến động có số liệu và khoảng thời gian đối chiếu rõ.
- [ ] **RQ-INSIGHT-03-02** Ước tính chi cuối kỳ được gắn giả định/thiếu dữ liệu và không được trình bày như số chắc chắn.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Budget 800000, actual 900000 -> overspend 100000, không ghi thiếu tiền mặt 100000.
- [ ] Tháng trước chi 0 -> ghi không tính được phần trăm, không infinity%.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :domain:test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/INSIGHT-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng INSIGHT-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
