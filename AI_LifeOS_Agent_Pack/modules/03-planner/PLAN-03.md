# PLAN-03 Engine nhắc việc Android

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Nhắc task/lịch mà không dùng timer JavaScript hoặc vòng nền chạy liên tục.

## Nhiệm vụ cần có trước

[PLAN-02](../../modules/03-planner/PLAN-02.md), [DATA-02](../../modules/01-data-security/DATA-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/app/src/main/kotlin/vn/lifeos/app/reminders/`
- `android/app/src/main/AndroidManifest.xml`
- `android/data/src/main/kotlin/vn/lifeos/data/reminders/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tạo Notification channels và xử lý quyền thông báo Android theo phiên bản.
2. Schedule one-shot AlarmManager theo occurrence; exact chỉ khi cần và quyền cho phép, không dùng USE_EXACT_ALARM như lối tắt.
3. Receiver xử lý nhanh, worker làm phần dài; snooze/complete cập nhật DB và hủy alarm đúng ID.
4. Nghe reboot/time/timezone và reschedule từ DB sau unlock; tránh báo lại hàng loạt nhắc đã hết hạn.
5. Thông báo màn hình khóa mặc định ẩn nội dung nhạy cảm và deep link tới đúng entity.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-PLAN-03-01** Nhắc/snooze/hủy còn đúng sau reboot, sửa task và đổi múi giờ trên thiết bị được kiểm.
- [ ] **RQ-PLAN-03-02** Thiếu quyền exact/notification hiển thị trạng thái hạn chế, không báo lịch nhắc chắc chắn đúng phút.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Sửa giờ task từ 09:00 sang 10:00 thì alarm cũ bị hủy.
- [ ] Complete từ notification không tạo hai lần completion.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/PLAN-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng PLAN-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
