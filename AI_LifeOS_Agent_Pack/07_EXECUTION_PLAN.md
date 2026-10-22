# Kế hoạch thực hiện theo dependency

Đây là một thứ tự hợp lệ cho repo mới. Mỗi dòng làm một task; không cần chạy nhiều agent đồng thời. Nếu chọn nhánh ưu tiên khác, giữ đủ prerequisite. Gate thật chưa có không ngăn làm phần fixture, nhưng chưa được nghiệm thu CONNECTED.

## Các mốc review

1. FND + DATA + UX onboarding: repo build được, DB/migration và bảo mật có nền.
2. PLAN + MS + BRIEF: bản tin công việc 09:00, đọc Microsoft và lịch có nguồn.
3. FIN + NOTI: sổ tiền mặt/chuyển khoản đúng rồi mới nhận thông báo ngân hàng.
4. INSIGHT + GOOGLE + HEALTH + AI + BACKUP: charts/coaching trong app, Gmail/Calendar tùy chọn, riêng tư và khôi phục.
5. REL-01/02: kiểm bản cá nhân và tích hợp thực tế.
6. SAAS + SYNC: chỉ bắt đầu thương mại khi local core ổn, rồi REL-03.
7. CHANNEL: làm nếu người dùng chọn Telegram/laptop, không chặn Android khi chưa chọn.

Danh sách topological dưới bảo đảm dependency; số thứ tự thư mục chỉ để tìm module, không ép làm trọn module trước tất cả module khác.

| Thứ tự | Task | Prerequisite | Mức |
| --- | --- | --- | --- |
| 01 | [FND-01](modules/00-foundation/FND-01.md) Khởi tạo repo Kotlin và toolchain | Không có | PERSONAL |
| 02 | [FND-02](modules/00-foundation/FND-02.md) Chuẩn hóa kiểu dữ liệu và quy tắc miền | FND-01 | PERSONAL |
| 03 | [FND-03](modules/00-foundation/FND-03.md) Dựng kiểm thử và CI tối thiểu | FND-02 | PERSONAL |
| 04 | [DATA-01](modules/01-data-security/DATA-01.md) Room và migration nền tảng | FND-03 | PERSONAL |
| 05 | [DATA-02](modules/01-data-security/DATA-02.md) Mã hóa DB và kho bí mật | DATA-01 | PERSONAL |
| 06 | [DATA-03](modules/01-data-security/DATA-03.md) Nền HTTP và trạng thái kết nối | DATA-01 | CONNECTED |
| 07 | [DATA-04](modules/01-data-security/DATA-04.md) Retention và xóa dữ liệu theo nguồn | DATA-02, DATA-03 | PERSONAL |
| 08 | [UX-01](modules/02-app-shell/UX-01.md) App shell Compose và navigation | DATA-01 | PERSONAL |
| 09 | [UX-02](modules/02-app-shell/UX-02.md) Onboarding hồ sơ và quyền theo tính năng | UX-01, DATA-04 | PERSONAL |
| 10 | [PLAN-01](modules/03-planner/PLAN-01.md) Schema task, dự án và lịch | DATA-01, FND-02 | PERSONAL |
| 11 | [PLAN-02](modules/03-planner/PLAN-02.md) Màn hình CRUD công việc và lịch | PLAN-01, UX-02 | PERSONAL |
| 12 | [PLAN-03](modules/03-planner/PLAN-03.md) Engine nhắc việc Android | PLAN-02, DATA-02 | PERSONAL |
| 13 | [FIN-01](modules/04-finance/FIN-01.md) Sổ giao dịch và số tiền chính xác | DATA-01, FND-02 | PERSONAL |
| 14 | [FIN-02](modules/04-finance/FIN-02.md) Nhập thu chi, tiền mặt và chuyển khoản | FIN-01, UX-02 | PERSONAL |
| 15 | [FIN-04](modules/04-finance/FIN-04.md) Hàng chờ nhập và ghép bằng chứng chống trùng | FIN-01 | PERSONAL |
| 16 | [FIN-07](modules/04-finance/FIN-07.md) Giao dịch định kỳ và khoản dự kiến | FIN-02, FIN-04, PLAN-03 | PERSONAL |
| 17 | [FIN-08](modules/04-finance/FIN-08.md) Sửa khoản đã post bằng reversal | FIN-02 | PERSONAL |
| 18 | [FIN-09](modules/04-finance/FIN-09.md) Hoàn tiền và điều chỉnh số dư có lý do | FIN-02 | PERSONAL |
| 19 | [FIN-03](modules/04-finance/FIN-03.md) Ngân sách và báo cáo tháng | FIN-02, FIN-08, FIN-09 | PERSONAL |
| 20 | [FIN-05](modules/04-finance/FIN-05.md) Mục tiêu và tài khoản tiết kiệm | FIN-03 | PERSONAL |
| 21 | [FIN-06](modules/04-finance/FIN-06.md) Theo dõi cho vay và đi vay | FIN-03 | PERSONAL |
| 22 | [FIN-10](modules/04-finance/FIN-10.md) Đối soát tài khoản và chênh lệch | FIN-04, FIN-09 | PERSONAL |
| 23 | [NOTI-01](modules/05-bank-notifications/NOTI-01.md) Cấp quyền và NotificationListenerService | DATA-02, UX-02, FIN-04 | CONNECTED |
| 24 | [NOTI-02](modules/05-bank-notifications/NOTI-02.md) Parser thông báo theo mẫu ngân hàng | NOTI-01, FND-02 | CONNECTED |
| 25 | [NOTI-03](modules/05-bank-notifications/NOTI-03.md) Lưu sự kiện lặp và phục hồi sau crash | NOTI-02, FIN-04 | CONNECTED |
| 26 | [NOTI-04](modules/05-bank-notifications/NOTI-04.md) Luồng xác nhận ngân hàng trên thiết bị | NOTI-03, FIN-02 | CONNECTED |
| 27 | [MS-01](modules/06-microsoft/MS-01.md) MSAL đa tài khoản và khả năng kết nối | DATA-03, UX-02 | CONNECTED |
| 28 | [MS-02](modules/06-microsoft/MS-02.md) Đồng bộ thư theo folder và checkpoint | MS-01 | CONNECTED |
| 29 | [MS-03](modules/06-microsoft/MS-03.md) Phân loại email và bằng chứng công việc | MS-02, FND-02 | CONNECTED |
| 30 | [MS-04](modules/06-microsoft/MS-04.md) Lịch Microsoft chỉ đọc và hợp nhất | MS-01, PLAN-02 | CONNECTED |
| 31 | [BRIEF-01](modules/07-morning-briefing/BRIEF-01.md) Nguồn tin công nghệ và lọc mới | DATA-03 | CONNECTED |
| 32 | [BRIEF-02](modules/07-morning-briefing/BRIEF-02.md) Engine tổng hợp snapshot bản tin | BRIEF-01, MS-03, MS-04, PLAN-02 | CONNECTED |
| 33 | [BRIEF-03](modules/07-morning-briefing/BRIEF-03.md) Màn hình bản tin và lịch sử | BRIEF-02, UX-02 | CONNECTED |
| 34 | [BRIEF-04](modules/07-morning-briefing/BRIEF-04.md) Lịch 09:00 và thông báo bản tin | BRIEF-03, PLAN-03 | CONNECTED |
| 35 | [HEALTH-01](modules/08-health-nutrition/HEALTH-01.md) Nhật ký và chỉ số sức khỏe thủ công | DATA-01, UX-02 | PERSONAL |
| 36 | [HEALTH-02](modules/08-health-nutrition/HEALTH-02.md) Bữa ăn, nước và mục tiêu thói quen | HEALTH-01 | PERSONAL |
| 37 | [HEALTH-03](modules/08-health-nutrition/HEALTH-03.md) Health Connect có quyền theo loại dữ liệu | HEALTH-01, DATA-04 | CONNECTED |
| 38 | [HEALTH-04](modules/08-health-nutrition/HEALTH-04.md) Xu hướng và gợi ý có giới hạn | HEALTH-02 | PERSONAL |
| 39 | [GOOGLE-01](modules/09-google/GOOGLE-01.md) Google OAuth cho Gmail và Calendar | DATA-03, UX-02 | CONNECTED |
| 40 | [GOOGLE-02](modules/09-google/GOOGLE-02.md) Gmail bổ sung giao dịch ngân hàng | GOOGLE-01, FIN-04 | CONNECTED |
| 41 | [GOOGLE-03](modules/09-google/GOOGLE-03.md) Google Calendar chỉ đọc tùy chọn | GOOGLE-01, MS-04 | CONNECTED |
| 42 | [INSIGHT-01](modules/10-finance-insights/INSIGHT-01.md) Công thức tổng hợp tài chính nội bộ | FIN-03, FIN-10 | PERSONAL |
| 43 | [INSIGHT-02](modules/10-finance-insights/INSIGHT-02.md) Biểu đồ tài chính native và drill-down | INSIGHT-01, UX-02 | PERSONAL |
| 44 | [UX-03](modules/02-app-shell/UX-03.md) Dashboard Hôm nay và tìm kiếm | UX-02, PLAN-02, INSIGHT-02, HEALTH-01, BRIEF-03 | PERSONAL |
| 45 | [INSIGHT-03](modules/10-finance-insights/INSIGHT-03.md) Cảnh báo chi tiêu và so sánh kỳ | INSIGHT-01, FIN-07, PLAN-03 | PERSONAL |
| 46 | [AI-01](modules/11-ai-assistant/AI-01.md) Cổng AI và privacy budget | DATA-03, DATA-04, FND-02 | PERSONAL |
| 47 | [AI-02](modules/11-ai-assistant/AI-02.md) Pending action và approve idempotent | AI-01, DATA-01 | PERSONAL |
| 48 | [PLAN-04](modules/03-planner/PLAN-04.md) Đề xuất công việc từ nguồn ngoài | PLAN-02, MS-03, AI-02 | CONNECTED |
| 49 | [INSIGHT-04](modules/10-finance-insights/INSIGHT-04.md) AI cố vấn chi tiêu cá nhân có bằng chứng | INSIGHT-02, INSIGHT-03, AI-02 | PERSONAL |
| 50 | [AI-03](modules/11-ai-assistant/AI-03.md) Hỏi đáp cá nhân và tóm tắt có nguồn | AI-02, INSIGHT-01, HEALTH-04, BRIEF-03 | PERSONAL |
| 51 | [AI-04](modules/11-ai-assistant/AI-04.md) Giới hạn on-device và chống prompt injection | AI-03, MS-03, BRIEF-01 | PERSONAL |
| 52 | [BACKUP-01](modules/12-backup-export/BACKUP-01.md) Định dạng sao lưu mã hóa portable | DATA-02, DATA-04, FIN-02, PLAN-02, HEALTH-02 | PERSONAL |
| 53 | [BACKUP-02](modules/12-backup-export/BACKUP-02.md) Khôi phục an toàn và migration | BACKUP-01 | PERSONAL |
| 54 | [BACKUP-03](modules/12-backup-export/BACKUP-03.md) Xuất JSON CSV và xóa dữ liệu | BACKUP-02, FIN-03, DATA-04 | PERSONAL |
| 55 | [REL-01](modules/16-release/REL-01.md) Nghiệm thu bản cá nhân ngoại tuyến | UX-03, PLAN-03, FIN-05, FIN-06, FIN-07, FIN-10, INSIGHT-04, HEALTH-04, AI-04, BACKUP-03 | PERSONAL |
| 56 | [REL-02](modules/16-release/REL-02.md) Nghiệm thu tích hợp trên thiết bị và tài khoản thật | REL-01, NOTI-04, MS-04, PLAN-04, BRIEF-04, GOOGLE-02, GOOGLE-03 | CONNECTED |
| 57 | [SYNC-01](modules/13-sync/SYNC-01.md) Hợp đồng wire và change envelopes | FND-02, FIN-01, PLAN-01 | SAAS |
| 58 | [SYNC-02](modules/13-sync/SYNC-02.md) Outbox và push/pull mobile | SYNC-01, DATA-02, DATA-03 | SAAS |
| 59 | [SAAS-01](modules/14-saas/SAAS-01.md) Khởi tạo backend Kotlin và PostgreSQL | FND-03 | SAAS |
| 60 | [SAAS-02](modules/14-saas/SAAS-02.md) OIDC và quyền workspace | SAAS-01, SYNC-01 | SAAS |
| 61 | [SYNC-03](modules/13-sync/SYNC-03.md) Server change log và tenant isolation | SYNC-01, SAAS-02 | SAAS |
| 62 | [SYNC-04](modules/13-sync/SYNC-04.md) Giải quyết xung đột nhiều thiết bị | SYNC-02, SYNC-03, FIN-08 | SAAS |
| 63 | [SAAS-03](modules/14-saas/SAAS-03.md) Scheduler bản tin và nguồn cloud opt-in | SAAS-02, BRIEF-04, SYNC-03 | SAAS |
| 64 | [SAAS-04](modules/14-saas/SAAS-04.md) AI gateway và quota theo workspace | SAAS-02, AI-01 | SAAS |
| 65 | [SAAS-05](modules/14-saas/SAAS-05.md) Subscription và entitlement server | SAAS-02, SAAS-04 | SAAS |
| 66 | [SAAS-06](modules/14-saas/SAAS-06.md) Vận hành, backup server và xóa cloud | SAAS-03, SAAS-04, SAAS-05, SYNC-04 | SAAS |
| 67 | [REL-03](modules/16-release/REL-03.md) Nghiệm thu SaaS và bàn giao cuối | REL-02, SAAS-06, SYNC-04 | SAAS |
| 68 | [CHANNEL-01](modules/15-extra-channels/CHANNEL-01.md) Telegram opt-in và chống gửi trùng | SAAS-03 | OPTIONAL |
| 69 | [CHANNEL-02](modules/15-extra-channels/CHANNEL-02.md) Kênh laptop và chọn kênh chính | SAAS-03 | OPTIONAL |

## Chuyển task khi context sắp hết

Ghi `docs/progress/HANDOFF-TASK-ID.md`: task đang làm; contract/version; file đã đổi và chưa commit; test đã chạy/kết quả; phần còn thiếu; lệnh tiếp theo; gate ngoài hệ thống. Agent mới đọc handoff, không bắt đầu lại và không đánh task đã hoàn tất khi còn việc.
