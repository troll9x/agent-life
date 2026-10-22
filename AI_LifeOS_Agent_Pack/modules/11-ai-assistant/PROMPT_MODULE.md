# Prompt điều phối module AI

Bạn là agent thực hiện module **Trợ lý AI và hành động có xác nhận** trong repo AI LifeOS. Đọc AGENTS.md của repo, [hướng dẫn chung](../../06_AGENT_RULES.md) và [kiến trúc](../../02_ARCHITECTURE.md). Mục tiêu module: AI tóm tắt có nguồn, hạn chế dữ liệu gửi ra ngoài và có chế độ không dùng AI.

## Đầu vào và lựa chọn task

Đọc [bảng trạng thái](../../TASK_BOARD.md) bản repo và evidence `docs/progress/`. Không suy DONE chỉ vì đã có file Kotlin. Các prerequisite bên ngoài module có thể cần: [BRIEF-01](../../modules/07-morning-briefing/BRIEF-01.md), [BRIEF-03](../../modules/07-morning-briefing/BRIEF-03.md), [DATA-01](../../modules/01-data-security/DATA-01.md), [DATA-03](../../modules/01-data-security/DATA-03.md), [DATA-04](../../modules/01-data-security/DATA-04.md), [FND-02](../../modules/00-foundation/FND-02.md), [HEALTH-04](../../modules/08-health-nutrition/HEALTH-04.md), [INSIGHT-01](../../modules/10-finance-insights/INSIGHT-01.md), [MS-03](../../modules/06-microsoft/MS-03.md)

Thứ tự các task của module sau đây đã xét dependency nội bộ; vẫn phải kiểm prerequisite cụ thể trong file task:

1. [AI-01](AI-01.md): Cổng AI và privacy budget.
2. [AI-02](AI-02.md): Pending action và approve idempotent.
3. [AI-03](AI-03.md): Hỏi đáp cá nhân và tóm tắt có nguồn.
4. [AI-04](AI-04.md): Giới hạn on-device và chống prompt injection.

Trong lượt này, chọn **một task chưa IMPLEMENTED** có prerequisite code/hợp đồng đủ dùng. Đọc prompt task đó và làm đúng các bước, phạm vi, tiêu chí, test và output evidence. Nếu task chỉ còn live gate, kiểm gate có điều kiện thực hiện chưa; không code lại từ đầu. Nếu tất cả task của module đã có code nhưng gate còn thiếu, báo rõ tiến độ và các gate còn mở thay vì giả hoàn tất.

## Phạm vi và kiểm chứng

Kotlin native, local-first; mọi tài chính/chart/coaching chạy trong ứng dụng. Microsoft là nguồn công việc ưu tiên; Google chỉ Gmail/Calendar tùy chọn. Google Sheet chỉ tham khảo tài liệu. Sửa registration tối thiểu khi cần nối feature và giữ nguyên công việc có sẵn.

Không triển khai trọn nhiều nhiệm vụ vào một lượt nếu điều đó làm vượt context; hoàn tất từng task, lưu handoff khi cần. Không tự giao agent khác, không chạy các task sửa cùng schema song song. Mỗi task phải có bằng chứng riêng và trạng thái implementation/live gate riêng.

## Bàn giao cuối lượt

Task đã chọn; requirement IDs và file đổi; lệnh/test với kết quả thật; bằng chứng; gate chưa kiểm; task tiếp theo sẵn sàng. Đối chiếu [checklist tổng](../../01_PRODUCT_CHECKLIST.md) và [traceability](../../TRACEABILITY.md). Nếu toàn module đã đủ điều kiện, báo rõ danh sách evidence cho từng task thay vì chỉ nói module đã xong.
