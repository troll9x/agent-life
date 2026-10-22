# Bối cảnh yêu cầu đã chốt

Người dùng Android muốn một trợ lý quản lý cuộc sống cá nhân, dùng trước rồi mở rộng thương mại. Cần lịch, nhắc, sức khỏe, ăn uống, phân tích AI, thu nhập/chi tiêu, tiền mặt và tích hợp hệ thống công việc Microsoft.

## Tài chính

Thông báo app ngân hàng là nguồn nhập ưu tiên trên Android; email là nguồn bổ sung để tìm khoản lỡ và đối soát. Giao dịch đề xuất được người dùng review trước khi vào sổ. Tiền mặt nhập thủ công. Chuyển khoản giữa tài khoản mình, tiết kiệm, gốc nợ không được cộng nhầm thu/chi.

Sheet tham chiếu: [bảng người dùng cung cấp](https://docs.google.com/spreadsheets/d/14SysM49-37bgrgAYvmIlpnjvvA8nu9KlCmQP1O-jwa4/edit). Cấu trúc được đọc vào 19/10/2026: HDSD, T1..T12 và Tổng hợp; thu nhập A:B, budget/result D:H, chi tiết J:N. Một số nhãn nhóm giữa T1 và Tổng hợp khác nhau. Đọc lại ngày 20/10/2026 xác nhận công thức và chart titles. Không dùng số liệu mẫu như tài chính thật. User chốt ngày 20/10: chỉ tham khảo cách tính/chart, mọi tài chính và lời khuyên AI làm trong LifeOS; không kết nối hoặc ghi bản sao Sheet. Chi tiết công thức và lỗi tham khảo ở 13_FINANCE_ANALYTICS.md.

## Công việc và bản tin

Email công việc đang dùng Microsoft qua Outlook 2016 trên Windows. Cần thêm Microsoft email/lịch, không chỉ Google. Đã tìm yêu cầu cũ trong chat “Cấp quyền trợ lý briefing”: 09:00 giờ Việt Nam, tiếng Việt, phân tích email P1–P4, top việc hôm nay, họp/deadline, tin công nghệ toàn cầu xác minh nguồn/date, tối đa 3 công cụ và đề xuất, báo tình trạng nguồn. Kênh laptop/Telegram được mong muốn thêm nếu khả dụng; Android trong LifeOS là kênh chính mới.

Quyền đọc Outlook trong chat cũ không tự cấp OAuth cho app Android. Outlook 2016 không quyết định loại mailbox. Tác vụ Codex cũ và worker LifeOS là hai hệ thống, không tự liên kết hoặc tắt nhau.

## Lựa chọn công nghệ mới

Người dùng cho phép Kotlin/Android Studio nếu phù hợp. Bộ task mới chọn Kotlin native cho repo mới để giảm bridge và xử lý Android APIs trực tiếp. SaaS vẫn mở rộng được qua backend API. Đây không phải yêu cầu xây iOS ngay hoặc viết lại code đang có mà chưa xem hiện trạng.
