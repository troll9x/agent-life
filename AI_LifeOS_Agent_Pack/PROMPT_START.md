# Prompt giao một nhiệm vụ cho agent

Sao chép đoạn dưới vào agent chạy trong repo code. Thay `FND-01` bằng ID của task muốn giao và đường dẫn bộ nếu đặt khác.

```text
Hãy triển khai nhiệm vụ FND-01 của AI LifeOS.

Bộ đặc tả nằm ở docs/agent-pack/. Đọc README.md, 06_AGENT_RULES.md
và file task có ID FND-01 trong modules/. Tuân thủ AGENTS.md của repo.

Đây là dự án Android Kotlin native + Jetpack Compose. Dùng kiến trúc,
quy tắc nghiệp vụ và hợp đồng dữ liệu trong bộ 2.1, không trộn TypeScript
hoặc schema ledger minh họa cũ. Tài chính/chart/coaching chạy trực tiếp trong app;
không xây tích hợp Google Sheet. Kiểm hiện trạng trước khi sửa.

Chỉ thực hiện task được giao. Đọc prerequisite và các file tối thiểu được
task dẫn, sửa đúng phạm vi, chạy kiểm thử/build phù hợp rồi ghi bằng chứng
tại docs/progress/FND-01.md. Cập nhật TASK_BOARD và requirement IDs tương ứng
trong bản sao checklist của repo; không đánh PASS nếu chưa kiểm.

Nếu thiếu client ID, quyền tenant, mẫu ngân hàng hoặc thiết bị, hoàn tất
phần code/fixture độc lập rồi ghi gate ngoài hệ thống còn mở. Không giả
đã gọi API thật. Không hardcode token hoặc dữ liệu cá nhân.

Trả lời cuối bằng: kết quả task, file đổi, lệnh và kết quả quan sát,
requirement IDs đã đạt, phần chưa kiểm/gate còn mở và task tiếp theo sẵn sàng.
```

File từng task tự nó cũng là prompt có thể gửi trực tiếp nếu agent đã biết đường dẫn bộ. Không gửi toàn bộ thư mục prompt vào cùng một lượt cho model nhỏ. Nếu task cần tách tiếp, yêu cầu agent lập HANDOFF có phạm vi cụ thể trước khi hết context, không tự báo hoàn thiện.
