# INSIGHT-04 AI cố vấn chi tiêu cá nhân có bằng chứng

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Người dùng nhận gợi ý điều chỉnh chi tiêu ngay trong app dựa trên ledger/ngân sách và mục tiêu của họ.

## Nhiệm vụ cần có trước

[INSIGHT-02](../../modules/10-finance-insights/INSIGHT-02.md), [INSIGHT-03](../../modules/10-finance-insights/INSIGHT-03.md), [AI-02](../../modules/11-ai-assistant/AI-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/domain/src/main/kotlin/vn/lifeos/domain/finance/coaching/`
- `android/data/src/main/kotlin/vn/lifeos/data/finance/coaching/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/coaching/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Gửi FinancialAnalysisContext tổng hợp tối thiểu có period, currency, metric IDs, data coverage và mục tiêu; không gửi raw notification/email ngân hàng.
2. Model giải thích category nào vượt, xu hướng và tối đa 3 hành động khả thi; từng số phải trỏ về metric đã tính, không cho LLM tự tính tổng.
3. What-if giảm một khoản X cho ra projectedSaving tính bằng domain code; nêu đó là giả định, không bảo đảm tiết kiệm thực.
4. Đề xuất đổi budget tạo PendingAction UPDATE_BUDGET với before/after + expectedVersion; user approve mới sửa. Không tự chuyển tiền/vay/đầu tư.
5. Nếu AI tắt/thiếu consent, hiển thị insight quy tắc; còn ít dữ liệu thì nêu hạn chế, không nhận xét về phẩm chất người dùng.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-INSIGHT-04-01** AI đưa lời khuyên chi tiêu trong app có số liệu/nguồn và data-quality; không cần Google Sheet.
- [ ] **RQ-INSIGHT-04-02** Số tiền giả định được tính chính xác bằng code và thay đổi ngân sách chỉ xảy ra sau xác nhận.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] User muốn giảm ăn ngoài 200000 trong 3 kỳ -> what-if 600000, không khẳng định đã tiết kiệm.
- [ ] Không có income hoặc mới ghi 2 ngày: AI không tự đặt mức chi tối đa như đã biết toàn bộ thu nhập.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/INSIGHT-04.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng INSIGHT-04 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
