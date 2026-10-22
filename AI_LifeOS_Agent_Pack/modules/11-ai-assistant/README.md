# AI Trợ lý AI và hành động có xác nhận

AI tóm tắt có nguồn, hạn chế dữ liệu gửi ra ngoài và có chế độ không dùng AI.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [AI-01](AI-01.md) | Cổng AI và privacy budget | [DATA-03](../../modules/01-data-security/DATA-03.md), [DATA-04](../../modules/01-data-security/DATA-04.md), [FND-02](../../modules/00-foundation/FND-02.md) | PERSONAL |
| [AI-02](AI-02.md) | Pending action và approve idempotent | [AI-01](../../modules/11-ai-assistant/AI-01.md), [DATA-01](../../modules/01-data-security/DATA-01.md) | PERSONAL |
| [AI-03](AI-03.md) | Hỏi đáp cá nhân và tóm tắt có nguồn | [AI-02](../../modules/11-ai-assistant/AI-02.md), [INSIGHT-01](../../modules/10-finance-insights/INSIGHT-01.md), [HEALTH-04](../../modules/08-health-nutrition/HEALTH-04.md), [BRIEF-03](../../modules/07-morning-briefing/BRIEF-03.md) | PERSONAL |
| [AI-04](AI-04.md) | Giới hạn on-device và chống prompt injection | [AI-03](../../modules/11-ai-assistant/AI-03.md), [MS-03](../../modules/06-microsoft/MS-03.md), [BRIEF-01](../../modules/07-morning-briefing/BRIEF-01.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
