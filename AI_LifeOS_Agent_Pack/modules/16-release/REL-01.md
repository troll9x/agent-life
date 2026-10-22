# REL-01 Nghiệm thu bản cá nhân ngoại tuyến

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Kiểm checklist PERSONAL trên APK thật và chuẩn bị bản cài reviewable.

## Nhiệm vụ cần có trước

[UX-03](../../modules/02-app-shell/UX-03.md), [PLAN-03](../../modules/03-planner/PLAN-03.md), [FIN-05](../../modules/04-finance/FIN-05.md), [FIN-06](../../modules/04-finance/FIN-06.md), [FIN-07](../../modules/04-finance/FIN-07.md), [FIN-10](../../modules/04-finance/FIN-10.md), [INSIGHT-04](../../modules/10-finance-insights/INSIGHT-04.md), [HEALTH-04](../../modules/08-health-nutrition/HEALTH-04.md), [AI-04](../../modules/11-ai-assistant/AI-04.md), [BACKUP-03](../../modules/12-backup-export/BACKUP-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.
- [05_BRIEFING_CONTRACT.md](../../05_BRIEFING_CONTRACT.md): nội dung, giới hạn và trạng thái bản tin.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `docs/verification/personal/`
- `docs/progress/`
- `android/app/build.gradle.kts`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Đi toàn bộ yêu cầu PERSONAL trong 01_PRODUCT_CHECKLIST.md; ghi PASS/FAIL/BLOCKED/NOT_RUN kèm evidence.
2. Chạy scenario offline tạo/sửa task/cash/meal, restart, đổi giờ, backup/restore và số tiền fixture.
3. Kiểm build release, R8, signing cấu hình ngoài repo, native ABI/page size tương thích nếu dùng SQLCipher.
4. Kiểm tiếng Việt, font 1.3x, dark mode, quyền bị từ chối, lockscreen privacy và lưu trữ khi gần đầy.
5. Tạo changelog/known issues và đường dẫn APK; mọi mục thiếu vẫn chưa tick, không phát hành production tự động.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-REL-01-01** Tất cả requirement PERSONAL có bằng chứng hoặc trạng thái chưa đạt rõ ràng.
- [ ] **RQ-REL-01-02** Có APK cài được và backup/restore trên installation khác đã kiểm; chưa kiểm thì gate còn mở.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Offline 24h, app restart, không mất giao dịch/task.
- [ ] Sai passphrase không làm mất dữ liệu đang có.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/REL-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng REL-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
