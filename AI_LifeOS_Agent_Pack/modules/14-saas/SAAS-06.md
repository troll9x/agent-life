# SAAS-06 Vận hành, backup server và xóa cloud

Mức sản phẩm: **SAAS**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Có cách vận hành và khôi phục được chứng minh trước khi nhận người dùng thương mại.

## Nhiệm vụ cần có trước

[SAAS-03](../../modules/14-saas/SAAS-03.md), [SAAS-04](../../modules/14-saas/SAAS-04.md), [SAAS-05](../../modules/14-saas/SAAS-05.md), [SYNC-04](../../modules/13-sync/SYNC-04.md)

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

- `infra/`
- `server/src/main/kotlin/vn/lifeos/server/privacy/`
- `docs/operations/`
- `docs/verification/saas/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Viết runbook deploy/rollback, schema migration rollout, TLS, secrets rotation và least privilege.
2. Metric/alert queue lag, briefing failure, auth errors, AI quota và backup age không chứa nội dung email.
3. Backup PostgreSQL/object storage, khôi phục sang staging và đối chiếu count/checksum/tenant isolation; RPO/RTO là mục tiêu cần đo, không tự nhận đã đạt.
4. Cloud export/delete job xóa đúng tenant, object, tokens và cache; xử lý backup retention đã công bố.
5. Tạo go-live checklist gồm OAuth/provider approval, privacy terms, billing sandbox và performance test theo tải được chốt; chuẩn bị triển khai reviewable.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-SAAS-06-01** Có restore drill thực tế ở staging và deletion job không ảnh hưởng tenant khác.
- [ ] **RQ-SAAS-06-02** Runbook nêu deployment/release gates còn thiếu; chưa deploy production không đánh đã triển khai.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Xóa workspace A không xóa B hoặc shared catalog.
- [ ] Khôi phục DB backup staging cho ra số liệu đã ghi nhận trước backup.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location server
.\gradlew.bat test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/SAAS-06.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng SAAS-06 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
