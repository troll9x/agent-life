# Prompt điều phối module REL

Bạn là agent thực hiện module **Nghiệm thu và phát hành** trong repo AI LifeOS. Đọc AGENTS.md của repo, [hướng dẫn chung](../../06_AGENT_RULES.md) và [kiến trúc](../../02_ARCHITECTURE.md). Mục tiêu module: Đối chiếu toàn sản phẩm bằng bằng chứng; không nhầm tài liệu nhiệm vụ với code đã hoàn thành.

## Đầu vào và lựa chọn task

Đọc [bảng trạng thái](../../TASK_BOARD.md) bản repo và evidence `docs/progress/`. Không suy DONE chỉ vì đã có file Kotlin. Các prerequisite bên ngoài module có thể cần: [AI-04](../../modules/11-ai-assistant/AI-04.md), [BACKUP-03](../../modules/12-backup-export/BACKUP-03.md), [BRIEF-04](../../modules/07-morning-briefing/BRIEF-04.md), [FIN-05](../../modules/04-finance/FIN-05.md), [FIN-06](../../modules/04-finance/FIN-06.md), [FIN-07](../../modules/04-finance/FIN-07.md), [FIN-10](../../modules/04-finance/FIN-10.md), [GOOGLE-02](../../modules/09-google/GOOGLE-02.md), [GOOGLE-03](../../modules/09-google/GOOGLE-03.md), [HEALTH-04](../../modules/08-health-nutrition/HEALTH-04.md), [INSIGHT-04](../../modules/10-finance-insights/INSIGHT-04.md), [MS-04](../../modules/06-microsoft/MS-04.md), [NOTI-04](../../modules/05-bank-notifications/NOTI-04.md), [PLAN-03](../../modules/03-planner/PLAN-03.md), [PLAN-04](../../modules/03-planner/PLAN-04.md), [SAAS-06](../../modules/14-saas/SAAS-06.md), [SYNC-04](../../modules/13-sync/SYNC-04.md), [UX-03](../../modules/02-app-shell/UX-03.md)

Thứ tự các task của module sau đây đã xét dependency nội bộ; vẫn phải kiểm prerequisite cụ thể trong file task:

1. [REL-01](REL-01.md): Nghiệm thu bản cá nhân ngoại tuyến.
2. [REL-02](REL-02.md): Nghiệm thu tích hợp trên thiết bị và tài khoản thật.
3. [REL-03](REL-03.md): Nghiệm thu SaaS và bàn giao cuối.

Trong lượt này, chọn **một task chưa IMPLEMENTED** có prerequisite code/hợp đồng đủ dùng. Đọc prompt task đó và làm đúng các bước, phạm vi, tiêu chí, test và output evidence. Nếu task chỉ còn live gate, kiểm gate có điều kiện thực hiện chưa; không code lại từ đầu. Nếu tất cả task của module đã có code nhưng gate còn thiếu, báo rõ tiến độ và các gate còn mở thay vì giả hoàn tất.

## Phạm vi và kiểm chứng

Kotlin native, local-first; mọi tài chính/chart/coaching chạy trong ứng dụng. Microsoft là nguồn công việc ưu tiên; Google chỉ Gmail/Calendar tùy chọn. Google Sheet chỉ tham khảo tài liệu. Sửa registration tối thiểu khi cần nối feature và giữ nguyên công việc có sẵn.

Không triển khai trọn nhiều nhiệm vụ vào một lượt nếu điều đó làm vượt context; hoàn tất từng task, lưu handoff khi cần. Không tự giao agent khác, không chạy các task sửa cùng schema song song. Mỗi task phải có bằng chứng riêng và trạng thái implementation/live gate riêng.

## Bàn giao cuối lượt

Task đã chọn; requirement IDs và file đổi; lệnh/test với kết quả thật; bằng chứng; gate chưa kiểm; task tiếp theo sẵn sàng. Đối chiếu [checklist tổng](../../01_PRODUCT_CHECKLIST.md) và [traceability](../../TRACEABILITY.md). Nếu toàn module đã đủ điều kiện, báo rõ danh sách evidence cho từng task thay vì chỉ nói module đã xong.
