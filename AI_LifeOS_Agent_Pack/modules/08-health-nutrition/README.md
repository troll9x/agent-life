# HEALTH Sức khỏe, dinh dưỡng và thói quen

Nhập tay luôn dùng được; Health Connect bổ sung khi người dùng cấp quyền.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [HEALTH-01](HEALTH-01.md) | Nhật ký và chỉ số sức khỏe thủ công | [DATA-01](../../modules/01-data-security/DATA-01.md), [UX-02](../../modules/02-app-shell/UX-02.md) | PERSONAL |
| [HEALTH-02](HEALTH-02.md) | Bữa ăn, nước và mục tiêu thói quen | [HEALTH-01](../../modules/08-health-nutrition/HEALTH-01.md) | PERSONAL |
| [HEALTH-03](HEALTH-03.md) | Health Connect có quyền theo loại dữ liệu | [HEALTH-01](../../modules/08-health-nutrition/HEALTH-01.md), [DATA-04](../../modules/01-data-security/DATA-04.md) | CONNECTED |
| [HEALTH-04](HEALTH-04.md) | Xu hướng và gợi ý có giới hạn | [HEALTH-02](../../modules/08-health-nutrition/HEALTH-02.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
