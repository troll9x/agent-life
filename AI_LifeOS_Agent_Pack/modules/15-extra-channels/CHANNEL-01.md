# CHANNEL-01 Telegram opt-in và chống gửi trùng

Mức sản phẩm: **OPTIONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Kênh Telegram chỉ gửi tới đích đã liên kết và được người dùng bật trong ứng dụng.

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

- `server/src/main/kotlin/vn/lifeos/server/channels/telegram/`
- `android/app/src/main/kotlin/vn/lifeos/app/settings/channels/`
- `docs/setup/TELEGRAM.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Bot token giữ ở backend; liên kết chat bằng mã dùng một lần, xác minh user/workspace trước khi lưu.
2. Hiển thị đích và nội dung mẫu; mặc định chỉ gửi có bản tin + link mở, không gửi full email công việc.
3. User bật kênh/schedule là consent gửi; nhiệm vụ coding chỉ dùng mock/sandbox đích test được phép, không gửi tin thật tùy tiện.
4. Delivery log unique workspace+date+channel, retry có giới hạn; 403/unlinked dừng gửi.
5. Disconnect xóa liên kết và hủy pending delivery; không thay đổi tác vụ Codex đang có.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-CHANNEL-01-01** Không gửi Telegram khi chưa có opt-in và chat đã xác minh.
- [ ] **RQ-CHANNEL-01-02** Một briefing/ngày không bị gửi nhiều lần do worker retry.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Mã liên kết hết hạn không dùng được.
- [ ] Disconnect trước giờ chạy làm pending send bị hủy.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location server
.\gradlew.bat test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/CHANNEL-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng CHANNEL-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
