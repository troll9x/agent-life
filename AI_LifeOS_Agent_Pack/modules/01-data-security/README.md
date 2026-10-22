# DATA Dữ liệu cục bộ và bảo mật

Room là nguồn dữ liệu của UI; migration, khóa và nền tảng kết nối được kiểm chứng.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [DATA-01](DATA-01.md) | Room và migration nền tảng | [FND-03](../../modules/00-foundation/FND-03.md) | PERSONAL |
| [DATA-02](DATA-02.md) | Mã hóa DB và kho bí mật | [DATA-01](../../modules/01-data-security/DATA-01.md) | PERSONAL |
| [DATA-03](DATA-03.md) | Nền HTTP và trạng thái kết nối | [DATA-01](../../modules/01-data-security/DATA-01.md) | CONNECTED |
| [DATA-04](DATA-04.md) | Retention và xóa dữ liệu theo nguồn | [DATA-02](../../modules/01-data-security/DATA-02.md), [DATA-03](../../modules/01-data-security/DATA-03.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
