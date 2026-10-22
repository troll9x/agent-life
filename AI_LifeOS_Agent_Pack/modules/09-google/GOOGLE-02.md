# GOOGLE-02 Gmail bổ sung giao dịch ngân hàng

Mức sản phẩm: **CONNECTED**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Quét email ngân hàng đã cho phép và ghép với notification/manual entry.

## Nhiệm vụ cần có trước

[GOOGLE-01](../../modules/09-google/GOOGLE-01.md), [FIN-04](../../modules/04-finance/FIN-04.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [09_EXTERNAL_SETUP.md](../../09_EXTERNAL_SETUP.md): gate tài khoản/quyền và fixture thay thế.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/data/src/main/kotlin/vn/lifeos/data/google/gmail/`
- `fixtures/bank-email/`
- `docs/setup/GMAIL.md`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Chọn sender/label/time-window, quét hữu hạn; list ID rồi get nội dung tối thiểu, không toàn hộp thư mặc định.
2. Lưu historyId/partial cursor theo account, xử lý history hết hạn bằng rescan hữu hạn có chống trùng.
3. Parser theo template email, không lấy số dư làm số tiền, không tin From display name là chứng thực giao dịch.
4. Tạo candidate hoặc thêm evidence vào khoản đã match; không post tự động và không sửa trạng thái mail.
5. Phone dùng polling có giới hạn/foreground sync; Pub/Sub watch chỉ được thêm ở backend có lifecycle renewal.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-GOOGLE-02-01** Email cùng giao dịch đã có không làm ghi sổ hai lần; cursor hết hạn không bỏ sót vùng đã chọn.
- [ ] **RQ-GOOGLE-02-02** Thiếu quyền/body/mẫu unsupported hiện partial/candidate cần xác nhận, không giả amount.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] historyId invalid dẫn tới bounded rescan, cùng mail không nhân đôi.
- [ ] Email lương đến sau notification confirmed được đề xuất match theo reference.

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
2. Evidence `docs/progress/GOOGLE-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng GOOGLE-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
