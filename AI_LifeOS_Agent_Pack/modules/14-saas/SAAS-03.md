# SAAS-03 Scheduler bản tin và nguồn cloud opt-in

Mức sản phẩm: **SAAS**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tạo bản tin theo timezone ngay cả khi Android không đang chạy, khi người dùng cho phép xử lý cloud.

## Nhiệm vụ cần có trước

[SAAS-02](../../modules/14-saas/SAAS-02.md), [BRIEF-04](../../modules/07-morning-briefing/BRIEF-04.md), [SYNC-03](../../modules/13-sync/SYNC-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [05_BRIEFING_CONTRACT.md](../../05_BRIEFING_CONTRACT.md): nội dung, giới hạn và trạng thái bản tin.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.
- [14_API_CONTRACTS.md](../../14_API_CONTRACTS.md): đúng route/wire và quy tắc idempotency liên quan.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `server/src/main/kotlin/vn/lifeos/server/briefing/`
- `server/src/main/kotlin/vn/lifeos/server/integrations/`
- `docs/setup/SERVER_INTEGRATIONS.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Backend consent riêng cho nguồn cloud: dùng confidential authorization flow chính thức, refresh token mã hóa qua secret manager/KMS; không upload cache MSAL từ Android.
2. Scheduler lưu nextRunAt theo ZoneId, lease/job key unique workspace+date; restart không chạy trùng.
3. Adapters Microsoft/Google có checkpoint, renewal webhook/subscription khi dùng và retry giới hạn; thu hồi quyền ngừng job tương ứng.
4. Push chỉ chứa briefing ID và trạng thái, không full email; nếu device offline giữ snapshot và hiển thị lúc kết nối.
5. Không hứa thông báo tới thiết bị đúng giây; record server generatedAt, push attempt và delivery khi quan sát được.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-SAAS-03-01** Một ngày/workspace có một logical briefing, chạy lại không nhân đôi notification.
- [ ] **RQ-SAAS-03-02** Không bật cloud consent thì backend không đọc email/sức khỏe hoặc nhận token nguồn đó.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Server restart 08:59/09:01 chỉ có một job result cho ngày đó.
- [ ] Microsoft token revoked -> briefing partial, phần RSS vẫn được tạo.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location server
.\gradlew.bat test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/SAAS-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng SAAS-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
