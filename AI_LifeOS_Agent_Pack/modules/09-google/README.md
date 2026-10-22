# GOOGLE Google OAuth, Gmail và Calendar

Google là tích hợp bổ sung độc lập với Microsoft; chỉ bật từng tính năng khi cần.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [GOOGLE-01](GOOGLE-01.md) | Google OAuth cho Gmail và Calendar | [DATA-03](../../modules/01-data-security/DATA-03.md), [UX-02](../../modules/02-app-shell/UX-02.md) | CONNECTED |
| [GOOGLE-02](GOOGLE-02.md) | Gmail bổ sung giao dịch ngân hàng | [GOOGLE-01](../../modules/09-google/GOOGLE-01.md), [FIN-04](../../modules/04-finance/FIN-04.md) | CONNECTED |
| [GOOGLE-03](GOOGLE-03.md) | Google Calendar chỉ đọc tùy chọn | [GOOGLE-01](../../modules/09-google/GOOGLE-01.md), [MS-04](../../modules/06-microsoft/MS-04.md) | CONNECTED |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
