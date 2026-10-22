# FND Nền tảng Android

Repo mở được bằng Android Studio và build từ terminal; các lớp có ranh giới thống nhất.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [FND-01](FND-01.md) | Khởi tạo repo Kotlin và toolchain | Không có | PERSONAL |
| [FND-02](FND-02.md) | Chuẩn hóa kiểu dữ liệu và quy tắc miền | [FND-01](../../modules/00-foundation/FND-01.md) | PERSONAL |
| [FND-03](FND-03.md) | Dựng kiểm thử và CI tối thiểu | [FND-02](../../modules/00-foundation/FND-02.md) | PERSONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
