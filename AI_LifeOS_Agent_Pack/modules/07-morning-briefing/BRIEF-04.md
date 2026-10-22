# BRIEF-04 Lịch 09:00 và thông báo bản tin

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Chuẩn bị dữ liệu trước giờ và nhắc mở bản tin theo timezone, không hứa mạng hoàn thành đúng phút.

## Nhiệm vụ cần có trước

[BRIEF-03](../../modules/07-morning-briefing/BRIEF-03.md), [PLAN-03](../../modules/03-planner/PLAN-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [05_BRIEFING_CONTRACT.md](../../05_BRIEFING_CONTRACT.md): nội dung, giới hạn và trạng thái bản tin.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/app/src/main/kotlin/vn/lifeos/app/briefing/scheduling/`
- `android/data/src/main/kotlin/vn/lifeos/data/briefing/scheduling/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Lưu BriefingSchedule(time=09:00, zone=Asia/Ho_Chi_Minh, enabled), cho đổi/bật tắt.
2. WorkManager unique work thử prefetch theo mạng/pin; alarm chỉ hiển thị nhắc và dùng snapshot có sẵn.
3. Nếu chưa có bản mới, thông báo “Bản tin đang cập nhật” hoặc mở bản gần nhất kèm ngày; không gọi snapshot cũ là hôm nay.
4. Reschedule sau reboot/timezone/settings; coalesce local/server push bằng workspace+localDate+channel, ghi DeliveredNotification.
5. Có tùy chọn nhận trong LifeOS; lịch Codex cũ không tự được chuyển/xóa. Thiết kế hook cho CHANNEL và backend sau.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-BRIEF-04-01** Không có hai thông báo cùng bản tin/ngày/kênh khi job chạy lại.
- [ ] **RQ-BRIEF-04-02** Ngày/múi giờ và tình trạng chuẩn bị được xử lý đúng cả khi offline hoặc thiếu exact alarm permission.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] 08:59 offline, 09:00 nhắc mở snapshot 18/10 phải hiện đúng ngày cũ.
- [ ] Đổi giờ 09:00 sang 08:30 hủy nhắc cũ và tạo nhắc mới.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/BRIEF-04.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng BRIEF-04 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
