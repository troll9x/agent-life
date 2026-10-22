# BRIEF Bản tin công việc và công nghệ 09:00

Tạo snapshot có nguồn và phần thiếu rõ ràng, đọc được trên điện thoại, không phụ thuộc chat Codex.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [BRIEF-01](BRIEF-01.md) | Nguồn tin công nghệ và lọc mới | [DATA-03](../../modules/01-data-security/DATA-03.md) | CONNECTED |
| [BRIEF-02](BRIEF-02.md) | Engine tổng hợp snapshot bản tin | [BRIEF-01](../../modules/07-morning-briefing/BRIEF-01.md), [MS-03](../../modules/06-microsoft/MS-03.md), [MS-04](../../modules/06-microsoft/MS-04.md), [PLAN-02](../../modules/03-planner/PLAN-02.md) | CONNECTED |
| [BRIEF-03](BRIEF-03.md) | Màn hình bản tin và lịch sử | [BRIEF-02](../../modules/07-morning-briefing/BRIEF-02.md), [UX-02](../../modules/02-app-shell/UX-02.md) | CONNECTED |
| [BRIEF-04](BRIEF-04.md) | Lịch 09:00 và thông báo bản tin | [BRIEF-03](../../modules/07-morning-briefing/BRIEF-03.md), [PLAN-03](../../modules/03-planner/PLAN-03.md) | CONNECTED |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
