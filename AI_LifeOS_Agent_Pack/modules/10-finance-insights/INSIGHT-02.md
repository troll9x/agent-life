# INSIGHT-02 Biểu đồ tài chính native và drill-down

Mức sản phẩm: **PERSONAL**. Trạng thái lúc tạo tài liệu: **TODO**. Đây là prompt triển khai, không phải kết quả đã chạy.

## Mục tiêu

Vẽ chart ngay trong Android bằng analytics model, không nhúng trang Google Sheet.

## Nhiệm vụ cần có trước

[INSIGHT-01](../../modules/10-finance-insights/INSIGHT-01.md), [UX-02](../../modules/02-app-shell/UX-02.md)

Chỉ cần dependency đã có code/hợp đồng đúng để triển khai bằng fixture. Live gate bên ngoài chưa có không được coi là đã PASS và phải truyền tới task nghiệm thu cuối.

## Tài liệu cần đọc

- [02_ARCHITECTURE.md](../../02_ARCHITECTURE.md): kiến trúc Kotlin và ranh giới lớp.
- [06_AGENT_RULES.md](../../06_AGENT_RULES.md): quy trình làm một task và báo kết quả.
- [04_DATA_CONTRACTS.md](../../04_DATA_CONTRACTS.md): đúng entity/interface thuộc module task.
- [03_DOMAIN_RULES.md](../../03_DOMAIN_RULES.md): quy tắc tiền/thời gian/nguồn cần giữ.
- [13_FINANCE_ANALYTICS.md](../../13_FINANCE_ANALYTICS.md): công thức/chart/coaching trực tiếp trong app.

Chỉ đọc thêm implementation/test trong phạm vi liên quan. Đường dẫn code dưới đây tính từ repo root.

## Phạm vi file

- `android/app/src/main/kotlin/vn/lifeos/app/finance/charts/`
- `android/app/src/main/kotlin/vn/lifeos/app/finance/reports/`

Có thể sửa registration tối thiểu ở AppContainer/navigation/manifest/Room migrations/version catalog để nối feature; báo rõ trong evidence. Không refactor module khác hoặc đổi public contract ngoài nhu cầu task.

## Các bước thực hiện

1. Chọn thư viện Compose chart ổn định có license phù hợp hoặc Canvas đơn giản; khóa version nếu dùng dependency.
2. Hiển thị cột ngân sách/thực chi, xếp hạng nhóm chi, đường lũy kế ngày và cột thu/chi 12 tháng.
3. Donut cơ cấu chỉ dùng positive gross expense với nhãn đúng, hoặc dùng signed bar cho net values có số âm; không làm mất refund.
4. Bộ lọc kỳ/năm/currency/account/category thống nhất; chạm series mở đúng giao dịch cấu thành.
5. Có bảng số liệu thay thế, legend/axis/unit và empty/missing/partial states; font 1.3x, TalkBack và offline.

## Yêu cầu nghiệm thu của task

- [ ] **RQ-INSIGHT-02-01** Các chart chạy offline từ dữ liệu app và mỗi số có thể drill-down tới đúng danh sách giao dịch.
- [ ] **RQ-INSIGHT-02-02** Chart và bảng cùng tổng, có đơn vị/chú giải; giá trị âm/không có dữ liệu không bị vẽ sai.

Từng bước nghiệp vụ ở trên và các trường hợp dưới cũng phải được kiểm. Những requirement ID này được liên kết trong [checklist toàn sản phẩm](../../01_PRODUCT_CHECKLIST.md).

## Tình huống bắt buộc

- [ ] Đổi năm 2025/2026 không lẫn các giao dịch tháng 1.
- [ ] Tháng chỉ refund hiển thị signed bar/table phù hợp, không donut âm.

## Kiểm thử và build

Ưu tiên kiểm nhỏ nhất đánh đúng rủi ro. Dưới đây là lệnh dự kiến sau scaffold; kiểm `TOOLCHAIN.md`/`TESTING.md` trong repo để dùng command thật nếu toolchain thay tên task. Không ghi PASS cho command chưa chạy.

```powershell
Set-Location android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Nếu sửa UI, chạy smoke test/Compose test và mô tả màn hình thực tế. Nếu sửa Room, chạy migration/instrumentation trên thiết bị khi có. Nếu sửa HTTP, có fixture/mock-server cho lỗi và phân trang; cần gate thật thì theo [thiết lập](../../09_EXTERNAL_SETUP.md). Thiếu môi trường ghi NOT_RUN/EXTERNAL_BLOCKED và lý do cụ thể.

## Đầu ra bàn giao

1. Code production trong phạm vi task, test/fixture cần thiết và registration tối thiểu.
2. Evidence `docs/progress/INSIGHT-02.md` theo [mẫu](../../10_EVIDENCE_TEMPLATE.md): file đổi, lệnh thật, expected/actual, device/account type nếu có và gate còn mở.
3. Cập nhật dòng INSIGHT-02 trong TASK_BOARD bản repo. IMPLEMENTED khác VERIFIED; chỉ tick requirement khi evidence đáp ứng đúng criterion.
4. Báo kết quả ngắn, phần chưa kiểm và task tiếp theo sẵn sàng. Không tự tiếp tục task khác, deploy hoặc gửi thông tin ra ngoài chỉ vì file này có bước tích hợp.
