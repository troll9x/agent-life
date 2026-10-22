# INSIGHT Phân tích, biểu đồ và lời khuyên chi tiêu trong app

Tái hiện ý tưởng ngân sách/thực chi của mẫu tham khảo bằng dữ liệu local, không phụ thuộc Google Sheet.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [INSIGHT-01](INSIGHT-01.md) | Công thức tổng hợp tài chính nội bộ | [FIN-03](../../modules/04-finance/FIN-03.md), [FIN-10](../../modules/04-finance/FIN-10.md) | PERSONAL |
| [INSIGHT-02](INSIGHT-02.md) | Biểu đồ tài chính native và drill-down | [INSIGHT-01](../../modules/10-finance-insights/INSIGHT-01.md), [UX-02](../../modules/02-app-shell/UX-02.md) | PERSONAL |
| [INSIGHT-03](INSIGHT-03.md) | Cảnh báo chi tiêu và so sánh kỳ | [INSIGHT-01](../../modules/10-finance-insights/INSIGHT-01.md), [FIN-07](../../modules/04-finance/FIN-07.md), [PLAN-03](../../modules/03-planner/PLAN-03.md) | PERSONAL |
| [INSIGHT-04](INSIGHT-04.md) | AI cố vấn chi tiêu cá nhân có bằng chứng | [INSIGHT-02](../../modules/10-finance-insights/INSIGHT-02.md), [INSIGHT-03](../../modules/10-finance-insights/INSIGHT-03.md), [AI-02](../../modules/11-ai-assistant/AI-02.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
