# AI LifeOS Bộ nhiệm vụ triển khai cho agent

Phiên bản 2.1 · 20/10/2026 · Ngôn ngữ tài liệu tiếng Việt, tên code tiếng Anh.

Bộ này chuyển yêu cầu sản phẩm đã thống nhất thành checklist nghiệm thu và prompt triển khai từng nhiệm vụ. Đích triển khai mới là **Android native bằng Kotlin và Jetpack Compose**, mở bằng Android Studio. Microsoft email/lịch công việc và bản tin 09:00 là chức năng chính. Google vẫn có Gmail và Calendar tùy chọn. Tài chính, biểu đồ và coaching chi tiêu chạy trực tiếp trong LifeOS; Google Sheet chỉ là mẫu tham khảo. Ứng dụng hoạt động local-first; SaaS là giai đoạn bổ sung.

Đây là bộ đặc tả và prompt. Chưa có tính năng ứng dụng nào được coi là đã viết hoặc kiểm thử chỉ vì có file nhiệm vụ. Mọi checkbox ban đầu đều chưa đạt.

17 module, 69 nhiệm vụ, 138 yêu cầu nghiệm thu có mã.

## Bắt đầu

1. Đọc [hướng dẫn](00_START_HERE.md) và [quyết định kiến trúc](02_ARCHITECTURE.md).
2. Dùng [checklist toàn sản phẩm](01_PRODUCT_CHECKLIST.md) để đối chiếu phạm vi cuối.
3. Chọn một nhiệm vụ sẵn sàng từ [kế hoạch thực hiện](07_EXECUTION_PLAN.md).
4. Gửi [prompt khởi động](PROMPT_START.md) kèm file nhiệm vụ cho agent. Mỗi lượt chỉ giao một task ID.
5. Yêu cầu agent ghi bằng chứng theo [mẫu nghiệm thu](10_EVIDENCE_TEMPLATE.md), cập nhật bản sao TASK_BOARD trong repo code.

Các link trong bộ dùng đường dẫn tương đối để giữ nguyên khi sao chép cả thư mục vào repo. Có thể đặt bộ tài liệu tại `docs/agent-pack/`; đường dẫn code ghi trong task vẫn tính từ **gốc repo code**, không tính từ thư mục tài liệu.

## File cần dùng

| File | Mục đích |
| --- | --- |
| 01_PRODUCT_CHECKLIST.md | Sản phẩm cuối phải làm được gì, mỗi dòng có mã để đối chiếu |
| MODULE_INDEX.md | Danh mục module và các task |
| 02_ARCHITECTURE.md | Một hướng Kotlin thống nhất; không trộn với repo React Native cũ |
| 03_DOMAIN_RULES.md | Quy tắc tiền, thời gian, nguồn dữ liệu và tác động AI |
| 04_DATA_CONTRACTS.md | Entity, trường, quan hệ và interface dùng chung |
| 14_API_CONTRACTS.md | Route SaaS, sync wire, lỗi và quy tắc idempotency |
| 05_BRIEFING_CONTRACT.md | Nội dung bản tin 09:00 kế thừa yêu cầu trước |
| 06_AGENT_RULES.md | Cách agent làm một việc nhỏ và báo kết quả trung thực |
| 07_EXECUTION_PLAN.md | Thứ tự theo dependency và các mốc release |
| 08_ACCEPTANCE_SCENARIOS.md | Kịch bản xuyên module, số liệu đầu vào và kết quả mong đợi |
| 09_EXTERNAL_SETUP.md | Tài khoản, quyền, thiết bị và phần có thể làm bằng fixture |
| TRACEABILITY.md | Mỗi yêu cầu thuộc task nào, không để rơi chức năng |
| TASK_BOARD.md | Trạng thái triển khai, kiểm thật và bằng chứng |
| 11_CHANGE_CONTROL.md | Quyền ưu tiên tài liệu và cách thay đổi hợp đồng |
| 12_SOURCES.md | Tài liệu chính thức để agent đối chiếu SDK/API |
| PRODUCT_CONTEXT.md | Nhu cầu đã chốt và phạm vi mới ngày 20/10 |
| 13_FINANCE_ANALYTICS.md | Công thức, biểu đồ và AI chi tiêu trong app, tham khảo có sửa lỗi từ mẫu |

Không cần đọc tất cả file cho mỗi task. Task nêu đúng phần phải đọc và phạm vi được sửa. Bộ có cả hướng thương mại, nhưng agent chỉ triển khai những task được giao; không tự deploy, gửi thư hoặc tạo chi phí thật.
