# GOOGLE-01 Google OAuth cho Gmail và Calendar

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Kết nối Google tùy chọn cho email và lịch, không cần quyền Drive hoặc Sheets.

## Nhiệm vụ cần có trước

[DATA-03](../../modules/01-data-security/DATA-03.md), [UX-02](../../modules/02-app-shell/UX-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/google/auth/`
- `android/app/src/main/kotlin/vn/lifeos/app/settings/google/`
- `docs/setup/GOOGLE.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Đăng ký Android OAuth client theo hướng dẫn Google hiện hành; phân biệt xác thực tài khoản với cấp quyền từng dữ liệu.
2. Dùng authorization SDK/browser chính thức được hỗ trợ trên Android; không nhúng client secret.
3. Chỉ yêu cầu gmail.readonly khi bật email và calendar read scope thích hợp khi bật lịch; không yêu cầu drive.file, spreadsheets hoặc quyền Drive.
4. Trạng thái scope/token và checkpoint tách theo account/tính năng; thiếu OAuth config có fake provider rõ nhãn.
5. Disconnect hủy worker của nguồn và xóa token cache app; vẫn giữ ledger/local task đã xác nhận.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-GOOGLE-01-01** Gmail và Calendar có consent/trạng thái riêng; ứng dụng không cần Google để quản lý tài chính.
- [ ] **RQ-GOOGLE-01-02** Không có scope/SDK/flow Google Sheets hoặc Drive chỉ để phục vụ báo cáo tài chính.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Từ chối Gmail vẫn dùng lịch Google đã cấp và tài chính local.
- [ ] Không kết nối Google vẫn nhập thu/chi, xem chart và nhận gợi ý từ dữ liệu app.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/GOOGLE-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng GOOGLE-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
