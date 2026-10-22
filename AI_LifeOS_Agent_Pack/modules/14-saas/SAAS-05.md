# SAAS-05 Subscription và entitlement server

Mức sản phẩm: **SAAS**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Chuẩn bị gói trả phí dùng test mode và policy phù hợp kênh phân phối, chưa thu tiền thật.

## Nhiệm vụ cần có trước

[SAAS-02](../../modules/14-saas/SAAS-02.md), [SAAS-04](../../modules/14-saas/SAAS-04.md)

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

- `server/src/main/kotlin/vn/lifeos/server/billing/`
- `android/app/src/main/kotlin/vn/lifeos/app/subscription/`
- `docs/setup/BILLING.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Lập feature matrix FREE/PERSONAL/PRO, giữ quyền đọc/xuất dữ liệu cá nhân kể cả gói hết hạn.
2. Chọn Play Billing hoặc checkout web theo chính sách hiện hành của kênh phát hành; ghi nguồn và quyết định trong BILLING.md.
3. Receipt/purchase token xác minh ở server; webhook có signature, idempotency và replay protection.
4. Entitlement là dữ liệu server, client không tự bật PRO bằng preference; kiểm expiration/refund/grace period.
5. Sandbox/test purchase trước; không tạo sản phẩm/thu tiền production nếu chưa được người dùng chỉ đạo riêng.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-SAAS-05-01** Entitlement thay đổi theo giao dịch đã xác minh, duplicate webhook không cộng gói hai lần.
- [ ] **RQ-SAAS-05-02** Hết gói vẫn xem/xuất dữ liệu và dùng local core theo feature matrix đã công bố.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Webhook refund thu hồi entitlement theo policy, không xóa ledger.
- [ ] Token mua hàng giả bị từ chối.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location server
.\gradlew.bat test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/SAAS-05.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng SAAS-05 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
