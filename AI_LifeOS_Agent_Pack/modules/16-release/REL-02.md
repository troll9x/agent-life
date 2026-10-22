# REL-02 Nghiệm thu tích hợp trên thiết bị và tài khoản thật

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Đóng các gate ngoài hệ thống bằng nguồn thật trong phạm vi người dùng đã cấp.

## Nhiệm vụ cần có trước

[REL-01](../../modules/16-release/REL-01.md), [NOTI-04](../../modules/05-bank-notifications/NOTI-04.md), [MS-04](../../modules/06-microsoft/MS-04.md), [PLAN-04](../../modules/03-planner/PLAN-04.md), [BRIEF-04](../../modules/07-morning-briefing/BRIEF-04.md), [GOOGLE-02](../../modules/09-google/GOOGLE-02.md), [GOOGLE-03](../../modules/09-google/GOOGLE-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.
- [05_BRIEFING_CONTRACT.md](../../05_BRIEFING_CONTRACT.md): nội dung, giới hạn và trạng thái bản tin.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `docs/verification/connected/`
- `docs/progress/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Kiểm Microsoft tenant/account type, Mail.Read/Calendar quyền thật và không thay đổi mailbox.
2. Kiểm notification ngân hàng đúng package/mẫu, không giữ nội dung app khác, thu hồi permission và đổi format.
3. Kiểm Google chỉ cho Gmail/Calendar; tài chính, chart và coaching vẫn chạy không Google. Xác minh không có Sheets/Drive permission trong app.
4. Kiểm nhắc briefing khi máy ngủ, reboot, offline và nguồn partial; ghi device/OEM/API, không suy rộng một máy thành mọi Android.
5. Test fixture PASS không thay test live; gate thiếu account/admin/device phải EXTERNAL_BLOCKED có cách mở.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-REL-02-01** Yêu cầu CONNECTED được đối chiếu thực tế, bao gồm nguồn lỗi và trạng thái partial.
- [ ] **RQ-REL-02-02** Những provider chưa được cấp quyền không bị đánh đã tích hợp hoàn chỉnh.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Cùng giao dịch qua mail + notification + manual match chỉ ghi một lần.
- [ ] Calendar bị chặn không tạo lời khẳng định lịch trống.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/REL-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng REL-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
