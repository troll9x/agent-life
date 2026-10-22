# REL Nghiệm thu và phát hành

Đối chiếu toàn sản phẩm bằng bằng chứng; không nhầm tài liệu nhiệm vụ với code đã hoàn thành.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [REL-01](REL-01.md) | Nghiệm thu bản cá nhân ngoại tuyến | [UX-03](../../modules/02-app-shell/UX-03.md), [PLAN-03](../../modules/03-planner/PLAN-03.md), [FIN-05](../../modules/04-finance/FIN-05.md), [FIN-06](../../modules/04-finance/FIN-06.md), [FIN-07](../../modules/04-finance/FIN-07.md), [FIN-10](../../modules/04-finance/FIN-10.md), [INSIGHT-04](../../modules/10-finance-insights/INSIGHT-04.md), [HEALTH-04](../../modules/08-health-nutrition/HEALTH-04.md), [AI-04](../../modules/11-ai-assistant/AI-04.md), [BACKUP-03](../../modules/12-backup-export/BACKUP-03.md) | PERSONAL |
| [REL-02](REL-02.md) | Nghiệm thu tích hợp trên thiết bị và tài khoản thật | [REL-01](../../modules/16-release/REL-01.md), [NOTI-04](../../modules/05-bank-notifications/NOTI-04.md), [MS-04](../../modules/06-microsoft/MS-04.md), [PLAN-04](../../modules/03-planner/PLAN-04.md), [BRIEF-04](../../modules/07-morning-briefing/BRIEF-04.md), [GOOGLE-02](../../modules/09-google/GOOGLE-02.md), [GOOGLE-03](../../modules/09-google/GOOGLE-03.md) | CONNECTED |
| [REL-03](REL-03.md) | Nghiệm thu SaaS và bàn giao cuối | [REL-02](../../modules/16-release/REL-02.md), [SAAS-06](../../modules/14-saas/SAAS-06.md), [SYNC-04](../../modules/13-sync/SYNC-04.md) | SAAS |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
