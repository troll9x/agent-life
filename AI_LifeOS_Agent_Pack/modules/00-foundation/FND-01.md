# FND-01 Khởi tạo repo Kotlin và toolchain

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tạo dự án Android native mới với ba Gradle module :app, :domain, :data. Chưa viết nghiệp vụ.

## Nhiệm vụ cần có trước

Không có; kiểm repo và toolchain trước.

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/`
- `docs/engineering/TOOLCHAIN.md`
- `.gitignore`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Kiểm tra repo hiện tại và AGENTS.md; nếu đã có dự án Android thì nối vào cấu trúc đó và ghi ánh xạ đường dẫn, không xóa code.
2. Chọn bộ Kotlin, AGP, Gradle, JDK, Compose và Room ổn định tương thích theo nguồn chính thức; khóa version catalog, không dùng dấu + hoặc latest.
3. Đặt minSdk 28; chọn compileSdk/targetSdk ổn định phù hợp điều kiện phân phối hiện tại; ghi bản đã dùng và lý do trong TOOLCHAIN.md.
4. Tạo domain Kotlin JVM, data Android library, app Compose; dependency app -> data/domain và data -> domain, không có chiều ngược.
5. Tạo Gradle wrapper đầy đủ và hướng dẫn mở thư mục android/ bằng Android Studio; không commit local.properties hoặc khóa ký.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-FND-01-01** Repo build được APK debug bằng Gradle wrapper và mở được trong Android Studio.
- [ ] **RQ-FND-01-02** Phiên bản toolchain được khóa cụ thể; domain không import Android, Room, Compose hoặc SDK nhà cung cấp.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Build sạch không cần Gmail/Microsoft/API key.
- [ ] Nếu thiếu Android SDK/JDK: ghi đường dẫn cần cấu hình và kết quả lệnh thật; không báo build đã qua.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/FND-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng FND-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
