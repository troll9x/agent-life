# FIN-07 Giao dịch định kỳ và khoản dự kiến

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Nhắc lương/hóa đơn dự kiến trong app nhưng chỉ ghi sổ khi có xác nhận.

## Nhiệm vụ cần có trước

[FIN-02](../../modules/04-finance/FIN-02.md), [FIN-04](../../modules/04-finance/FIN-04.md), [PLAN-03](../../modules/03-planner/PLAN-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/scheduled/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/scheduled/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/scheduled/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tạo ScheduledTransaction với recurrence, nextDueDate, amount/category/account template và enabled.
2. Đến occurrence sinh một PlannedExpense hoặc candidate có khóa ruleId+occurrenceDate, không post.
3. Xác nhận dùng use case ghi sổ và link occurrence; ignore/skip không đổi balance.
4. Email/notification cùng hóa đơn đề xuất ghép occurrence đã có; không tạo khoản định kỳ lần hai.
5. Tích hợp reminder chung, đổi rule ảnh hưởng lần tương lai, giữ lịch sử đã ghi.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-07-01** Khoản dự kiến/lặp chưa xác nhận không làm thay đổi balance hoặc thực chi.
- [ ] **RQ-FIN-07-02** Mỗi occurrence có tối đa một khoản được post sau approve, có thể ghép với bằng chứng ngân hàng.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Tiền nhà dự kiến ngày 1 không tự trừ ví khi tới ngày.
- [ ] Restart worker cùng occurrence không sinh hai khoản.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FIN-07.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-07 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
