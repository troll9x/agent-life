# MS Microsoft email và lịch công việc

Tài khoản công việc và tài khoản Microsoft cá nhân được kết nối độc lập, chỉ đọc mặc định.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [MS-01](MS-01.md) | MSAL đa tài khoản và khả năng kết nối | [DATA-03](../../modules/01-data-security/DATA-03.md), [UX-02](../../modules/02-app-shell/UX-02.md) | CONNECTED |
| [MS-02](MS-02.md) | Đồng bộ thư theo folder và checkpoint | [MS-01](../../modules/06-microsoft/MS-01.md) | CONNECTED |
| [MS-03](MS-03.md) | Phân loại email và bằng chứng công việc | [MS-02](../../modules/06-microsoft/MS-02.md), [FND-02](../../modules/00-foundation/FND-02.md) | CONNECTED |
| [MS-04](MS-04.md) | Lịch Microsoft chỉ đọc và hợp nhất | [MS-01](../../modules/06-microsoft/MS-01.md), [PLAN-02](../../modules/03-planner/PLAN-02.md) | CONNECTED |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
