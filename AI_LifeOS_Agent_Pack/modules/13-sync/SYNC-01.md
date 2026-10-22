# SYNC-01 Hợp đồng wire và change envelopes

Mức sản phẩm: **SAAS**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Đóng băng định dạng đồng bộ độc lập với Room để mobile và server hiểu giống nhau.

## Nhiệm vụ cần có trước

[FND-02](../../modules/00-foundation/FND-02.md), [FIN-01](../../modules/04-finance/FIN-01.md), [PLAN-01](../../modules/03-planner/PLAN-01.md)

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

- `contracts/`
- `android/domain/src/main/kotlin/vn/lifeos/domain/sync/`
- `docs/engineering/SYNC_PROTOCOL.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. JSON envelope có protocolVersion,eventId,workspaceId,deviceId,entityType,entityId,baseVersion,operation,payload,occurredAt.
2. Money truyền amountMinor là chuỗi số nguyên; thời gian Instant UTC, all-day LocalDate riêng; không truyền token OAuth/raw mail.
3. Push ACK cho từng event với accepted/version/conflict/rejected; pull có opaque cursor và trang hữu hạn.
4. Định nghĩa tombstone, replay, event ordering theo aggregate, batch giới hạn và retention cursor.
5. Tạo golden fixtures và schema validation cho cả Kotlin mobile/server; không cho client tự chọn owner.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-SYNC-01-01** Client/server có một wire schema versioned, money không mất chính xác khi qua JSON.
- [ ] **RQ-SYNC-01-02** Duplicate/replay/conflict/rejected có response được định nghĩa trước khi code network sync.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] amountMinor lớn hơn 2^53 vẫn roundtrip chính xác dưới dạng string.
- [ ] Payload mang workspaceId khác context không được tin.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :domain:test
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/SYNC-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng SYNC-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
