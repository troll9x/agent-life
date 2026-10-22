# SAAS Backend và thương mại hóa

Backend tùy chọn bổ sung auth, tenant, scheduler, AI gateway và subscription; app cá nhân vẫn hoạt động độc lập.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [SAAS-01](SAAS-01.md) | Khởi tạo backend Kotlin và PostgreSQL | [FND-03](../../modules/00-foundation/FND-03.md) | SAAS |
| [SAAS-02](SAAS-02.md) | OIDC và quyền workspace | [SAAS-01](../../modules/14-saas/SAAS-01.md), [SYNC-01](../../modules/13-sync/SYNC-01.md) | SAAS |
| [SAAS-03](SAAS-03.md) | Scheduler bản tin và nguồn cloud opt-in | [SAAS-02](../../modules/14-saas/SAAS-02.md), [BRIEF-04](../../modules/07-morning-briefing/BRIEF-04.md), [SYNC-03](../../modules/13-sync/SYNC-03.md) | SAAS |
| [SAAS-04](SAAS-04.md) | AI gateway và quota theo workspace | [SAAS-02](../../modules/14-saas/SAAS-02.md), [AI-01](../../modules/11-ai-assistant/AI-01.md) | SAAS |
| [SAAS-05](SAAS-05.md) | Subscription và entitlement server | [SAAS-02](../../modules/14-saas/SAAS-02.md), [SAAS-04](../../modules/14-saas/SAAS-04.md) | SAAS |
| [SAAS-06](SAAS-06.md) | Vận hành, backup server và xóa cloud | [SAAS-03](../../modules/14-saas/SAAS-03.md), [SAAS-04](../../modules/14-saas/SAAS-04.md), [SAAS-05](../../modules/14-saas/SAAS-05.md), [SYNC-04](../../modules/13-sync/SYNC-04.md) | SAAS |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
