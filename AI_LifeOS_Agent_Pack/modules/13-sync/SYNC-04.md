# SYNC-04 Giải quyết xung đột nhiều thiết bị

Mức sản phẩm: **SAAS**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Người dùng xử lý xung đột có ý nghĩa, tránh last-write-wins cho tài chính.

## Nhiệm vụ cần có trước

[SYNC-02](../../modules/13-sync/SYNC-02.md), [SYNC-03](../../modules/13-sync/SYNC-03.md), [FIN-08](../../modules/04-finance/FIN-08.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.
- [14_API_CONTRACTS.md](../../14_API_CONTRACTS.md): đúng route/wire và quy tắc idempotency liên quan.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/sync/conflicts/`
- `android/app/src/main/kotlin/vn/lifeos/app/settings/sync/`
- `server/src/main/kotlin/vn/lifeos/server/sync/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Task/note có version conflict preview local/remote, cho chọn/merge; không tự merge mọi field.
2. Tài chính đã post bất biến: sửa là reversal/replacement và reversesId unique; từ chối hai reversal cạnh tranh.
3. Remote delete và local edit có trạng thái conflict, không hồi sinh entity âm thầm.
4. Tách sync cache nguồn ngoài với data người dùng: imported mail/raw health không lên cloud nếu policy cấm.
5. Tạo integration test hai client offline rồi reconnect cùng workspace.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-SYNC-04-01** Hai thiết bị sửa một khoản không tạo hai sổ thu/chi hoặc hai reversal.
- [ ] **RQ-SYNC-04-02** Xung đột hiện tới người dùng và giải quyết xong có thể sync tiếp.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Máy A sửa note, máy B complete task -> version conflict giữ đủ cả dữ liệu để chọn.
- [ ] A/B cùng sửa expense post -> server giữ một reversal hợp lệ, B phải review.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/SYNC-04.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng SYNC-04 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
