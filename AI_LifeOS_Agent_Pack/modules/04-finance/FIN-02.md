# FIN-02 Nhập thu chi, tiền mặt và chuyển khoản

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Làm form và use case thu/chi/chuyển khoản ngoại tuyến; các nghiệp vụ sửa/hoàn tiền được tách task riêng.

## Nhiệm vụ cần có trước

[FIN-01](../../modules/04-finance/FIN-01.md), [UX-02](../../modules/02-app-shell/UX-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/app/src/main/kotlin/vn/lifeos/app/finance/entry/`
- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/commands/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Làm form thu/chi có account, category, magnitude amount dương, ngày/giờ, payee/note và nguồn MANUAL.
2. Tiền mặt là Account kind CASH, không là một cột tổng phụ tách sổ. Cho chọn ví mặc định để nhập nhanh.
3. Chuyển khoản chọn hai account khác nhau cùng currency; ghi header và hai entry đối ứng atomically.
4. Validate theo domain, submit có operationId ổn định; double tap/restart không tạo thêm giao dịch.
5. Hiển thị xác nhận và danh sách giao dịch sau post; không tự tích hợp refund/reversal vào task này.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FIN-02-01** Ghi thu/chi và tiền mặt hoạt động offline, dữ liệu còn sau đóng/mở app.
- [ ] **RQ-FIN-02-02** Chuyển khoản nội bộ và submit lặp giữ đúng số dư, không tăng income/expense.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Chuyển 1.000.000 bank -> cash thay số dư hai ví và tổng thu/chi không đổi.
- [ ] Bấm lưu cùng form 2 lần vẫn một transaction, không cần mạng.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FIN-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FIN-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
