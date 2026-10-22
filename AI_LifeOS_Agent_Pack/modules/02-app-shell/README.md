# UX Giao diện, onboarding và cài đặt

Người dùng Việt Nam sử dụng được các module trên một ứng dụng có navigation và trạng thái rõ ràng.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [UX-01](UX-01.md) | App shell Compose và navigation | [DATA-01](../../modules/01-data-security/DATA-01.md) | PERSONAL |
| [UX-02](UX-02.md) | Onboarding hồ sơ và quyền theo tính năng | [UX-01](../../modules/02-app-shell/UX-01.md), [DATA-04](../../modules/01-data-security/DATA-04.md) | PERSONAL |
| [UX-03](UX-03.md) | Dashboard Hôm nay và tìm kiếm | [UX-02](../../modules/02-app-shell/UX-02.md), [PLAN-02](../../modules/03-planner/PLAN-02.md), [INSIGHT-02](../../modules/10-finance-insights/INSIGHT-02.md), [HEALTH-01](../../modules/08-health-nutrition/HEALTH-01.md), [BRIEF-03](../../modules/07-morning-briefing/BRIEF-03.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
