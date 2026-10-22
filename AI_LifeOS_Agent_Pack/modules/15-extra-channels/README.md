# CHANNEL Telegram và thông báo laptop tùy chọn

Thêm kênh nhận sau Android theo lựa chọn; không tự nối dữ liệu với lịch Codex cũ.

Mỗi file dưới là một prompt riêng. Làm theo dependency của task, không chỉ theo số thư mục.

[Prompt điều phối module](PROMPT_MODULE.md): chọn một task sẵn sàng và thực hiện theo từng lượt.

| Task | Mục tiêu | Prerequisite | Mức |
| --- | --- | --- | --- |
| [CHANNEL-01](CHANNEL-01.md) | Telegram opt-in và chống gửi trùng | [SAAS-03](../../modules/14-saas/SAAS-03.md) | OPTIONAL |
| [CHANNEL-02](CHANNEL-02.md) | Kênh laptop và chọn kênh chính | [SAAS-03](../../modules/14-saas/SAAS-03.md) | OPTIONAL |

## Nghiệm thu module

Đối chiếu requirement IDs trong từng task với [checklist tổng](../../01_PRODUCT_CHECKLIST.md). Không tick module chỉ vì đã có UI hoặc fake adapter. Dữ liệu/domain, lỗi/permissions, persistence và live gate bắt buộc đều phải có evidence.
