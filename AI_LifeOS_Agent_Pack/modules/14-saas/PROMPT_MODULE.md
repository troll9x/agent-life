# Prompt điều phối module SAAS

Bạn là agent thực hiện module **Backend và thương mại hóa** trong repo AI LifeOS. Đọc AGENTS.md của repo, [hướng dẫn chung](../../06_AGENT_RULES.md) và [kiến trúc](../../02_ARCHITECTURE.md). Mục tiêu module: Backend tùy chọn bổ sung auth, tenant, scheduler, AI gateway và subscription; app cá nhân vẫn hoạt động độc lập.

## Đầu vào và lựa chọn task

Đọc [bảng trạng thái](../../TASK_BOARD.md) bản repo và evidence `docs/progress/`. Không suy DONE chỉ vì đã có file Kotlin. Các prerequisite bên ngoài module có thể cần: [AI-01](../../modules/11-ai-assistant/AI-01.md), [BRIEF-04](../../modules/07-morning-briefing/BRIEF-04.md), [FND-03](../../modules/00-foundation/FND-03.md), [SYNC-01](../../modules/13-sync/SYNC-01.md), [SYNC-03](../../modules/13-sync/SYNC-03.md), [SYNC-04](../../modules/13-sync/SYNC-04.md)

Thứ tự các task của module sau đây đã xét dependency nội bộ; vẫn phải kiểm prerequisite cụ thể trong file task:

1. [SAAS-01](SAAS-01.md): Khởi tạo backend Kotlin và PostgreSQL.
2. [SAAS-02](SAAS-02.md): OIDC và quyền workspace.
3. [SAAS-03](SAAS-03.md): Scheduler bản tin và nguồn cloud opt-in.
4. [SAAS-04](SAAS-04.md): AI gateway và quota theo workspace.
5. [SAAS-05](SAAS-05.md): Subscription và entitlement server.
6. [SAAS-06](SAAS-06.md): Vận hành, backup server và xóa cloud.

Trong lượt này, chọn **một task chưa IMPLEMENTED** có prerequisite code/hợp đồng đủ dùng. Đọc prompt task đó và làm đúng các bước, phạm vi, tiêu chí, test và output evidence. Nếu task chỉ còn live gate, kiểm gate có điều kiện thực hiện chưa; không code lại từ đầu. Nếu tất cả task của module đã có code nhưng gate còn thiếu, báo rõ tiến độ và các gate còn mở thay vì giả hoàn tất.

## Phạm vi và kiểm chứng

Kotlin native, local-first; mọi tài chính/chart/coaching chạy trong ứng dụng. Microsoft là nguồn công việc ưu tiên; Google chỉ Gmail/Calendar tùy chọn. Google Sheet chỉ tham khảo tài liệu. Sửa registration tối thiểu khi cần nối feature và giữ nguyên công việc có sẵn.

Không triển khai trọn nhiều nhiệm vụ vào một lượt nếu điều đó làm vượt context; hoàn tất từng task, lưu handoff khi cần. Không tự giao agent khác, không chạy các task sửa cùng schema song song. Mỗi task phải có bằng chứng riêng và trạng thái implementation/live gate riêng.

## Bàn giao cuối lượt

Task đã chọn; requirement IDs và file đổi; lệnh/test với kết quả thật; bằng chứng; gate chưa kiểm; task tiếp theo sẵn sàng. Đối chiếu [checklist tổng](../../01_PRODUCT_CHECKLIST.md) và [traceability](../../TRACEABILITY.md). Nếu toàn module đã đủ điều kiện, báo rõ danh sách evidence cho từng task thay vì chỉ nói module đã xong.
