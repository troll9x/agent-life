# Đối chiếu yêu cầu và nhiệm vụ

Không có requirement vô chủ; mỗi yêu cầu có một task chính và evidence được lưu sau khi code.

| Requirement | Mức | Task owner | Điều kiện đạt | Evidence dự kiến |
| --- | --- | --- | --- | --- |
| RQ-FND-01-01 | PERSONAL | [FND-01](modules/00-foundation/FND-01.md) | Repo build được APK debug bằng Gradle wrapper và mở được trong Android Studio. | `docs/progress/FND-01.md` |
| RQ-FND-01-02 | PERSONAL | [FND-01](modules/00-foundation/FND-01.md) | Phiên bản toolchain được khóa cụ thể; domain không import Android, Room, Compose hoặc SDK nhà cung cấp. | `docs/progress/FND-01.md` |
| RQ-FND-02-01 | PERSONAL | [FND-02](modules/00-foundation/FND-02.md) | Money VND 1.000.000 được lưu là 1000000 Long, không có sai số dấu phẩy động. | `docs/progress/FND-02.md` |
| RQ-FND-02-02 | PERSONAL | [FND-02](modules/00-foundation/FND-02.md) | Khoảng ngày địa phương chuyển thành [startInclusive,endExclusive) UTC đúng, kể cả DST của múi giờ khác Việt Nam. | `docs/progress/FND-02.md` |
| RQ-FND-03-01 | PERSONAL | [FND-03](modules/00-foundation/FND-03.md) | Một commit có lệnh build/unit/lint tái lập được, CI không cần credential bên ngoài. | `docs/progress/FND-03.md` |
| RQ-FND-03-02 | PERSONAL | [FND-03](modules/00-foundation/FND-03.md) | Kết quả không chạy trên thiết bị được đánh dấu NOT_RUN, không tính là PASS. | `docs/progress/FND-03.md` |
| RQ-DATA-01-01 | PERSONAL | [DATA-01](modules/01-data-security/DATA-01.md) | Hồ sơ và trạng thái kết nối tồn tại sau đóng/mở tiến trình, UI quan sát qua repository. | `docs/progress/DATA-01.md` |
| RQ-DATA-01-02 | PERSONAL | [DATA-01](modules/01-data-security/DATA-01.md) | Migration giữ nguyên ID, số lượng và dữ liệu đã ghi; schema history được lưu trong repo. | `docs/progress/DATA-01.md` |
| RQ-DATA-02-01 | PERSONAL | [DATA-02](modules/01-data-security/DATA-02.md) | File DB người dùng không thể đọc bằng SQLite thường mà không có khóa. | `docs/progress/DATA-02.md` |
| RQ-DATA-02-02 | PERSONAL | [DATA-02](modules/01-data-security/DATA-02.md) | Tác vụ nền đọc được DB sau khi thiết bị đã unlock ít nhất một lần; lỗi khóa không gây xóa dữ liệu. | `docs/progress/DATA-02.md` |
| RQ-DATA-03-01 | CONNECTED | [DATA-03](modules/01-data-security/DATA-03.md) | 401/403/429/offline tạo trạng thái nguồn rõ ràng, không được biến thành danh sách rỗng thành công. | `docs/progress/DATA-03.md` |
| RQ-DATA-03-02 | CONNECTED | [DATA-03](modules/01-data-security/DATA-03.md) | Log không chứa bearer token, nội dung email hoặc thông tin tài khoản ngân hàng. | `docs/progress/DATA-03.md` |
| RQ-DATA-04-01 | PERSONAL | [DATA-04](modules/01-data-security/DATA-04.md) | Ngắt kết nối ngăn request tiếp theo của nguồn và không xóa giao dịch đã xác nhận ngoài ý muốn. | `docs/progress/DATA-04.md` |
| RQ-DATA-04-02 | PERSONAL | [DATA-04](modules/01-data-security/DATA-04.md) | Cleanup lặp nhiều lần an toàn; dữ liệu raw hết hạn không còn trên ổ. | `docs/progress/DATA-04.md` |
| RQ-UX-01-01 | PERSONAL | [UX-01](modules/02-app-shell/UX-01.md) | Đi tới được tất cả khu vực và back đúng; khu vực chưa triển khai ghi rõ trạng thái. | `docs/progress/UX-01.md` |
| RQ-UX-01-02 | PERSONAL | [UX-01](modules/02-app-shell/UX-01.md) | UI đọc được khi font tăng 1.3x, có chế độ tối và thông báo lỗi có thao tác thử lại. | `docs/progress/UX-01.md` |
| RQ-UX-02-01 | PERSONAL | [UX-02](modules/02-app-shell/UX-02.md) | Cài mới có thể vào app và nhập dữ liệu không cần Microsoft/Google hoặc quyền sức khỏe. | `docs/progress/UX-02.md` |
| RQ-UX-02-02 | PERSONAL | [UX-02](modules/02-app-shell/UX-02.md) | Cài đặt được giữ sau khởi động lại; cấp quyền bị từ chối không gây vòng hỏi lặp. | `docs/progress/UX-02.md` |
| RQ-UX-03-01 | PERSONAL | [UX-03](modules/02-app-shell/UX-03.md) | Dashboard và tìm kiếm hoạt động ngoại tuyến với dữ liệu thật từ các repository. | `docs/progress/UX-03.md` |
| RQ-UX-03-02 | PERSONAL | [UX-03](modules/02-app-shell/UX-03.md) | Tổng tài chính trên dashboard bằng báo cáo tài chính cùng kỳ, tiền tệ và bộ lọc. | `docs/progress/UX-03.md` |
| RQ-PLAN-01-01 | PERSONAL | [PLAN-01](modules/03-planner/PLAN-01.md) | Task/checklist/lịch được lưu ngoại tuyến và có migration không mất dữ liệu. | `docs/progress/PLAN-01.md` |
| RQ-PLAN-01-02 | PERSONAL | [PLAN-01](modules/03-planner/PLAN-01.md) | Sự kiện cả ngày không bị lệch sang ngày trước khi đổi múi giờ. | `docs/progress/PLAN-01.md` |
| RQ-PLAN-02-01 | PERSONAL | [PLAN-02](modules/03-planner/PLAN-02.md) | Có thể tạo/sửa/hoàn tất/checklist/lịch ngoại tuyến; thay đổi hiện trên dashboard. | `docs/progress/PLAN-02.md` |
| RQ-PLAN-02-02 | PERSONAL | [PLAN-02](modules/03-planner/PLAN-02.md) | Lịch chồng nhau có cảnh báo và lịch lặp không phát sinh occurrence trùng. | `docs/progress/PLAN-02.md` |
| RQ-PLAN-03-01 | PERSONAL | [PLAN-03](modules/03-planner/PLAN-03.md) | Nhắc/snooze/hủy còn đúng sau reboot, sửa task và đổi múi giờ trên thiết bị được kiểm. | `docs/progress/PLAN-03.md` |
| RQ-PLAN-03-02 | PERSONAL | [PLAN-03](modules/03-planner/PLAN-03.md) | Thiếu quyền exact/notification hiển thị trạng thái hạn chế, không báo lịch nhắc chắc chắn đúng phút. | `docs/progress/PLAN-03.md` |
| RQ-PLAN-04-01 | CONNECTED | [PLAN-04](modules/03-planner/PLAN-04.md) | Email chưa được xác nhận không tự tạo hoặc sửa task/lịch. | `docs/progress/PLAN-04.md` |
| RQ-PLAN-04-02 | CONNECTED | [PLAN-04](modules/03-planner/PLAN-04.md) | Approve một đề xuất hai lần chỉ tạo một task, có nguồn và deadline đã xác nhận. | `docs/progress/PLAN-04.md` |
| RQ-FIN-01-01 | PERSONAL | [FIN-01](modules/04-finance/FIN-01.md) | Số dư account bằng opening + tổng entry; chuyển khoản cập nhật hai account atomically. | `docs/progress/FIN-01.md` |
| RQ-FIN-01-02 | PERSONAL | [FIN-01](modules/04-finance/FIN-01.md) | Không có Double/Float trong tính tiền và không ghi ledger không cân đối của transfer. | `docs/progress/FIN-01.md` |
| RQ-FIN-02-01 | PERSONAL | [FIN-02](modules/04-finance/FIN-02.md) | Ghi thu/chi và tiền mặt hoạt động offline, dữ liệu còn sau đóng/mở app. | `docs/progress/FIN-02.md` |
| RQ-FIN-02-02 | PERSONAL | [FIN-02](modules/04-finance/FIN-02.md) | Chuyển khoản nội bộ và submit lặp giữ đúng số dư, không tăng income/expense. | `docs/progress/FIN-02.md` |
| RQ-FIN-03-01 | PERSONAL | [FIN-03](modules/04-finance/FIN-03.md) | Số trên dashboard, danh sách và CSV đối chiếu được cùng kỳ/currency. | `docs/progress/FIN-03.md` |
| RQ-FIN-03-02 | PERSONAL | [FIN-03](modules/04-finance/FIN-03.md) | Chuyển tiền sang tiết kiệm không tiêu ngân sách chi phí; refund giảm expense theo kỳ nhận refund. | `docs/progress/FIN-03.md` |
| RQ-FIN-04-01 | PERSONAL | [FIN-04](modules/04-finance/FIN-04.md) | Thông báo/email/nhập tay cùng khoản sau quyết định ghép chỉ làm ghi sổ một lần. | `docs/progress/FIN-04.md` |
| RQ-FIN-04-02 | PERSONAL | [FIN-04](modules/04-finance/FIN-04.md) | Giao dịch cùng tiền khác reference được giữ riêng; mơ hồ luôn chờ người dùng. | `docs/progress/FIN-04.md` |
| RQ-FIN-05-01 | PERSONAL | [FIN-05](modules/04-finance/FIN-05.md) | Mục tiêu tiết kiệm có tiến độ lấy từ ledger và dữ liệu lưu ngoại tuyến. | `docs/progress/FIN-05.md` |
| RQ-FIN-05-02 | PERSONAL | [FIN-05](modules/04-finance/FIN-05.md) | Chuyển sang tiết kiệm không tiêu ngân sách chi phí hoặc nhân đôi tổng tài sản. | `docs/progress/FIN-05.md` |
| RQ-FIN-06-01 | PERSONAL | [FIN-06](modules/04-finance/FIN-06.md) | Gốc vay/cho vay và hoàn gốc thay đúng balance/principal, không cộng thu/chi tiêu dùng. | `docs/progress/FIN-06.md` |
| RQ-FIN-06-02 | PERSONAL | [FIN-06](modules/04-finance/FIN-06.md) | Lãi/phí được thống kê riêng, principal outstanding đối chiếu được từng movement. | `docs/progress/FIN-06.md` |
| RQ-FIN-07-01 | PERSONAL | [FIN-07](modules/04-finance/FIN-07.md) | Khoản dự kiến/lặp chưa xác nhận không làm thay đổi balance hoặc thực chi. | `docs/progress/FIN-07.md` |
| RQ-FIN-07-02 | PERSONAL | [FIN-07](modules/04-finance/FIN-07.md) | Mỗi occurrence có tối đa một khoản được post sau approve, có thể ghép với bằng chứng ngân hàng. | `docs/progress/FIN-07.md` |
| RQ-FIN-08-01 | PERSONAL | [FIN-08](modules/04-finance/FIN-08.md) | Sửa khoản 500000 thành 600000 cho net expense 600000, số dư chỉ giảm thêm 100000 trong cùng kỳ. | `docs/progress/FIN-08.md` |
| RQ-FIN-08-02 | PERSONAL | [FIN-08](modules/04-finance/FIN-08.md) | Double submit/hai client cùng reverse một khoản chỉ có một reversal được chấp nhận. | `docs/progress/FIN-08.md` |
| RQ-FIN-09-01 | PERSONAL | [FIN-09](modules/04-finance/FIN-09.md) | Chi 500000, refund 100000 -> net expense 400000 và income 0 trong cùng kỳ. | `docs/progress/FIN-09.md` |
| RQ-FIN-09-02 | PERSONAL | [FIN-09](modules/04-finance/FIN-09.md) | Adjustment đổi số dư nhưng không làm AI/report coi đó là thu nhập hoặc chi phí tiêu dùng. | `docs/progress/FIN-09.md` |
| RQ-FIN-10-01 | PERSONAL | [FIN-10](modules/04-finance/FIN-10.md) | Đối soát cùng mốc thời gian ra đúng difference và không tự thay balance. | `docs/progress/FIN-10.md` |
| RQ-FIN-10-02 | PERSONAL | [FIN-10](modules/04-finance/FIN-10.md) | Trạng thái thiếu nguồn/chưa đối soát được truyền vào báo cáo và lời khuyên AI. | `docs/progress/FIN-10.md` |
| RQ-NOTI-01-01 | CONNECTED | [NOTI-01](modules/05-bank-notifications/NOTI-01.md) | Notification app không nằm allowlist không để lại nội dung trong DB/log. | `docs/progress/NOTI-01.md` |
| RQ-NOTI-01-02 | CONNECTED | [NOTI-01](modules/05-bank-notifications/NOTI-01.md) | Khi UI đóng, notification được phép vẫn vào queue; thu hồi quyền được nhận diện. | `docs/progress/NOTI-01.md` |
| RQ-NOTI-02-01 | CONNECTED | [NOTI-02](modules/05-bank-notifications/NOTI-02.md) | Template đã hỗ trợ trích đúng amount của giao dịch, không lấy nhầm số dư. | `docs/progress/NOTI-02.md` |
| RQ-NOTI-02-02 | CONNECTED | [NOTI-02](modules/05-bank-notifications/NOTI-02.md) | Mẫu thiếu nội dung hoặc không rõ tiền vào/ra luôn chờ xác nhận. | `docs/progress/NOTI-02.md` |
| RQ-NOTI-03-01 | CONNECTED | [NOTI-03](modules/05-bank-notifications/NOTI-03.md) | Replay cùng event sau crash không tạo thêm candidate hoặc ledger. | `docs/progress/NOTI-03.md` |
| RQ-NOTI-03-02 | CONNECTED | [NOTI-03](modules/05-bank-notifications/NOTI-03.md) | Cùng notification key nhưng giao dịch mới có ref khác vẫn được nhận là khoản mới. | `docs/progress/NOTI-03.md` |
| RQ-NOTI-04-01 | CONNECTED | [NOTI-04](modules/05-bank-notifications/NOTI-04.md) | Candidate xác nhận một lần xuất hiện trên sổ và báo cáo đúng một lần. | `docs/progress/NOTI-04.md` |
| RQ-NOTI-04-02 | CONNECTED | [NOTI-04](modules/05-bank-notifications/NOTI-04.md) | Có bằng chứng kiểm thử thiết bị hoặc ghi EXTERNAL_BLOCKED nếu chưa có thiết bị/mẫu thật. | `docs/progress/NOTI-04.md` |
| RQ-MS-01-01 | CONNECTED | [MS-01](modules/06-microsoft/MS-01.md) | Microsoft A/B có connection và token đúng account, disconnect một account không ảnh hưởng account còn lại. | `docs/progress/MS-01.md` |
| RQ-MS-01-02 | CONNECTED | [MS-01](modules/06-microsoft/MS-01.md) | Tổ chức từ chối quyền/Exchange không được hỗ trợ hiển thị lý do, không vòng login vô hạn. | `docs/progress/MS-01.md` |
| RQ-MS-02-01 | CONNECTED | [MS-02](modules/06-microsoft/MS-02.md) | Quét bị ngắt giữa trang không bỏ thư, lần sau tiếp tục và không nhân đôi signal. | `docs/progress/MS-02.md` |
| RQ-MS-02-02 | CONNECTED | [MS-02](modules/06-microsoft/MS-02.md) | Đọc thư không đánh dấu đã đọc, di chuyển, xóa, gửi hoặc mở attachment. | `docs/progress/MS-02.md` |
| RQ-MS-03-01 | CONNECTED | [MS-03](modules/06-microsoft/MS-03.md) | Mỗi P1/P2 có lý do, trích dẫn nguồn và liên kết mở email; không bịa deadline. | `docs/progress/MS-03.md` |
| RQ-MS-03-02 | CONNECTED | [MS-03](modules/06-microsoft/MS-03.md) | Các thư trong cùng trao đổi không tạo nhiều bản sao cùng một việc. | `docs/progress/MS-03.md` |
| RQ-MS-04-01 | CONNECTED | [MS-04](modules/06-microsoft/MS-04.md) | Họp Microsoft, all-day và hủy lịch được phản ánh đúng trong lịch hợp nhất. | `docs/progress/MS-04.md` |
| RQ-MS-04-02 | CONNECTED | [MS-04](modules/06-microsoft/MS-04.md) | Đọc lịch không tự tạo/sửa/accept invite; calendar bị chặn hiện trạng thái riêng. | `docs/progress/MS-04.md` |
| RQ-BRIEF-01-01 | CONNECTED | [BRIEF-01](modules/07-morning-briefing/BRIEF-01.md) | Tin bản tin có ngày và URL đã thu thập thật; không lặp cùng sự kiện vô ích. | `docs/progress/BRIEF-01.md` |
| RQ-BRIEF-01-02 | CONNECTED | [BRIEF-01](modules/07-morning-briefing/BRIEF-01.md) | Thiếu web/RSS cho ra PARTIAL/ERROR, không ghi “không có tin mới” như đã xác minh. | `docs/progress/BRIEF-01.md` |
| RQ-BRIEF-02-01 | CONNECTED | [BRIEF-02](modules/07-morning-briefing/BRIEF-02.md) | Snapshot đủ 8 phần hoặc ghi rõ phần thiếu; tổng quan và số đếm khớp dữ liệu nguồn. | `docs/progress/BRIEF-02.md` |
| RQ-BRIEF-02-02 | CONNECTED | [BRIEF-02](modules/07-morning-briefing/BRIEF-02.md) | Bản tin không tự gửi email, đổi trạng thái thư hoặc tạo task/lịch. | `docs/progress/BRIEF-02.md` |
| RQ-BRIEF-03-01 | CONNECTED | [BRIEF-03](modules/07-morning-briefing/BRIEF-03.md) | Có thể đọc bản tin và lịch sử ngoại tuyến; nguồn cũ/thiếu được thấy rõ. | `docs/progress/BRIEF-03.md` |
| RQ-BRIEF-03-02 | CONNECTED | [BRIEF-03](modules/07-morning-briefing/BRIEF-03.md) | Bấm nguồn mở đúng email/event/trang đã liên kết hoặc báo không khả dụng. | `docs/progress/BRIEF-03.md` |
| RQ-BRIEF-04-01 | CONNECTED | [BRIEF-04](modules/07-morning-briefing/BRIEF-04.md) | Không có hai thông báo cùng bản tin/ngày/kênh khi job chạy lại. | `docs/progress/BRIEF-04.md` |
| RQ-BRIEF-04-02 | CONNECTED | [BRIEF-04](modules/07-morning-briefing/BRIEF-04.md) | Ngày/múi giờ và tình trạng chuẩn bị được xử lý đúng cả khi offline hoặc thiếu exact alarm permission. | `docs/progress/BRIEF-04.md` |
| RQ-HEALTH-01-01 | PERSONAL | [HEALTH-01](modules/08-health-nutrition/HEALTH-01.md) | Nhập, sửa, xem lịch sử sức khỏe chạy không mạng và giữ đúng đơn vị. | `docs/progress/HEALTH-01.md` |
| RQ-HEALTH-01-02 | PERSONAL | [HEALTH-01](modules/08-health-nutrition/HEALTH-01.md) | Không có dữ liệu hiển thị thiếu dữ liệu, không dùng số demo làm thống kê. | `docs/progress/HEALTH-01.md` |
| RQ-HEALTH-02-01 | PERSONAL | [HEALTH-02](modules/08-health-nutrition/HEALTH-02.md) | Ghi bữa ăn/nước/mục tiêu và tổng ngày đúng ngoại tuyến. | `docs/progress/HEALTH-02.md` |
| RQ-HEALTH-02-02 | PERSONAL | [HEALTH-02](modules/08-health-nutrition/HEALTH-02.md) | Giá trị ước tính và giá trị người dùng đo/nhập được phân biệt. | `docs/progress/HEALTH-02.md` |
| RQ-HEALTH-03-01 | CONNECTED | [HEALTH-03](modules/08-health-nutrition/HEALTH-03.md) | Từ chối/thu hồi quyền không crash và ngừng request dữ liệu bị thu hồi. | `docs/progress/HEALTH-03.md` |
| RQ-HEALTH-03-02 | CONNECTED | [HEALTH-03](modules/08-health-nutrition/HEALTH-03.md) | Mẫu imported có provenance, không nhân đôi steps/sleep từ nhiều thiết bị. | `docs/progress/HEALTH-03.md` |
| RQ-HEALTH-04-01 | PERSONAL | [HEALTH-04](modules/08-health-nutrition/HEALTH-04.md) | Biểu đồ và số tổng có thể đối chiếu lại số mẫu và đơn vị. | `docs/progress/HEALTH-04.md` |
| RQ-HEALTH-04-02 | PERSONAL | [HEALTH-04](modules/08-health-nutrition/HEALTH-04.md) | Mẫu thiếu được ghi rõ, không suy ra người dùng khỏe/không khỏe từ absence. | `docs/progress/HEALTH-04.md` |
| RQ-GOOGLE-01-01 | CONNECTED | [GOOGLE-01](modules/09-google/GOOGLE-01.md) | Gmail và Calendar có consent/trạng thái riêng; ứng dụng không cần Google để quản lý tài chính. | `docs/progress/GOOGLE-01.md` |
| RQ-GOOGLE-01-02 | CONNECTED | [GOOGLE-01](modules/09-google/GOOGLE-01.md) | Không có scope/SDK/flow Google Sheets hoặc Drive chỉ để phục vụ báo cáo tài chính. | `docs/progress/GOOGLE-01.md` |
| RQ-GOOGLE-02-01 | CONNECTED | [GOOGLE-02](modules/09-google/GOOGLE-02.md) | Email cùng giao dịch đã có không làm ghi sổ hai lần; cursor hết hạn không bỏ sót vùng đã chọn. | `docs/progress/GOOGLE-02.md` |
| RQ-GOOGLE-02-02 | CONNECTED | [GOOGLE-02](modules/09-google/GOOGLE-02.md) | Thiếu quyền/body/mẫu unsupported hiện partial/candidate cần xác nhận, không giả amount. | `docs/progress/GOOGLE-02.md` |
| RQ-GOOGLE-03-01 | CONNECTED | [GOOGLE-03](modules/09-google/GOOGLE-03.md) | Lịch Google hiển thị cùng Microsoft/local, giữ nguồn và không làm sai all-day. | `docs/progress/GOOGLE-03.md` |
| RQ-GOOGLE-03-02 | CONNECTED | [GOOGLE-03](modules/09-google/GOOGLE-03.md) | Thu hồi quyền Google không làm mất lịch cục bộ hoặc kết nối Microsoft. | `docs/progress/GOOGLE-03.md` |
| RQ-INSIGHT-01-01 | PERSONAL | [INSIGHT-01](modules/10-finance-insights/INSIGHT-01.md) | Report/chart/AI cùng dùng một kết quả tính từ ledger; số dư thực và ngân sách còn lại được phân biệt. | `docs/progress/INSIGHT-01.md` |
| RQ-INSIGHT-01-02 | PERSONAL | [INSIGHT-01](modules/10-finance-insights/INSIGHT-01.md) | Tổng năm đủ 12 tháng, chia 0/thiếu tháng/refund âm xử lý rõ và không cần Google account. | `docs/progress/INSIGHT-01.md` |
| RQ-INSIGHT-02-01 | PERSONAL | [INSIGHT-02](modules/10-finance-insights/INSIGHT-02.md) | Các chart chạy offline từ dữ liệu app và mỗi số có thể drill-down tới đúng danh sách giao dịch. | `docs/progress/INSIGHT-02.md` |
| RQ-INSIGHT-02-02 | PERSONAL | [INSIGHT-02](modules/10-finance-insights/INSIGHT-02.md) | Chart và bảng cùng tổng, có đơn vị/chú giải; giá trị âm/không có dữ liệu không bị vẽ sai. | `docs/progress/INSIGHT-02.md` |
| RQ-INSIGHT-03-01 | PERSONAL | [INSIGHT-03](modules/10-finance-insights/INSIGHT-03.md) | Cảnh báo vượt ngân sách/biến động có số liệu và khoảng thời gian đối chiếu rõ. | `docs/progress/INSIGHT-03.md` |
| RQ-INSIGHT-03-02 | PERSONAL | [INSIGHT-03](modules/10-finance-insights/INSIGHT-03.md) | Ước tính chi cuối kỳ được gắn giả định/thiếu dữ liệu và không được trình bày như số chắc chắn. | `docs/progress/INSIGHT-03.md` |
| RQ-INSIGHT-04-01 | PERSONAL | [INSIGHT-04](modules/10-finance-insights/INSIGHT-04.md) | AI đưa lời khuyên chi tiêu trong app có số liệu/nguồn và data-quality; không cần Google Sheet. | `docs/progress/INSIGHT-04.md` |
| RQ-INSIGHT-04-02 | PERSONAL | [INSIGHT-04](modules/10-finance-insights/INSIGHT-04.md) | Số tiền giả định được tính chính xác bằng code và thay đổi ngân sách chỉ xảy ra sau xác nhận. | `docs/progress/INSIGHT-04.md` |
| RQ-AI-01-01 | PERSONAL | [AI-01](modules/11-ai-assistant/AI-01.md) | Tắt AI không phá CRUD/báo cáo/bản tin quy tắc; remote không được gọi khi thiếu consent. | `docs/progress/AI-01.md` |
| RQ-AI-01-02 | PERSONAL | [AI-01](modules/11-ai-assistant/AI-01.md) | Response sai schema hoặc không có evidence không được thực thi hành động. | `docs/progress/AI-01.md` |
| RQ-AI-02-01 | PERSONAL | [AI-02](modules/11-ai-assistant/AI-02.md) | AI chưa được approve không thay đổi dữ liệu quan trọng. | `docs/progress/AI-02.md` |
| RQ-AI-02-02 | PERSONAL | [AI-02](modules/11-ai-assistant/AI-02.md) | Double approve hoặc replay action không tạo thêm task/giao dịch. | `docs/progress/AI-02.md` |
| RQ-AI-03-01 | PERSONAL | [AI-03](modules/11-ai-assistant/AI-03.md) | Câu trả lời về tổng chi khớp báo cáo cùng kỳ/currency và có link nguồn. | `docs/progress/AI-03.md` |
| RQ-AI-03-02 | PERSONAL | [AI-03](modules/11-ai-assistant/AI-03.md) | Câu hỏi ngoài dữ liệu không được trả bằng thông tin cá nhân bịa đặt. | `docs/progress/AI-03.md` |
| RQ-AI-04-01 | PERSONAL | [AI-04](modules/11-ai-assistant/AI-04.md) | Nội dung email/web không gây gọi tool ngoài allowlist hoặc lộ bí mật. | `docs/progress/AI-04.md` |
| RQ-AI-04-02 | PERSONAL | [AI-04](modules/11-ai-assistant/AI-04.md) | Thiếu local/remote model vẫn có fallback và trạng thái rõ, không treo app. | `docs/progress/AI-04.md` |
| RQ-BACKUP-01-01 | PERSONAL | [BACKUP-01](modules/12-backup-export/BACKUP-01.md) | Backup mã hóa không đọc được plaintext và có thể giải bằng passphrase trên installation mới. | `docs/progress/BACKUP-01.md` |
| RQ-BACKUP-01-02 | PERSONAL | [BACKUP-01](modules/12-backup-export/BACKUP-01.md) | Snapshot khôi phục giữ đúng ID, balance và task/health, không mang token OAuth theo. | `docs/progress/BACKUP-01.md` |
| RQ-BACKUP-02-01 | PERSONAL | [BACKUP-02](modules/12-backup-export/BACKUP-02.md) | Restore sang installation khác đọc được dữ liệu bằng khóa mới, không cần khóa máy cũ. | `docs/progress/BACKUP-02.md` |
| RQ-BACKUP-02-02 | PERSONAL | [BACKUP-02](modules/12-backup-export/BACKUP-02.md) | Restore file hỏng/phiên bản chưa hỗ trợ không làm thay đổi workspace hiện tại. | `docs/progress/BACKUP-02.md` |
| RQ-BACKUP-03-01 | PERSONAL | [BACKUP-03](modules/12-backup-export/BACKUP-03.md) | JSON/CSV mở được, số tiền/date/currency đối chiếu đúng và không rò token. | `docs/progress/BACKUP-03.md` |
| RQ-BACKUP-03-02 | PERSONAL | [BACKUP-03](modules/12-backup-export/BACKUP-03.md) | Xóa local xóa dữ liệu được chọn, không xóa tệp hoặc workspace khác. | `docs/progress/BACKUP-03.md` |
| RQ-SYNC-01-01 | SAAS | [SYNC-01](modules/13-sync/SYNC-01.md) | Client/server có một wire schema versioned, money không mất chính xác khi qua JSON. | `docs/progress/SYNC-01.md` |
| RQ-SYNC-01-02 | SAAS | [SYNC-01](modules/13-sync/SYNC-01.md) | Duplicate/replay/conflict/rejected có response được định nghĩa trước khi code network sync. | `docs/progress/SYNC-01.md` |
| RQ-SYNC-02-01 | SAAS | [SYNC-02](modules/13-sync/SYNC-02.md) | Thay đổi ngoại tuyến vẫn tồn tại và gửi được sau restart/reconnect, không nhân đôi. | `docs/progress/SYNC-02.md` |
| RQ-SYNC-02-02 | SAAS | [SYNC-02](modules/13-sync/SYNC-02.md) | Cursor chỉ tiến sau apply thành công, không làm mất trang khi crash. | `docs/progress/SYNC-02.md` |
| RQ-SYNC-03-01 | SAAS | [SYNC-03](modules/13-sync/SYNC-03.md) | Push lặp chỉ tác động một lần và pull không chứa bản ghi workspace khác. | `docs/progress/SYNC-03.md` |
| RQ-SYNC-03-02 | SAAS | [SYNC-03](modules/13-sync/SYNC-03.md) | Financial aggregate không bị tách hoặc mất entry khi sync. | `docs/progress/SYNC-03.md` |
| RQ-SYNC-04-01 | SAAS | [SYNC-04](modules/13-sync/SYNC-04.md) | Hai thiết bị sửa một khoản không tạo hai sổ thu/chi hoặc hai reversal. | `docs/progress/SYNC-04.md` |
| RQ-SYNC-04-02 | SAAS | [SYNC-04](modules/13-sync/SYNC-04.md) | Xung đột hiện tới người dùng và giải quyết xong có thể sync tiếp. | `docs/progress/SYNC-04.md` |
| RQ-SAAS-01-01 | SAAS | [SAAS-01](modules/14-saas/SAAS-01.md) | Backend khởi động local, migration chạy được trên DB sạch và readiness báo đúng tình trạng DB. | `docs/progress/SAAS-01.md` |
| RQ-SAAS-01-02 | SAAS | [SAAS-01](modules/14-saas/SAAS-01.md) | Thiếu env bắt buộc fail rõ; không tự dùng credential production hoặc dev secret ở production. | `docs/progress/SAAS-01.md` |
| RQ-SAAS-02-01 | SAAS | [SAAS-02](modules/14-saas/SAAS-02.md) | User không đọc/ghi workspace khác bằng thay ID ở URL, JSON hoặc cursor. | `docs/progress/SAAS-02.md` |
| RQ-SAAS-02-02 | SAAS | [SAAS-02](modules/14-saas/SAAS-02.md) | Token hết hạn/sai issuer/audience/signature bị từ chối; app local vẫn dùng khi SaaS logout. | `docs/progress/SAAS-02.md` |
| RQ-SAAS-03-01 | SAAS | [SAAS-03](modules/14-saas/SAAS-03.md) | Một ngày/workspace có một logical briefing, chạy lại không nhân đôi notification. | `docs/progress/SAAS-03.md` |
| RQ-SAAS-03-02 | SAAS | [SAAS-03](modules/14-saas/SAAS-03.md) | Không bật cloud consent thì backend không đọc email/sức khỏe hoặc nhận token nguồn đó. | `docs/progress/SAAS-03.md` |
| RQ-SAAS-04-01 | SAAS | [SAAS-04](modules/14-saas/SAAS-04.md) | APK không cần chứa provider secret; vượt quota không gọi model tiếp. | `docs/progress/SAAS-04.md` |
| RQ-SAAS-04-02 | SAAS | [SAAS-04](modules/14-saas/SAAS-04.md) | Request AI không được phép về email công việc bị từ chối trước khi gửi provider. | `docs/progress/SAAS-04.md` |
| RQ-SAAS-05-01 | SAAS | [SAAS-05](modules/14-saas/SAAS-05.md) | Entitlement thay đổi theo giao dịch đã xác minh, duplicate webhook không cộng gói hai lần. | `docs/progress/SAAS-05.md` |
| RQ-SAAS-05-02 | SAAS | [SAAS-05](modules/14-saas/SAAS-05.md) | Hết gói vẫn xem/xuất dữ liệu và dùng local core theo feature matrix đã công bố. | `docs/progress/SAAS-05.md` |
| RQ-SAAS-06-01 | SAAS | [SAAS-06](modules/14-saas/SAAS-06.md) | Có restore drill thực tế ở staging và deletion job không ảnh hưởng tenant khác. | `docs/progress/SAAS-06.md` |
| RQ-SAAS-06-02 | SAAS | [SAAS-06](modules/14-saas/SAAS-06.md) | Runbook nêu deployment/release gates còn thiếu; chưa deploy production không đánh đã triển khai. | `docs/progress/SAAS-06.md` |
| RQ-CHANNEL-01-01 | OPTIONAL | [CHANNEL-01](modules/15-extra-channels/CHANNEL-01.md) | Không gửi Telegram khi chưa có opt-in và chat đã xác minh. | `docs/progress/CHANNEL-01.md` |
| RQ-CHANNEL-01-02 | OPTIONAL | [CHANNEL-01](modules/15-extra-channels/CHANNEL-01.md) | Một briefing/ngày không bị gửi nhiều lần do worker retry. | `docs/progress/CHANNEL-01.md` |
| RQ-CHANNEL-02-01 | OPTIONAL | [CHANNEL-02](modules/15-extra-channels/CHANNEL-02.md) | Laptop có thể xem cùng briefing snapshot của tài khoản đã xác thực. | `docs/progress/CHANNEL-02.md` |
| RQ-CHANNEL-02-02 | OPTIONAL | [CHANNEL-02](modules/15-extra-channels/CHANNEL-02.md) | Người dùng quản lý được kênh nhận và hiểu khả năng trùng với tác vụ Codex cũ. | `docs/progress/CHANNEL-02.md` |
| RQ-REL-01-01 | PERSONAL | [REL-01](modules/16-release/REL-01.md) | Tất cả requirement PERSONAL có bằng chứng hoặc trạng thái chưa đạt rõ ràng. | `docs/progress/REL-01.md` |
| RQ-REL-01-02 | PERSONAL | [REL-01](modules/16-release/REL-01.md) | Có APK cài được và backup/restore trên installation khác đã kiểm; chưa kiểm thì gate còn mở. | `docs/progress/REL-01.md` |
| RQ-REL-02-01 | CONNECTED | [REL-02](modules/16-release/REL-02.md) | Yêu cầu CONNECTED được đối chiếu thực tế, bao gồm nguồn lỗi và trạng thái partial. | `docs/progress/REL-02.md` |
| RQ-REL-02-02 | CONNECTED | [REL-02](modules/16-release/REL-02.md) | Những provider chưa được cấp quyền không bị đánh đã tích hợp hoàn chỉnh. | `docs/progress/REL-02.md` |
| RQ-REL-03-01 | SAAS | [REL-03](modules/16-release/REL-03.md) | Không có requirement bắt buộc thiếu owner/task/evidence mà vẫn báo hoàn thiện. | `docs/progress/REL-03.md` |
| RQ-REL-03-02 | SAAS | [REL-03](modules/16-release/REL-03.md) | Hai tenant và hai thiết bị kiểm đúng; có kế hoạch release và rollback reviewable. | `docs/progress/REL-03.md` |
