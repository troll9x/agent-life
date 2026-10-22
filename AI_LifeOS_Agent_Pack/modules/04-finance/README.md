# FIN Sổ tài chính và ngân sách

Thu nhập, chi tiêu, tiền mặt và chuyển khoản có số liệu kiểm chứng được; sổ là nguồn chính.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [FIN-01](FIN-01.md) | Sổ giao dịch và số tiền chính xác | [DATA-01](../../modules/01-data-security/DATA-01.md), [FND-02](../../modules/00-foundation/FND-02.md) | PERSONAL |
| [FIN-02](FIN-02.md) | Nhập thu chi, tiền mặt và chuyển khoản | [FIN-01](../../modules/04-finance/FIN-01.md), [UX-02](../../modules/02-app-shell/UX-02.md) | PERSONAL |
| [FIN-03](FIN-03.md) | Ngân sách và báo cáo tháng | [FIN-02](../../modules/04-finance/FIN-02.md), [FIN-08](../../modules/04-finance/FIN-08.md), [FIN-09](../../modules/04-finance/FIN-09.md) | PERSONAL |
| [FIN-04](FIN-04.md) | Hàng chờ nhập và ghép bằng chứng chống trùng | [FIN-01](../../modules/04-finance/FIN-01.md) | PERSONAL |
| [FIN-05](FIN-05.md) | Mục tiêu và tài khoản tiết kiệm | [FIN-03](../../modules/04-finance/FIN-03.md) | PERSONAL |
| [FIN-06](FIN-06.md) | Theo dõi cho vay và đi vay | [FIN-03](../../modules/04-finance/FIN-03.md) | PERSONAL |
| [FIN-07](FIN-07.md) | Giao dịch định kỳ và khoản dự kiến | [FIN-02](../../modules/04-finance/FIN-02.md), [FIN-04](../../modules/04-finance/FIN-04.md), [PLAN-03](../../modules/03-planner/PLAN-03.md) | PERSONAL |
| [FIN-08](FIN-08.md) | Sửa khoản đã post bằng reversal | [FIN-02](../../modules/04-finance/FIN-02.md) | PERSONAL |
| [FIN-09](FIN-09.md) | Hoàn tiền và điều chỉnh số dư có lý do | [FIN-02](../../modules/04-finance/FIN-02.md) | PERSONAL |
| [FIN-10](FIN-10.md) | Đối soát tài khoản và chênh lệch | [FIN-04](../../modules/04-finance/FIN-04.md), [FIN-09](../../modules/04-finance/FIN-09.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
