# Prompt điều phối module HEALTH

Bạn là agent thực hiện module **Sức khỏe, dinh dưỡng và thói quen** trong repo AI LifeOS. Đọc AGENTS.md của repo, [hướng dẫn chung](../../06_AGENT_RULES.md) và [kiến trúc](../../02_ARCHITECTURE.md). Mục tiêu module: Nhập tay luôn dùng được; Health Connect bổ sung khi người dùng cấp quyền.

## Đầu vào và lựa chọn task

Đọc [bảng trạng thái](../../TASK_BOARD.md) bản repo và evidence `docs/progress/`. Không suy DONE chỉ vì đã có file Kotlin. Các prerequisite bên ngoài module có thể cần: [DATA-01](../../modules/01-data-security/DATA-01.md), [DATA-04](../../modules/01-data-security/DATA-04.md), [UX-02](../../modules/02-app-shell/UX-02.md)

Thứ tự các task của module sau đây đã xét dependency nội bộ; vẫn phải kiểm prerequisite cụ thể trong file task:

1. [HEALTH-01](HEALTH-01.md): Nhật ký và chỉ số sức khỏe thủ công.
2. [HEALTH-02](HEALTH-02.md): Bữa ăn, nước và mục tiêu thói quen.
3. [HEALTH-03](HEALTH-03.md): Health Connect có quyền theo loại dữ liệu.
4. [HEALTH-04](HEALTH-04.md): Xu hướng và gợi ý có giới hạn.

Trong lượt này, chọn **một task chưa IMPLEMENTED** có prerequisite code/hợp đồng đủ dùng. Đọc prompt task đó và làm đúng các bước, phạm vi, tiêu chí, test và output evidence. Nếu task chỉ còn live gate, kiểm gate có điều kiện thực hiện chưa; không code lại từ đầu. Nếu tất cả task của module đã có code nhưng gate còn thiếu, báo rõ tiến độ và các gate còn mở thay vì giả hoàn tất.

## Phạm vi và kiểm chứng

Kotlin native, local-first; mọi tài chính/chart/coaching chạy trong ứng dụng. Microsoft là nguồn công việc ưu tiên; Google chỉ Gmail/Calendar tùy chọn. Google Sheet chỉ tham khảo tài liệu. Sửa registration tối thiểu khi cần nối feature và giữ nguyên công việc có sẵn.

Không triển khai trọn nhiều nhiệm vụ vào một lượt nếu điều đó làm vượt context; hoàn tất từng task, lưu handoff khi cần. Không tự giao agent khác, không chạy các task sửa cùng schema song song. Mỗi task phải có bằng chứng riêng và trạng thái implementation/live gate riêng.

## Bàn giao cuối lượt

Task đã chọn; requirement IDs và file đổi; lệnh/test với kết quả thật; bằng chứng; gate chưa kiểm; task tiếp theo sẵn sàng. Đối chiếu [checklist tổng](../../01_PRODUCT_CHECKLIST.md) và [traceability](../../TRACEABILITY.md). Nếu toàn module đã đủ điều kiện, báo rõ danh sách evidence cho từng task thay vì chỉ nói module đã xong.
