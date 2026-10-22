# Prompt điều phối module FND

Bạn là agent thực hiện module **Nền tảng Android** trong repo AI LifeOS. Đọc AGENTS.md của repo, [hướng dẫn chung](../../06_AGENT_RULES.md) và [kiến trúc](../../02_ARCHITECTURE.md). Mục tiêu module: Repo mở được bằng Android Studio và build từ terminal; các lớp có ranh giới thống nhất.

## Đầu vào và lựa chọn task

Đọc [bảng trạng thái](../../TASK_BOARD.md) bản repo và evidence `docs/progress/`. Không suy DONE chỉ vì đã có file Kotlin. Các prerequisite bên ngoài module có thể cần: Không có.

Thứ tự các task của module sau đây đã xét dependency nội bộ; vẫn phải kiểm prerequisite cụ thể trong file task:

1. [FND-01](FND-01.md): Khởi tạo repo Kotlin và toolchain.
2. [FND-02](FND-02.md): Chuẩn hóa kiểu dữ liệu và quy tắc miền.
3. [FND-03](FND-03.md): Dựng kiểm thử và CI tối thiểu.

Trong lượt này, chọn **một task chưa IMPLEMENTED** có prerequisite code/hợp đồng đủ dùng. Đọc prompt task đó và làm đúng các bước, phạm vi, tiêu chí, test và output evidence. Nếu task chỉ còn live gate, kiểm gate có điều kiện thực hiện chưa; không code lại từ đầu. Nếu tất cả task của module đã có code nhưng gate còn thiếu, báo rõ tiến độ và các gate còn mở thay vì giả hoàn tất.

## Phạm vi và kiểm chứng

Kotlin native, local-first; mọi tài chính/chart/coaching chạy trong ứng dụng. Microsoft là nguồn công việc ưu tiên; Google chỉ Gmail/Calendar tùy chọn. Google Sheet chỉ tham khảo tài liệu. Sửa registration tối thiểu khi cần nối feature và giữ nguyên công việc có sẵn.

Không triển khai trọn nhiều nhiệm vụ vào một lượt nếu điều đó làm vượt context; hoàn tất từng task, lưu handoff khi cần. Không tự giao agent khác, không chạy các task sửa cùng schema song song. Mỗi task phải có bằng chứng riêng và trạng thái implementation/live gate riêng.

## Bàn giao cuối lượt

Task đã chọn; requirement IDs và file đổi; lệnh/test với kết quả thật; bằng chứng; gate chưa kiểm; task tiếp theo sẵn sàng. Đối chiếu [checklist tổng](../../01_PRODUCT_CHECKLIST.md) và [traceability](../../TRACEABILITY.md). Nếu toàn module đã đủ điều kiện, báo rõ danh sách evidence cho từng task thay vì chỉ nói module đã xong.
