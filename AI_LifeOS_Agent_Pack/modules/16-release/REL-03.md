# REL-03 Nghiệm thu SaaS và bàn giao cuối

Mức sản phẩm: **SAAS**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Đối chiếu bản thương mại với checklist và chuẩn bị quyết định phát hành.

## Nhiệm vụ cần có trước

[REL-02](../../modules/16-release/REL-02.md), [SAAS-06](../../modules/14-saas/SAAS-06.md), [SYNC-04](../../modules/13-sync/SYNC-04.md)

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

- `docs/verification/final/`
- `docs/progress/`
- `docs/operations/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Kiểm toàn bộ SAAS requirement: authz tenant, hai client offline/reconnect, quota, billing sandbox, scheduler và backup restore.
2. Tổng hợp checklist FULL = PERSONAL+CONNECTED+SAAS; OPTIONAL chỉ vào phạm vi nếu đã bật trong release scope.
3. Thực hiện kịch bản E2E ngày làm việc gồm bản tin 09:00, task, cash, ngân hàng, chart tài chính, lời khuyên AI, health và reconnect.
4. Ghi test commands thật, commit/version, thiết bị, môi trường, evidence đã làm sạch và unresolved issues.
5. Bàn giao release notes, runbooks, data export guide và gates cần user/admin/provider; chỉ mark hoàn tất khi mọi gate bắt buộc đã đạt.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-REL-03-01** Không có requirement bắt buộc thiếu owner/task/evidence mà vẫn báo hoàn thiện.
- [ ] **RQ-REL-03-02** Hai tenant và hai thiết bị kiểm đúng; có kế hoạch release và rollback reviewable.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Mất mạng trước 09:00 và reconnect sau đó hiển thị snapshot đúng ngày/source status.
- [ ] Hết subscription không chặn người dùng xuất dữ liệu của mình.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
# Chạy ở repo root, mỗi phần mở terminal đúng thư mục:
# android/: gradlew.bat :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug
# server/: gradlew.bat test
# Thiết bị và live gates: theo 08_ACCEPTANCE_SCENARIOS và task cụ thể.
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/REL-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng REL-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
