# Prompt điều phối module INSIGHT

Bạn là agent thực hiện module **Phân tích, biểu đồ và lời khuyên chi tiêu trong app** trong repo AI LifeOS. Đọc AGENTS.md của repo, [hướng dẫn chung](../../06_AGENT_RULES.md) và [kiến trúc](../../02_ARCHITECTURE.md). Mục tiêu module: Tái hiện ý tưởng ngân sách/thực chi của mẫu tham khảo bằng dữ liệu local, không phụ thuộc Google Sheet.

## Đầu vào và lựa chọn task

Đọc [bảng trạng thái](../../TASK_BOARD.md) bản repo và evidence `docs/progress/`. Không suy DONE chỉ vì đã có file Kotlin. Các prerequisite bên ngoài module có thể cần: [AI-02](../../modules/11-ai-assistant/AI-02.md), [FIN-03](../../modules/04-finance/FIN-03.md), [FIN-07](../../modules/04-finance/FIN-07.md), [FIN-10](../../modules/04-finance/FIN-10.md), [PLAN-03](../../modules/03-planner/PLAN-03.md), [UX-02](../../modules/02-app-shell/UX-02.md)

Thứ tự các task của module sau đây đã xét dependency nội bộ; vẫn phải kiểm prerequisite cụ thể trong file task:

1. [INSIGHT-01](INSIGHT-01.md): Công thức tổng hợp tài chính nội bộ.
2. [INSIGHT-02](INSIGHT-02.md): Biểu đồ tài chính native và drill-down.
3. [INSIGHT-03](INSIGHT-03.md): Cảnh báo chi tiêu và so sánh kỳ.
4. [INSIGHT-04](INSIGHT-04.md): AI cố vấn chi tiêu cá nhân có bằng chứng.

Trong lượt này, chọn **một task chưa IMPLEMENTED** có prerequisite code/hợp đồng đủ dùng. Đọc prompt task đó và làm đúng các bước, phạm vi, tiêu chí, test và output evidence. Nếu task chỉ còn live gate, kiểm gate có điều kiện thực hiện chưa; không code lại từ đầu. Nếu tất cả task của module đã có code nhưng gate còn thiếu, báo rõ tiến độ và các gate còn mở thay vì giả hoàn tất.

## Phạm vi và kiểm chứng

Kotlin native, local-first; mọi tài chính/chart/coaching chạy trong ứng dụng. Microsoft là nguồn công việc ưu tiên; Google chỉ Gmail/Calendar tùy chọn. Google Sheet chỉ tham khảo tài liệu. Sửa registration tối thiểu khi cần nối feature và giữ nguyên công việc có sẵn.

Không triển khai trọn nhiều nhiệm vụ vào một lượt nếu điều đó làm vượt context; hoàn tất từng task, lưu handoff khi cần. Không tự giao agent khác, không chạy các task sửa cùng schema song song. Mỗi task phải có bằng chứng riêng và trạng thái implementation/live gate riêng.

## Bàn giao cuối lượt

Task đã chọn; requirement IDs và file đổi; lệnh/test với kết quả thật; bằng chứng; gate chưa kiểm; task tiếp theo sẵn sàng. Đối chiếu [checklist tổng](../../01_PRODUCT_CHECKLIST.md) và [traceability](../../TRACEABILITY.md). Nếu toàn module đã đủ điều kiện, báo rõ danh sách evidence cho từng task thay vì chỉ nói module đã xong.
