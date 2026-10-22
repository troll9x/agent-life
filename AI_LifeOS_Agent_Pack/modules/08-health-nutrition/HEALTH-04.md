# HEALTH-04 Xu hướng và gợi ý có giới hạn

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tổng hợp xu hướng 7/30 ngày từ dữ liệu thật và nêu thiếu dữ liệu.

## Nhiệm vụ cần có trước

[HEALTH-02](../../modules/08-health-nutrition/HEALTH-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/health/trends/`
- `android/app/src/main/kotlin/vn/lifeos/app/health/trends/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Tính mean/median/trend theo metric/unit/source đã chọn; thống kê số ngày có mẫu.
2. Biểu đồ không nối qua khoảng trống như có giá trị đo; range và axis có đơn vị.
3. Gợi ý thói quen dựa mục tiêu người dùng và dữ liệu, không chẩn đoán/thuốc/khẳng định quan hệ nhân quả.
4. Lời cảnh báo sức khỏe nghiêm trọng dùng nội dung được rà soát khi triển khai, hướng người dùng tìm trợ giúp y tế, không qua AI tùy ý.
5. Tạo health context tối thiểu cho AI, bị khóa bởi privacy policy.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-HEALTH-04-01** Biểu đồ và số tổng có thể đối chiếu lại số mẫu và đơn vị.
- [ ] **RQ-HEALTH-04-02** Mẫu thiếu được ghi rõ, không suy ra người dùng khỏe/không khỏe từ absence.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Chỉ có 2 đêm ngủ trong 7 ngày: hiển thị 2/7, không kết luận thói quen ổn định.
- [ ] Đổi nguồn ưu tiên không cộng cả manual lẫn imported cùng ngày.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/HEALTH-04.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng HEALTH-04 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
