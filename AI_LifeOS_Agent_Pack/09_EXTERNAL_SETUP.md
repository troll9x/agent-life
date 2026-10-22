# Thiết lập và gate bên ngoài

Không cần tài khoản thật để dựng domain, UI, DB và adapter bằng fixture. Chỉ gate kiểm tích hợp thật đòi quyền bên ngoài. Không đưa mật khẩu/token/API secret vào tài liệu hoặc chat.

| Gate | Cần trước kiểm thật | Task sở hữu | Cách làm khi chưa có |
| --- | --- | --- | --- |
| ANDROID_BUILD | Android Studio/SDK, JDK tương thích, wrapper dependencies | FND-01 | Ghi NOT_RUN nếu chưa build được, không đoán version |
| ANDROID_DEVICE | Emulator và ít nhất một máy Android thực, model/API được ghi | PLAN-03, NOTI-04, REL-01/02 | Unit/fixture vẫn chạy; device gate NOT_RUN |
| BANK_TEMPLATE | Tên app/package xác minh, mẫu notification đã che tài khoản/người nhận | NOTI-02 | Parser fixture và UnsupportedTemplate, không tự đoán bank format |
| NOTIFICATION_ACCESS | User tự bật quyền Android, bank app có notification | NOTI-01 | Permission-denied UI, tiền mặt vẫn dùng được |
| MICROSOFT | Loại hộp thư, Entra app public client, client ID, redirect signature, user/admin consent | MS-01 | Fake source rõ nhãn, ghi EXTERNAL_BLOCKED cho live |
| GOOGLE | OAuth app và consent Gmail/Calendar tương ứng | GOOGLE-01 | Fake adapter; tài chính không cần Google hoặc Drive scope |
| HEALTH_CONNECT | Thiết bị khả dụng, permission từng record type, nguồn có dữ liệu | HEALTH-03 | Manual log vẫn hoạt động, unavailable state |
| AI_PROVIDER | Local endpoint hoặc gateway, policy dữ liệu và quota | AI-01, SAAS-04 | OFF/FAKE, không suy local Android có Ollama |
| BACKEND | DB staging, OIDC test tenant, secrets store, TLS khi network thật | SAAS-01/02/06 | Chạy local/test, không deploy production |
| BILLING | Kênh phân phối và sandbox store/provider phù hợp policy | SAAS-05 | Entitlement fixture, không thu tiền thật |
| TELEGRAM | Bot secret backend, chat liên kết và opt-in | CHANNEL-01 | Mock delivery, không tự gửi user khác |

## Microsoft cần xác minh

Outlook 2016 có thể chứa Exchange Online, Exchange nội bộ hoặc IMAP/POP. Không dùng tên client để quyết định Graph hỗ trợ. LifeOS đăng nhập riêng từng account được phép; “tất cả account cấu hình trên Outlook” trong yêu cầu trước là phạm vi mong muốn, không phải API tự nhìn thấy mọi account desktop.

Entra có thể yêu cầu admin consent, publisher verification hoặc conditional access. Không yêu cầu bỏ policy tổ chức để dùng app. Nếu bị chặn, source status phải nêu đúng. Cloud Microsoft connector sau này cần server consent flow riêng, không copy token cache từ điện thoại/laptop.

## Tài chính không có gate Google Sheet

Ngày 20/10/2026 user chốt Sheet chỉ là mẫu tham khảo công thức/chart. App không đọc/ghi/import/export Sheet, không xin drive.file hoặc spreadsheets. Không cần Google account để nhập thu/chi, vẽ chart, đối soát hoặc nhận insight quy tắc. AI remote vẫn có gate provider/consent riêng. Công thức và các lỗi tham khảo đã được ghi trong 13_FINANCE_ANALYTICS.md.

## Ghi gate

Tên gate, task/requirement liên quan, phần code đã kiểm nội bộ, bước live còn thiếu, người/thiết bị/quyền cần, trạng thái và evidence. Ghi “chưa có admin consent cho tenant test” thay vì “không hoạt động” hoặc “đã tích hợp”.
