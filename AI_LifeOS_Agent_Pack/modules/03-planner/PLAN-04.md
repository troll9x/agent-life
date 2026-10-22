# PLAN-04 Đề xuất công việc từ nguồn ngoài

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Biến email công việc thành đề xuất có bằng chứng và tránh tạo task trùng.

## Nhiệm vụ cần có trước

[PLAN-02](../../modules/03-planner/PLAN-02.md), [MS-03](../../modules/06-microsoft/MS-03.md), [AI-02](../../modules/11-ai-assistant/AI-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/planner/proposals/`
- `android/app/src/main/kotlin/vn/lifeos/app/planner/proposals/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Nhận ActionProposal từ email, hiển thị email nguồn, câu có deadline và lý do ưu tiên.
2. Không tự đoán deadline khi thư chỉ ghi sớm/khẩn; giữ deadline null và nhãn cần xác nhận.
3. Approve tạo task cùng link nguồn trong một DB transaction; unique proposal decision ngăn double click.
4. Update cùng conversation cho thấy nội dung mới và task đã liên quan; không tự đổi deadline đã được sửa tay.
5. Cho bỏ qua/hoãn/xem nguồn và đánh dấu task hoàn thành độc lập với trạng thái email.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-PLAN-04-01** Email chưa được xác nhận không tự tạo hoặc sửa task/lịch.
- [ ] **RQ-PLAN-04-02** Approve một đề xuất hai lần chỉ tạo một task, có nguồn và deadline đã xác nhận.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Email ghi trước 17h ngày 20/10/2026: đề xuất deadline đúng múi giờ người gửi/ngữ cảnh hoặc yêu cầu làm rõ.
- [ ] Email ghi làm sớm: không tự đặt hạn hôm nay.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/PLAN-04.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng PLAN-04 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
