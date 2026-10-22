# HEALTH-03 Health Connect có quyền theo loại dữ liệu

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Đọc dữ liệu Health Connect khi khả dụng mà không thu thập vượt nhu cầu.

## Nhiệm vụ cần có trước

[HEALTH-01](../../modules/08-health-nutrition/HEALTH-01.md), [DATA-04](../../modules/01-data-security/DATA-04.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/healthconnect/`
- `android/app/src/main/kotlin/vn/lifeos/app/health/connect/`
- `android/app/src/main/AndroidManifest.xml`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Kiểm tra availability/API/package và hướng dẫn cập nhật/cài phù hợp; không coi mọi Android đều có Health Connect.
2. Xin read-only theo loại người dùng bật: steps, sleep, weight; chỉ thêm heart rate nếu có nhu cầu rõ và disclosure tương ứng.
3. Map provider record ID/data origin/timezone; dùng aggregate cho bước và policy nguồn ưu tiên để không cộng trùng wearable/manual.
4. Incremental đọc theo cursor/changes API được hỗ trợ, xử lý deletion và thu hồi quyền.
5. Work profile/thiếu quyền -> unavailable/partial; manual journal vẫn chạy.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-HEALTH-03-01** Từ chối/thu hồi quyền không crash và ngừng request dữ liệu bị thu hồi.
- [ ] **RQ-HEALTH-03-02** Mẫu imported có provenance, không nhân đôi steps/sleep từ nhiều thiết bị.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Cùng steps record quét 2 lần chỉ lưu một mẫu.
- [ ] Người dùng cho steps nhưng không sleep thì chỉ phần steps hoạt động.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/HEALTH-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng HEALTH-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
