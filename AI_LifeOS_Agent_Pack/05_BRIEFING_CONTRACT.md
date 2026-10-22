# Hợp đồng AI Morning Briefing

## Cấu hình

Giờ 09:00 hằng ngày, ZoneId Asia/Ho_Chi_Minh; tiếng Việt; email từ lần quét hoàn tất gần nhất, mặc định 24h; tối đa 50 thư mỗi lượt, batch 5–10. Chỉ tài khoản/folder được kết nối trong LifeOS. Không đọc Spam/Junk/Deleted mặc định. Người dùng trước đây đồng ý kiểm tra tất cả account trong Outlook 2016, nhưng LifeOS vẫn cần từng tài khoản đăng nhập/cấp quyền theo máy chủ thực tế.

Tin tức 24h, mở rộng 48h khi thiếu và đánh dấu khoảng thời gian. Tối đa 30 tin ứng viên, tối đa 10 tin đưa vào; chỉ nguồn/date thực đã xác minh. Không bắt buộc đủ 10 khi không có tin đạt chất lượng. Chủ đề AI/agent/local LLM/công cụ lập trình; React/Next/Node/TypeScript/Python/PHP/WordPress/database; hạ tầng/cloud/backup/bảo mật; công nghệ giáo dục; consumer tech khi nổi bật.

## Tám phần

1. Tổng quan tối đa 5 dòng: số thư đã kiểm, P1/P2, số việc cần xử lý và tin nổi bật.
2. Email cần chú ý: P1/P2 có sender, subject, ý chính, việc cần làm, deadline nếu có và lý do ưu tiên. Thống kê P1..P4, P4 không lộ chi tiết.
3. Tối đa 5 việc hôm nay: có nguồn, phân biệt được giao hay đề xuất, approve mới thành task.
4. Lịch họp và deadline: thời gian/zone, nội dung, nơi/link, chuẩn bị. Calendar lỗi phải ghi chưa đối chiếu được.
5. Tối đa 10 tin công nghệ: tiêu đề, 2–3 câu tóm tắt có căn cứ, điểm áp dụng, ngày đăng và URL nguồn.
6. Tối đa 3 công nghệ đáng thử: tên, công dụng, cách áp dụng và link chính thức; không giả là mới nếu chỉ mới phát hiện.
7. Tối đa 3 đề xuất: dựa trên email/tin đã xác minh, không hành động tự động.
8. Tình trạng thu thập: nguồn, trạng thái, time window, count đã xử lý, backlog/partial, last success và completedAt.

## Snapshot và sự thật của trạng thái

- COMPLETE chỉ khi các nguồn bắt buộc đang bật trong cấu hình đã thu thập đủ phạm vi và render xong. Tắt một nguồn chủ đích phải ghi NOT_ENABLED, không ERROR.
- PARTIAL khi một nguồn bật còn lỗi/backlog/thiếu dữ liệu. STALE nói về freshness của dữ liệu nguồn, không che thời điểm snapshot.
- Một bản tin/ngày/workspace có revision, không tạo nhiều logical bản tin cho mỗi retry.
- Mailbox 0 thư sau quét thành công khác mail không truy cập được. Lịch 0 event thành công khác Calendar bị từ chối.
- Mọi nguồn có checkpoint riêng, không dùng mail failure để ngăn cập nhật RSS đã thành công.

## Thông báo và kênh nhận

Android notification mở bản tin. Màn hình khóa mặc định chỉ hiện có bản tin. WorkManager prefetch không cam kết xong đúng 09:00; alarm có thể dùng snapshot và báo cần cập nhật. Cloud scheduler có thể chuẩn bị khi máy ngủ, nhưng mạng/OS thiết bị vẫn ảnh hưởng delivery. Máy tắt/force-stop không có cam kết nhận ngay.

Telegram/laptop là kênh tùy chọn có liên kết và opt-in riêng. Có thể chỉ gửi link/bản tóm tắt không nhạy cảm. Tác vụ AI Morning Briefing đã có trong Codex không tự trở thành worker LifeOS; việc chuyển kênh cần người dùng chọn để tránh gửi hai bản tin.

## Ví dụ fixture

Ngày 19/10/2026, 12 email đã kiểm: P1=1, P2=3, P3=6, P4=2. Có 65 thư trong vùng nhưng chỉ xử lý 50 thì count báo 50 và backlog 15, PARTIAL. Không tạo số P1..P4 cho 15 thư chưa đọc.

Email “vá hệ thống trước 17:00 ngày 19/10/2026 giờ Việt Nam” có deadline evidence. Email “xử lý sớm giúp anh” chỉ có urgency signal, deadline null. Nếu Microsoft Calendar lỗi, phần lịch ghi chưa đối chiếu; không ghi hôm nay không có họp.
