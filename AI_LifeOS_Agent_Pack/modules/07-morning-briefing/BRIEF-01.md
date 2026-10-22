# BRIEF-01 Nguồn tin công nghệ và lọc mới

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Tạo NewsProvider RSS/API cấu hình được, lưu URL/ngày và sự kiện chống lặp.

## Nhiệm vụ cần có trước

[DATA-03](../../modules/01-data-security/DATA-03.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [05_BRIEFING_CONTRACT.md](../../05_BRIEFING_CONTRACT.md): nội dung, giới hạn và trạng thái bản tin.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/news/`
- `android/domain/src/main/kotlin/vn/lifeos/domain/news/`
- `fixtures/news/`
- `docs/setup/NEWS.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Seed nguồn chính thức xác minh URL hoạt động; ưu tiên AI, lập trình, hạ tầng/bảo mật, giáo dục; consumer tech chỉ tin nổi bật.
2. Lấy tối đa 30 ứng viên trong 24h; mở rộng 48h khi thiếu và đánh dấu rõ; không có ngày xác minh thì không gắn nhãn tin mới.
3. Chuẩn hóa canonical URL và event identity, phát hiện bài lặp; lưu title, date, source, excerpt cho phép, trạng thái kiểm chứng.
4. Không chỉ dựa tiêu đề để khẳng định nội dung; fetch trang khi cho phép, bảo vệ SSRF/redirect nếu dùng backend và giới hạn nội dung.
5. Không đủ nguồn thì trả partial, không bịa URL/tin hay xếp điểm chính xác giả.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-BRIEF-01-01** Tin bản tin có ngày và URL đã thu thập thật; không lặp cùng sự kiện vô ích.
- [ ] **RQ-BRIEF-01-02** Thiếu web/RSS cho ra PARTIAL/ERROR, không ghi “không có tin mới” như đã xác minh.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Article không publishedAt: loại khỏi top tin 24h.
- [ ] Hai URL syndication cùng sự kiện chỉ chọn một mục, giữ nguồn gốc.

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
2. Evidence `docs/progress/BRIEF-01.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng BRIEF-01 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
