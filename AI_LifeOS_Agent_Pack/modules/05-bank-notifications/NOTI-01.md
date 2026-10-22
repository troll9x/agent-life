# NOTI-01 Cấp quyền và NotificationListenerService

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Nhận nội dung notification từ app ngân hàng đã chọn ngay cả khi UI đóng.

## Nhiệm vụ cần có trước

[DATA-02](../../modules/01-data-security/DATA-02.md), [UX-02](../../modules/02-app-shell/UX-02.md), [FIN-04](../../modules/04-finance/FIN-04.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/app/src/main/kotlin/vn/lifeos/app/banknotifications/`
- `android/app/src/main/AndroidManifest.xml`
- `android/data/src/main/kotlin/vn/lifeos/data/notifications/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tạo service với BIND_NOTIFICATION_LISTENER_SERVICE và intent filter đúng theo tài liệu Android.
2. Màn hình giải thích Notification access có phạm vi rộng; người dùng tự bật Cài đặt, LifeOS dùng allowlist package riêng.
3. Lọc package trước mọi persistence/log/AI; bỏ group summary, OTP-only và app không được chọn.
4. Callback không làm IO/network nặng; đưa payload tối thiểu vào hàng chờ bền với time/key/package.
5. Hiện trạng thái connected/disconnected/permission và thời điểm nhận cuối; không tuyên bố quét được lịch sử đã mất.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-NOTI-01-01** Notification app không nằm allowlist không để lại nội dung trong DB/log.
- [ ] **RQ-NOTI-01-02** Khi UI đóng, notification được phép vẫn vào queue; thu hồi quyền được nhận diện.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Thông báo fake từ package khác có nội dung giống ngân hàng bị bỏ.
- [ ] Màn hình khóa giấu amount thì candidate có missingFields, không amount=0.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/NOTI-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng NOTI-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
