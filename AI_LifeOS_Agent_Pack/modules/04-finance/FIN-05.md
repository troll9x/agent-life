# FIN-05 Mục tiêu và tài khoản tiết kiệm

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Theo dõi mục tiêu tiết kiệm ngay trong app, không tính chuyển tiền vào tiết kiệm là expense.

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

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/goals/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/goals/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/goals/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tạo SavingsGoal name/currency/targetMinor/targetDate/linkedAccountIds và migration.
2. Hiển thị target, số hiện có từ account được chọn và tiến độ; goal là view của ledger, không có balance riêng chỉnh tay.
3. Chuyển từ bank sang savings sử dụng TRANSFER hiện có, không tăng thu/chi.
4. Một account có thể hiển thị trong nhiều mục tiêu nhưng tổng tài sản chỉ cộng một lần; nêu rõ không phải tiền mới sinh.
5. Cho thay target hoặc archive goal, không xóa lịch sử account/giao dịch.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-05-01** Mục tiêu tiết kiệm có tiến độ lấy từ ledger và dữ liệu lưu ngoại tuyến.
- [ ] **RQ-FIN-05-02** Chuyển sang tiết kiệm không tiêu ngân sách chi phí hoặc nhân đôi tổng tài sản.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Mục tiêu 10000000, chuyển 2000000 vào savings -> tiến độ 20%, expense 0.
- [ ] Archive mục tiêu không xóa account tiết kiệm.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FIN-05.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-05 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
