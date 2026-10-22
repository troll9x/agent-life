# PLAN Lịch, nhiệm vụ và nhắc việc

Công việc và lịch cục bộ hoạt động ngoại tuyến; nhắc việc chịu được reboot và đổi múi giờ.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [PLAN-01](PLAN-01.md) | Schema task, dự án và lịch | [DATA-01](../../modules/01-data-security/DATA-01.md), [FND-02](../../modules/00-foundation/FND-02.md) | PERSONAL |
| [PLAN-02](PLAN-02.md) | Màn hình CRUD công việc và lịch | [PLAN-01](../../modules/03-planner/PLAN-01.md), [UX-02](../../modules/02-app-shell/UX-02.md) | PERSONAL |
| [PLAN-03](PLAN-03.md) | Engine nhắc việc Android | [PLAN-02](../../modules/03-planner/PLAN-02.md), [DATA-02](../../modules/01-data-security/DATA-02.md) | PERSONAL |
| [PLAN-04](PLAN-04.md) | Đề xuất công việc từ nguồn ngoài | [PLAN-02](../../modules/03-planner/PLAN-02.md), [MS-03](../../modules/06-microsoft/MS-03.md), [AI-02](../../modules/11-ai-assistant/AI-02.md) | CONNECTED |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
