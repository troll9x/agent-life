# BACKUP Sao lưu, khôi phục và quyền sở hữu dữ liệu

Người dùng xuất dữ liệu được và khôi phục trên máy khác bằng mật khẩu riêng.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [BACKUP-01](BACKUP-01.md) | Định dạng sao lưu mã hóa portable | [DATA-02](../../modules/01-data-security/DATA-02.md), [DATA-04](../../modules/01-data-security/DATA-04.md), [FIN-02](../../modules/04-finance/FIN-02.md), [PLAN-02](../../modules/03-planner/PLAN-02.md), [HEALTH-02](../../modules/08-health-nutrition/HEALTH-02.md) | PERSONAL |
| [BACKUP-02](BACKUP-02.md) | Khôi phục an toàn và migration | [BACKUP-01](../../modules/12-backup-export/BACKUP-01.md) | PERSONAL |
| [BACKUP-03](BACKUP-03.md) | Xuất JSON CSV và xóa dữ liệu | [BACKUP-02](../../modules/12-backup-export/BACKUP-02.md), [FIN-03](../../modules/04-finance/FIN-03.md), [DATA-04](../../modules/01-data-security/DATA-04.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
