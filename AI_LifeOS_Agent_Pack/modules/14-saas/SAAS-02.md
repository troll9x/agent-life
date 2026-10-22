# SAAS-02 OIDC và quyền workspace

Mức sản phẩm: **SAAS**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Có đăng nhập SaaS độc lập với quyền đọc Microsoft/Google và tenant boundaries.

## Nhiệm vụ cần có trước

[SAAS-01](../../modules/14-saas/SAAS-01.md), [SYNC-01](../../modules/13-sync/SYNC-01.md)

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

- `server/src/main/kotlin/vn/lifeos/server/auth/`
- `server/src/main/kotlin/vn/lifeos/server/workspace/`
- `server/src/main/resources/db/migration/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Dùng OIDC provider cấu hình được; verify JWT signature, issuer, audience, expiry và JWKS refresh; không tự viết password system.
2. Tạo User, Workspace, Membership(OWNER/MEMBER), DeviceSession; mọi route resolve quyền server-side.
3. Tạo test fixture JWT keys riêng cho test; fake auth chỉ trong test/debug, production không bật bypass.
4. Android login SaaS qua browser/SDK được hỗ trợ, lưu token an toàn; Microsoft mail consent vẫn là luồng tính năng riêng.
5. Thêm integration test IDOR qua path/body/cursor và revoke device/session.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-SAAS-02-01** User không đọc/ghi workspace khác bằng thay ID ở URL, JSON hoặc cursor.
- [ ] **RQ-SAAS-02-02** Token hết hạn/sai issuer/audience/signature bị từ chối; app local vẫn dùng khi SaaS logout.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Membership bị revoke giữa các request thì request sau denied.
- [ ] Token Microsoft Graph không được chấp nhận như token SaaS nếu issuer/audience sai.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location server
.\gradlew.bat test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/SAAS-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng SAAS-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
