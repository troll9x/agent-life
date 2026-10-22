# CHANNEL-02 Kênh laptop và chọn kênh chính

Mức sản phẩm: **OPTIONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Cung cấp web/PWA companion tối giản hoặc hợp đồng kênh desktop để nhận nhắc trên laptop.

## Nhiệm vụ cần có trước

[SAAS-03](../../modules/14-saas/SAAS-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [05_BRIEFING_CONTRACT.md](../../05_BRIEFING_CONTRACT.md): nội dung, giới hạn và trạng thái bản tin.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `web/`
- `server/src/main/kotlin/vn/lifeos/server/channels/`
- `docs/setup/DESKTOP_NOTIFICATIONS.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Chọn một phương án web/PWA sau khi kiểm API browser chính thức; không dựng Windows service đọc Outlook như mặc định.
2. Login SaaS, render briefing snapshot và đăng ký notification riêng theo thiết bị có permission.
3. Cài chọn Android/Telegram/laptop là kênh chính hoặc nhiều kênh chủ đích; de-dup theo device/channel.
4. Không hiển thị nội dung email nhạy cảm trên lockscreen mặc định; deep link cần đăng nhập.
5. Tài liệu chuyển từ lịch AI Morning Briefing trong Codex là thao tác riêng của người dùng, app không tự archive/tắt automation.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-CHANNEL-02-01** Laptop có thể xem cùng briefing snapshot của tài khoản đã xác thực.
- [ ] **RQ-CHANNEL-02-02** Người dùng quản lý được kênh nhận và hiểu khả năng trùng với tác vụ Codex cũ.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Browser từ chối notification vẫn xem được briefing trong web.
- [ ] Link briefing workspace khác bị authz chặn.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
# Chạy test/build của web companion sau khi task khai báo command thật.
# Ghi command vào docs/engineering/TESTING.md và evidence.
# Đồng thời kiểm server: server/gradlew.bat test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/CHANNEL-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng CHANNEL-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
