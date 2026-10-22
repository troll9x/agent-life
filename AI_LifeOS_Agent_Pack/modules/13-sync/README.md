# SYNC Đồng bộ nhiều thiết bị

Thay đổi được gửi lặp an toàn và xung đột được giải quyết theo loại dữ liệu.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [SYNC-01](SYNC-01.md) | Hợp đồng wire và change envelopes | [FND-02](../../modules/00-foundation/FND-02.md), [FIN-01](../../modules/04-finance/FIN-01.md), [PLAN-01](../../modules/03-planner/PLAN-01.md) | SAAS |
| [SYNC-02](SYNC-02.md) | Outbox và push/pull mobile | [SYNC-01](../../modules/13-sync/SYNC-01.md), [DATA-02](../../modules/01-data-security/DATA-02.md), [DATA-03](../../modules/01-data-security/DATA-03.md) | SAAS |
| [SYNC-03](SYNC-03.md) | Server change log và tenant isolation | [SYNC-01](../../modules/13-sync/SYNC-01.md), [SAAS-02](../../modules/14-saas/SAAS-02.md) | SAAS |
| [SYNC-04](SYNC-04.md) | Giải quyết xung đột nhiều thiết bị | [SYNC-02](../../modules/13-sync/SYNC-02.md), [SYNC-03](../../modules/13-sync/SYNC-03.md), [FIN-08](../../modules/04-finance/FIN-08.md) | SAAS |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
