# FND-03 Dựng kiểm thử và CI tối thiểu

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Có một bộ kiểm chứng đáng tin trước khi agent thêm tính năng.

## Nhiệm vụ cần có trước

[FND-02](../../modules/00-foundation/FND-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/*/src/test/`
- `android/*/src/androidTest/`
- `.github/workflows/android.yml`
- `docs/engineering/TESTING.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Cấu hình unit tests Kotlin, coroutine test dispatcher, Room instrumentation tests và Compose UI tests ở đúng module.
2. Tạo FakeClock, FakeIdGenerator, FakeConnectivity và fixture factory dùng dữ liệu giả.
3. CI chạy unit tests, lint và assembleDebug; instrumentation có job riêng hoặc hướng dẫn chạy thiết bị rõ ràng.
4. Tạo TESTING.md ghi lệnh thực tế cho Windows và CI; báo rõ emulator/API đã kiểm khi có.
5. Đảm bảo test demo bắt được lỗi thực tế như overflow hoặc sai kỳ tháng, không chỉ assert giá trị hằng.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FND-03-01** Một commit có lệnh build/unit/lint tái lập được, CI không cần credential bên ngoài.
- [ ] **RQ-FND-03-02** Kết quả không chạy trên thiết bị được đánh dấu NOT_RUN, không tính là PASS.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] CI chạy với repo clone mới.
- [ ] Một test cố tình fail phải khiến pipeline fail; sau đó khôi phục test đúng.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FND-03.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FND-03 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
