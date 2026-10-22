# HEALTH-02 Bữa ăn, nước và mục tiêu thói quen

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Nhật ký bữa ăn và nước, có nhắc thói quen tùy chọn.

## Nhiệm vụ cần có trước

[HEALTH-01](../../modules/08-health-nutrition/HEALTH-01.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/nutrition/`
- `android/data/src/main/kotlin/vn/lifeos/data/nutrition/`
- `android/app/src/main/kotlin/vn/lifeos/app/nutrition/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. MealEntry gồm eatenAt, mealType, mô tả, portions, calories optional và estimate flag; WaterEntry lưu mL.
2. Goal theo metric và period với source người dùng tự đặt; không mặc định kê mức calories/thuốc theo suy đoán.
3. Tổng nước và bữa ăn theo timezone hồ sơ, chỉnh sửa có cập nhật số tổng.
4. Nếu calories nhập bằng AI sau này phải đánh dấu ước tính, không giả chính xác thực phẩm chưa có khối lượng.
5. Hook nhắc uống nước/ghi bữa ăn qua reminder chung; không triển khai scheduler riêng.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-HEALTH-02-01** Ghi bữa ăn/nước/mục tiêu và tổng ngày đúng ngoại tuyến.
- [ ] **RQ-HEALTH-02-02** Giá trị ước tính và giá trị người dùng đo/nhập được phân biệt.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Hai lần 250 mL = 500 mL; xóa một lần còn 250 mL.
- [ ] Bữa ăn không calories không bị tính là 0 calories đã xác minh.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/HEALTH-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng HEALTH-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
