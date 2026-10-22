# NOTI Thông báo ngân hàng Android

Nhận thông báo được phép, lọc tại máy và tạo candidate đáng kiểm tra.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [NOTI-01](NOTI-01.md) | Cấp quyền và NotificationListenerService | [DATA-02](../../modules/01-data-security/DATA-02.md), [UX-02](../../modules/02-app-shell/UX-02.md), [FIN-04](../../modules/04-finance/FIN-04.md) | CONNECTED |
| [NOTI-02](NOTI-02.md) | Parser thông báo theo mẫu ngân hàng | [NOTI-01](../../modules/05-bank-notifications/NOTI-01.md), [FND-02](../../modules/00-foundation/FND-02.md) | CONNECTED |
| [NOTI-03](NOTI-03.md) | Lưu sự kiện lặp và phục hồi sau crash | [NOTI-02](../../modules/05-bank-notifications/NOTI-02.md), [FIN-04](../../modules/04-finance/FIN-04.md) | CONNECTED |
| [NOTI-04](NOTI-04.md) | Luồng xác nhận ngân hàng trên thiết bị | [NOTI-03](../../modules/05-bank-notifications/NOTI-03.md), [FIN-02](../../modules/04-finance/FIN-02.md) | CONNECTED |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
