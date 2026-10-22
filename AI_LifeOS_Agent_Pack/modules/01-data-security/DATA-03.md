# DATA-03 Nền HTTP và trạng thái kết nối

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Dựng adapter HTTP dùng chung cho Microsoft, Google, tin tức và backend sau này.

## Nhiệm vụ cần có trước

[DATA-01](../../modules/01-data-security/DATA-01.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/network/`
- `android/domain/src/main/kotlin/vn/lifeos/domain/integration/`
- `android/data/src/test/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tạo HTTP client có timeout, cancellation, giới hạn kích thước phản hồi và JSON serialization theo SDK ổn định đã khóa.
2. Tách token provider khỏi HTTP client; gắn token đúng host/provider, không gửi bearer token khi redirect sang host khác.
3. Xử lý 401 thành AUTH_REQUIRED sau thử refresh hợp lệ, 403 thành PERMISSION_DENIED, 429 theo Retry-After; GET retry có giới hạn.
4. POST có tác dụng phụ chỉ retry khi hợp đồng idempotency hoặc reconcile cho phép; không retry mù.
5. Mock HTTP server fixture để kiểm tra phân trang, timeout, HTML thay JSON và log đã che dữ liệu.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-DATA-03-01** 401/403/429/offline tạo trạng thái nguồn rõ ràng, không được biến thành danh sách rỗng thành công.
- [ ] **RQ-DATA-03-02** Log không chứa bearer token, nội dung email hoặc thông tin tài khoản ngân hàng.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] 429 Retry-After=60 không tạo vòng lặp nhanh.
- [ ] Redirect URL lạ không nhận Authorization header.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug
# Khi thay Room/migration và đã có emulator/device:
.\gradlew.bat :data:connectedDebugAndroidTest
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/DATA-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng DATA-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
