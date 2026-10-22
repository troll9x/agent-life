# Checklist nghiệm thu toàn sản phẩm

17 module, 69 nhiệm vụ, 138 yêu cầu nghiệm thu có mã.

Tất cả checkbox ban đầu chưa đạt. Dùng để so sánh phần mềm thật với yêu cầu cuối, không phải đánh dấu rằng tài liệu đã viết xong.

Mã **RQ-TASK-ID-NN** ổn định; mỗi dòng có task owner trong TRACEABILITY.md. Đánh PASS chỉ khi criterion đã được kiểm phù hợp và có evidence trong repo.

| Mức | Số task | Điều kiện hoàn thiện |
| --- | --- | --- |
| PERSONAL | 37 | Local core + bảo mật + sao lưu, không phụ thuộc cấp mọi OAuth |
| CONNECTED | 19 | Khả năng provider đã code và live gate bắt buộc đã kiểm |
| SAAS | 11 | Cộng PERSONAL/CONNECTED và nhiều thiết bị, authz, billing, vận hành |
| OPTIONAL | 2 | Telegram/laptop khi release scope chủ động bật |

## Bảng đối chiếu nhanh chức năng cuối

| Nhóm | Hành vi người dùng phải kiểm được | Module |
| --- | --- | --- |
| Cá nhân/offline | Mở app, nhập task/cash/nhật ký, đóng/mở vẫn còn | DATA, UX, PLAN, FIN, HEALTH |
| Công việc Microsoft | Nhiều tài khoản đã kết nối, thư chỉ đọc và lịch hợp nhất | MS, PLAN |
| Bản tin 09:00 | Email P1–P4, 5 việc, họp/deadline, tin có nguồn và lỗi nguồn rõ | BRIEF |
| Tài chính | Thu nhập, chi, tiền mặt, chuyển khoản, refund, budgets, nợ, tiết kiệm | FIN |
| Nhập ngân hàng | Notification app đã chọn -> đề xuất -> xác nhận, email bổ sung | NOTI, GOOGLE, FIN |
| Phân tích chi tiêu trong app | Charts native, ngân sách/thực chi, tổng năm 12 tháng, drill-down và AI coaching | INSIGHT, FIN, AI |
| Sức khỏe | Manual + Health Connect, bữa ăn/nước/mục tiêu, trend có nguồn | HEALTH |
| AI | Hỏi đáp có dữ liệu, policy và pending action, OFF vẫn dùng app | AI |
| Dữ liệu cá nhân | Mã hóa, xuất, backup/restore sang máy khác, xóa theo phạm vi | DATA, BACKUP |
| Thương mại | SaaS login/tenant, sync conflict, AI quota, subscription, runbook | SYNC, SAAS |
| Kênh thêm | Telegram/laptop theo opt-in, quản lý kênh chính | CHANNEL |

## FND Nền tảng Android

### FND-01 Khởi tạo repo Kotlin và toolchain

Mức PERSONAL · [Prompt nhiệm vụ](modules/00-foundation/FND-01.md)

- [x] **RQ-FND-01-01** Repo build được APK debug bằng Gradle wrapper và mở được trong Android Studio. [Bằng chứng](docs/progress/FND-01.md).
- [x] **RQ-FND-01-02** Phiên bản toolchain được khóa cụ thể; domain không import Android, Room, Compose hoặc SDK nhà cung cấp. [Bằng chứng](docs/progress/FND-01.md).

### FND-02 Chuẩn hóa kiểu dữ liệu và quy tắc miền

Mức PERSONAL · [Prompt nhiệm vụ](modules/00-foundation/FND-02.md)

- [x] **RQ-FND-02-01** Money VND 1.000.000 được lưu là 1000000 Long, không có sai số dấu phẩy động. [Bằng chứng](docs/progress/FND-02.md).
- [x] **RQ-FND-02-02** Khoảng ngày địa phương chuyển thành [startInclusive,endExclusive) UTC đúng, kể cả DST của múi giờ khác Việt Nam. [Bằng chứng](docs/progress/FND-02.md).

### FND-03 Dựng kiểm thử và CI tối thiểu

Mức PERSONAL · [Prompt nhiệm vụ](modules/00-foundation/FND-03.md)

- [ ] **RQ-FND-03-01** Một commit có lệnh build/unit/lint tái lập được, CI không cần credential bên ngoài.
- [ ] **RQ-FND-03-02** Kết quả không chạy trên thiết bị được đánh dấu NOT_RUN, không tính là PASS.

## DATA Dữ liệu cục bộ và bảo mật

### DATA-01 Room và migration nền tảng

Mức PERSONAL · [Prompt nhiệm vụ](modules/01-data-security/DATA-01.md)

- [ ] **RQ-DATA-01-01** Hồ sơ và trạng thái kết nối tồn tại sau đóng/mở tiến trình, UI quan sát qua repository.
- [ ] **RQ-DATA-01-02** Migration giữ nguyên ID, số lượng và dữ liệu đã ghi; schema history được lưu trong repo.

### DATA-02 Mã hóa DB và kho bí mật

Mức PERSONAL · [Prompt nhiệm vụ](modules/01-data-security/DATA-02.md)

- [ ] **RQ-DATA-02-01** File DB người dùng không thể đọc bằng SQLite thường mà không có khóa.
- [ ] **RQ-DATA-02-02** Tác vụ nền đọc được DB sau khi thiết bị đã unlock ít nhất một lần; lỗi khóa không gây xóa dữ liệu.

### DATA-03 Nền HTTP và trạng thái kết nối

Mức CONNECTED · [Prompt nhiệm vụ](modules/01-data-security/DATA-03.md)

- [ ] **RQ-DATA-03-01** 401/403/429/offline tạo trạng thái nguồn rõ ràng, không được biến thành danh sách rỗng thành công.
- [ ] **RQ-DATA-03-02** Log không chứa bearer token, nội dung email hoặc thông tin tài khoản ngân hàng.

### DATA-04 Retention và xóa dữ liệu theo nguồn

Mức PERSONAL · [Prompt nhiệm vụ](modules/01-data-security/DATA-04.md)

- [ ] **RQ-DATA-04-01** Ngắt kết nối ngăn request tiếp theo của nguồn và không xóa giao dịch đã xác nhận ngoài ý muốn.
- [ ] **RQ-DATA-04-02** Cleanup lặp nhiều lần an toàn; dữ liệu raw hết hạn không còn trên ổ.

## UX Giao diện, onboarding và cài đặt

### UX-01 App shell Compose và navigation

Mức PERSONAL · [Prompt nhiệm vụ](modules/02-app-shell/UX-01.md)

- [ ] **RQ-UX-01-01** Đi tới được tất cả khu vực và back đúng; khu vực chưa triển khai ghi rõ trạng thái.
- [ ] **RQ-UX-01-02** UI đọc được khi font tăng 1.3x, có chế độ tối và thông báo lỗi có thao tác thử lại.

### UX-02 Onboarding hồ sơ và quyền theo tính năng

Mức PERSONAL · [Prompt nhiệm vụ](modules/02-app-shell/UX-02.md)

- [ ] **RQ-UX-02-01** Cài mới có thể vào app và nhập dữ liệu không cần Microsoft/Google hoặc quyền sức khỏe.
- [ ] **RQ-UX-02-02** Cài đặt được giữ sau khởi động lại; cấp quyền bị từ chối không gây vòng hỏi lặp.

### UX-03 Dashboard Hôm nay và tìm kiếm

Mức PERSONAL · [Prompt nhiệm vụ](modules/02-app-shell/UX-03.md)

- [ ] **RQ-UX-03-01** Dashboard và tìm kiếm hoạt động ngoại tuyến với dữ liệu thật từ các repository.
- [ ] **RQ-UX-03-02** Tổng tài chính trên dashboard bằng báo cáo tài chính cùng kỳ, tiền tệ và bộ lọc.

## PLAN Lịch, nhiệm vụ và nhắc việc

### PLAN-01 Schema task, dự án và lịch

Mức PERSONAL · [Prompt nhiệm vụ](modules/03-planner/PLAN-01.md)

- [ ] **RQ-PLAN-01-01** Task/checklist/lịch được lưu ngoại tuyến và có migration không mất dữ liệu.
- [ ] **RQ-PLAN-01-02** Sự kiện cả ngày không bị lệch sang ngày trước khi đổi múi giờ.

### PLAN-02 Màn hình CRUD công việc và lịch

Mức PERSONAL · [Prompt nhiệm vụ](modules/03-planner/PLAN-02.md)

- [ ] **RQ-PLAN-02-01** Có thể tạo/sửa/hoàn tất/checklist/lịch ngoại tuyến; thay đổi hiện trên dashboard.
- [ ] **RQ-PLAN-02-02** Lịch chồng nhau có cảnh báo và lịch lặp không phát sinh occurrence trùng.

### PLAN-03 Engine nhắc việc Android

Mức PERSONAL · [Prompt nhiệm vụ](modules/03-planner/PLAN-03.md)

- [ ] **RQ-PLAN-03-01** Nhắc/snooze/hủy còn đúng sau reboot, sửa task và đổi múi giờ trên thiết bị được kiểm.
- [ ] **RQ-PLAN-03-02** Thiếu quyền exact/notification hiển thị trạng thái hạn chế, không báo lịch nhắc chắc chắn đúng phút.

### PLAN-04 Đề xuất công việc từ nguồn ngoài

Mức CONNECTED · [Prompt nhiệm vụ](modules/03-planner/PLAN-04.md)

- [ ] **RQ-PLAN-04-01** Email chưa được xác nhận không tự tạo hoặc sửa task/lịch.
- [ ] **RQ-PLAN-04-02** Approve một đề xuất hai lần chỉ tạo một task, có nguồn và deadline đã xác nhận.

## FIN Sổ tài chính và ngân sách

### FIN-01 Sổ giao dịch và số tiền chính xác

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-01.md)

- [ ] **RQ-FIN-01-01** Số dư account bằng opening + tổng entry; chuyển khoản cập nhật hai account atomically.
- [ ] **RQ-FIN-01-02** Không có Double/Float trong tính tiền và không ghi ledger không cân đối của transfer.

### FIN-02 Nhập thu chi, tiền mặt và chuyển khoản

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-02.md)

- [ ] **RQ-FIN-02-01** Ghi thu/chi và tiền mặt hoạt động offline, dữ liệu còn sau đóng/mở app.
- [ ] **RQ-FIN-02-02** Chuyển khoản nội bộ và submit lặp giữ đúng số dư, không tăng income/expense.

### FIN-03 Ngân sách và báo cáo tháng

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-03.md)

- [ ] **RQ-FIN-03-01** Số trên dashboard, danh sách và CSV đối chiếu được cùng kỳ/currency.
- [ ] **RQ-FIN-03-02** Chuyển tiền sang tiết kiệm không tiêu ngân sách chi phí; refund giảm expense theo kỳ nhận refund.

### FIN-04 Hàng chờ nhập và ghép bằng chứng chống trùng

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-04.md)

- [ ] **RQ-FIN-04-01** Thông báo/email/nhập tay cùng khoản sau quyết định ghép chỉ làm ghi sổ một lần.
- [ ] **RQ-FIN-04-02** Giao dịch cùng tiền khác reference được giữ riêng; mơ hồ luôn chờ người dùng.

### FIN-05 Mục tiêu và tài khoản tiết kiệm

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-05.md)

- [ ] **RQ-FIN-05-01** Mục tiêu tiết kiệm có tiến độ lấy từ ledger và dữ liệu lưu ngoại tuyến.
- [ ] **RQ-FIN-05-02** Chuyển sang tiết kiệm không tiêu ngân sách chi phí hoặc nhân đôi tổng tài sản.

### FIN-06 Theo dõi cho vay và đi vay

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-06.md)

- [ ] **RQ-FIN-06-01** Gốc vay/cho vay và hoàn gốc thay đúng balance/principal, không cộng thu/chi tiêu dùng.
- [ ] **RQ-FIN-06-02** Lãi/phí được thống kê riêng, principal outstanding đối chiếu được từng movement.

### FIN-07 Giao dịch định kỳ và khoản dự kiến

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-07.md)

- [ ] **RQ-FIN-07-01** Khoản dự kiến/lặp chưa xác nhận không làm thay đổi balance hoặc thực chi.
- [ ] **RQ-FIN-07-02** Mỗi occurrence có tối đa một khoản được post sau approve, có thể ghép với bằng chứng ngân hàng.

### FIN-08 Sửa khoản đã post bằng reversal

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-08.md)

- [ ] **RQ-FIN-08-01** Sửa khoản 500000 thành 600000 cho net expense 600000, số dư chỉ giảm thêm 100000 trong cùng kỳ.
- [ ] **RQ-FIN-08-02** Double submit/hai client cùng reverse một khoản chỉ có một reversal được chấp nhận.

### FIN-09 Hoàn tiền và điều chỉnh số dư có lý do

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-09.md)

- [ ] **RQ-FIN-09-01** Chi 500000, refund 100000 -> net expense 400000 và income 0 trong cùng kỳ.
- [ ] **RQ-FIN-09-02** Adjustment đổi số dư nhưng không làm AI/report coi đó là thu nhập hoặc chi phí tiêu dùng.

### FIN-10 Đối soát tài khoản và chênh lệch

Mức PERSONAL · [Prompt nhiệm vụ](modules/04-finance/FIN-10.md)

- [ ] **RQ-FIN-10-01** Đối soát cùng mốc thời gian ra đúng difference và không tự thay balance.
- [ ] **RQ-FIN-10-02** Trạng thái thiếu nguồn/chưa đối soát được truyền vào báo cáo và lời khuyên AI.

## NOTI Thông báo ngân hàng Android

### NOTI-01 Cấp quyền và NotificationListenerService

Mức CONNECTED · [Prompt nhiệm vụ](modules/05-bank-notifications/NOTI-01.md)

- [ ] **RQ-NOTI-01-01** Notification app không nằm allowlist không để lại nội dung trong DB/log.
- [ ] **RQ-NOTI-01-02** Khi UI đóng, notification được phép vẫn vào queue; thu hồi quyền được nhận diện.

### NOTI-02 Parser thông báo theo mẫu ngân hàng

Mức CONNECTED · [Prompt nhiệm vụ](modules/05-bank-notifications/NOTI-02.md)

- [ ] **RQ-NOTI-02-01** Template đã hỗ trợ trích đúng amount của giao dịch, không lấy nhầm số dư.
- [ ] **RQ-NOTI-02-02** Mẫu thiếu nội dung hoặc không rõ tiền vào/ra luôn chờ xác nhận.

### NOTI-03 Lưu sự kiện lặp và phục hồi sau crash

Mức CONNECTED · [Prompt nhiệm vụ](modules/05-bank-notifications/NOTI-03.md)

- [ ] **RQ-NOTI-03-01** Replay cùng event sau crash không tạo thêm candidate hoặc ledger.
- [ ] **RQ-NOTI-03-02** Cùng notification key nhưng giao dịch mới có ref khác vẫn được nhận là khoản mới.

### NOTI-04 Luồng xác nhận ngân hàng trên thiết bị

Mức CONNECTED · [Prompt nhiệm vụ](modules/05-bank-notifications/NOTI-04.md)

- [ ] **RQ-NOTI-04-01** Candidate xác nhận một lần xuất hiện trên sổ và báo cáo đúng một lần.
- [ ] **RQ-NOTI-04-02** Có bằng chứng kiểm thử thiết bị hoặc ghi EXTERNAL_BLOCKED nếu chưa có thiết bị/mẫu thật.

## MS Microsoft email và lịch công việc

### MS-01 MSAL đa tài khoản và khả năng kết nối

Mức CONNECTED · [Prompt nhiệm vụ](modules/06-microsoft/MS-01.md)

- [ ] **RQ-MS-01-01** Microsoft A/B có connection và token đúng account, disconnect một account không ảnh hưởng account còn lại.
- [ ] **RQ-MS-01-02** Tổ chức từ chối quyền/Exchange không được hỗ trợ hiển thị lý do, không vòng login vô hạn.

### MS-02 Đồng bộ thư theo folder và checkpoint

Mức CONNECTED · [Prompt nhiệm vụ](modules/06-microsoft/MS-02.md)

- [ ] **RQ-MS-02-01** Quét bị ngắt giữa trang không bỏ thư, lần sau tiếp tục và không nhân đôi signal.
- [ ] **RQ-MS-02-02** Đọc thư không đánh dấu đã đọc, di chuyển, xóa, gửi hoặc mở attachment.

### MS-03 Phân loại email và bằng chứng công việc

Mức CONNECTED · [Prompt nhiệm vụ](modules/06-microsoft/MS-03.md)

- [ ] **RQ-MS-03-01** Mỗi P1/P2 có lý do, trích dẫn nguồn và liên kết mở email; không bịa deadline.
- [ ] **RQ-MS-03-02** Các thư trong cùng trao đổi không tạo nhiều bản sao cùng một việc.

### MS-04 Lịch Microsoft chỉ đọc và hợp nhất

Mức CONNECTED · [Prompt nhiệm vụ](modules/06-microsoft/MS-04.md)

- [ ] **RQ-MS-04-01** Họp Microsoft, all-day và hủy lịch được phản ánh đúng trong lịch hợp nhất.
- [ ] **RQ-MS-04-02** Đọc lịch không tự tạo/sửa/accept invite; calendar bị chặn hiện trạng thái riêng.

## BRIEF Bản tin công việc và công nghệ 09:00

### BRIEF-01 Nguồn tin công nghệ và lọc mới

Mức CONNECTED · [Prompt nhiệm vụ](modules/07-morning-briefing/BRIEF-01.md)

- [ ] **RQ-BRIEF-01-01** Tin bản tin có ngày và URL đã thu thập thật; không lặp cùng sự kiện vô ích.
- [ ] **RQ-BRIEF-01-02** Thiếu web/RSS cho ra PARTIAL/ERROR, không ghi “không có tin mới” như đã xác minh.

### BRIEF-02 Engine tổng hợp snapshot bản tin

Mức CONNECTED · [Prompt nhiệm vụ](modules/07-morning-briefing/BRIEF-02.md)

- [ ] **RQ-BRIEF-02-01** Snapshot đủ 8 phần hoặc ghi rõ phần thiếu; tổng quan và số đếm khớp dữ liệu nguồn.
- [ ] **RQ-BRIEF-02-02** Bản tin không tự gửi email, đổi trạng thái thư hoặc tạo task/lịch.

### BRIEF-03 Màn hình bản tin và lịch sử

Mức CONNECTED · [Prompt nhiệm vụ](modules/07-morning-briefing/BRIEF-03.md)

- [ ] **RQ-BRIEF-03-01** Có thể đọc bản tin và lịch sử ngoại tuyến; nguồn cũ/thiếu được thấy rõ.
- [ ] **RQ-BRIEF-03-02** Bấm nguồn mở đúng email/event/trang đã liên kết hoặc báo không khả dụng.

### BRIEF-04 Lịch 09:00 và thông báo bản tin

Mức CONNECTED · [Prompt nhiệm vụ](modules/07-morning-briefing/BRIEF-04.md)

- [ ] **RQ-BRIEF-04-01** Không có hai thông báo cùng bản tin/ngày/kênh khi job chạy lại.
- [ ] **RQ-BRIEF-04-02** Ngày/múi giờ và tình trạng chuẩn bị được xử lý đúng cả khi offline hoặc thiếu exact alarm permission.

## HEALTH Sức khỏe, dinh dưỡng và thói quen

### HEALTH-01 Nhật ký và chỉ số sức khỏe thủ công

Mức PERSONAL · [Prompt nhiệm vụ](modules/08-health-nutrition/HEALTH-01.md)

- [ ] **RQ-HEALTH-01-01** Nhập, sửa, xem lịch sử sức khỏe chạy không mạng và giữ đúng đơn vị.
- [ ] **RQ-HEALTH-01-02** Không có dữ liệu hiển thị thiếu dữ liệu, không dùng số demo làm thống kê.

### HEALTH-02 Bữa ăn, nước và mục tiêu thói quen

Mức PERSONAL · [Prompt nhiệm vụ](modules/08-health-nutrition/HEALTH-02.md)

- [ ] **RQ-HEALTH-02-01** Ghi bữa ăn/nước/mục tiêu và tổng ngày đúng ngoại tuyến.
- [ ] **RQ-HEALTH-02-02** Giá trị ước tính và giá trị người dùng đo/nhập được phân biệt.

### HEALTH-03 Health Connect có quyền theo loại dữ liệu

Mức CONNECTED · [Prompt nhiệm vụ](modules/08-health-nutrition/HEALTH-03.md)

- [ ] **RQ-HEALTH-03-01** Từ chối/thu hồi quyền không crash và ngừng request dữ liệu bị thu hồi.
- [ ] **RQ-HEALTH-03-02** Mẫu imported có provenance, không nhân đôi steps/sleep từ nhiều thiết bị.

### HEALTH-04 Xu hướng và gợi ý có giới hạn

Mức PERSONAL · [Prompt nhiệm vụ](modules/08-health-nutrition/HEALTH-04.md)

- [ ] **RQ-HEALTH-04-01** Biểu đồ và số tổng có thể đối chiếu lại số mẫu và đơn vị.
- [ ] **RQ-HEALTH-04-02** Mẫu thiếu được ghi rõ, không suy ra người dùng khỏe/không khỏe từ absence.

## GOOGLE Google OAuth, Gmail và Calendar

### GOOGLE-01 Google OAuth cho Gmail và Calendar

Mức CONNECTED · [Prompt nhiệm vụ](modules/09-google/GOOGLE-01.md)

- [ ] **RQ-GOOGLE-01-01** Gmail và Calendar có consent/trạng thái riêng; ứng dụng không cần Google để quản lý tài chính.
- [ ] **RQ-GOOGLE-01-02** Không có scope/SDK/flow Google Sheets hoặc Drive chỉ để phục vụ báo cáo tài chính.

### GOOGLE-02 Gmail bổ sung giao dịch ngân hàng

Mức CONNECTED · [Prompt nhiệm vụ](modules/09-google/GOOGLE-02.md)

- [ ] **RQ-GOOGLE-02-01** Email cùng giao dịch đã có không làm ghi sổ hai lần; cursor hết hạn không bỏ sót vùng đã chọn.
- [ ] **RQ-GOOGLE-02-02** Thiếu quyền/body/mẫu unsupported hiện partial/candidate cần xác nhận, không giả amount.

### GOOGLE-03 Google Calendar chỉ đọc tùy chọn

Mức CONNECTED · [Prompt nhiệm vụ](modules/09-google/GOOGLE-03.md)

- [ ] **RQ-GOOGLE-03-01** Lịch Google hiển thị cùng Microsoft/local, giữ nguồn và không làm sai all-day.
- [ ] **RQ-GOOGLE-03-02** Thu hồi quyền Google không làm mất lịch cục bộ hoặc kết nối Microsoft.

## INSIGHT Phân tích, biểu đồ và lời khuyên chi tiêu trong app

### INSIGHT-01 Công thức tổng hợp tài chính nội bộ

Mức PERSONAL · [Prompt nhiệm vụ](modules/10-finance-insights/INSIGHT-01.md)

- [ ] **RQ-INSIGHT-01-01** Report/chart/AI cùng dùng một kết quả tính từ ledger; số dư thực và ngân sách còn lại được phân biệt.
- [ ] **RQ-INSIGHT-01-02** Tổng năm đủ 12 tháng, chia 0/thiếu tháng/refund âm xử lý rõ và không cần Google account.

### INSIGHT-02 Biểu đồ tài chính native và drill-down

Mức PERSONAL · [Prompt nhiệm vụ](modules/10-finance-insights/INSIGHT-02.md)

- [ ] **RQ-INSIGHT-02-01** Các chart chạy offline từ dữ liệu app và mỗi số có thể drill-down tới đúng danh sách giao dịch.
- [ ] **RQ-INSIGHT-02-02** Chart và bảng cùng tổng, có đơn vị/chú giải; giá trị âm/không có dữ liệu không bị vẽ sai.

### INSIGHT-03 Cảnh báo chi tiêu và so sánh kỳ

Mức PERSONAL · [Prompt nhiệm vụ](modules/10-finance-insights/INSIGHT-03.md)

- [ ] **RQ-INSIGHT-03-01** Cảnh báo vượt ngân sách/biến động có số liệu và khoảng thời gian đối chiếu rõ.
- [ ] **RQ-INSIGHT-03-02** Ước tính chi cuối kỳ được gắn giả định/thiếu dữ liệu và không được trình bày như số chắc chắn.

### INSIGHT-04 AI cố vấn chi tiêu cá nhân có bằng chứng

Mức PERSONAL · [Prompt nhiệm vụ](modules/10-finance-insights/INSIGHT-04.md)

- [ ] **RQ-INSIGHT-04-01** AI đưa lời khuyên chi tiêu trong app có số liệu/nguồn và data-quality; không cần Google Sheet.
- [ ] **RQ-INSIGHT-04-02** Số tiền giả định được tính chính xác bằng code và thay đổi ngân sách chỉ xảy ra sau xác nhận.

## AI Trợ lý AI và hành động có xác nhận

### AI-01 Cổng AI và privacy budget

Mức PERSONAL · [Prompt nhiệm vụ](modules/11-ai-assistant/AI-01.md)

- [ ] **RQ-AI-01-01** Tắt AI không phá CRUD/báo cáo/bản tin quy tắc; remote không được gọi khi thiếu consent.
- [ ] **RQ-AI-01-02** Response sai schema hoặc không có evidence không được thực thi hành động.

### AI-02 Pending action và approve idempotent

Mức PERSONAL · [Prompt nhiệm vụ](modules/11-ai-assistant/AI-02.md)

- [ ] **RQ-AI-02-01** AI chưa được approve không thay đổi dữ liệu quan trọng.
- [ ] **RQ-AI-02-02** Double approve hoặc replay action không tạo thêm task/giao dịch.

### AI-03 Hỏi đáp cá nhân và tóm tắt có nguồn

Mức PERSONAL · [Prompt nhiệm vụ](modules/11-ai-assistant/AI-03.md)

- [ ] **RQ-AI-03-01** Câu trả lời về tổng chi khớp báo cáo cùng kỳ/currency và có link nguồn.
- [ ] **RQ-AI-03-02** Câu hỏi ngoài dữ liệu không được trả bằng thông tin cá nhân bịa đặt.

### AI-04 Giới hạn on-device và chống prompt injection

Mức PERSONAL · [Prompt nhiệm vụ](modules/11-ai-assistant/AI-04.md)

- [ ] **RQ-AI-04-01** Nội dung email/web không gây gọi tool ngoài allowlist hoặc lộ bí mật.
- [ ] **RQ-AI-04-02** Thiếu local/remote model vẫn có fallback và trạng thái rõ, không treo app.

## BACKUP Sao lưu, khôi phục và quyền sở hữu dữ liệu

### BACKUP-01 Định dạng sao lưu mã hóa portable

Mức PERSONAL · [Prompt nhiệm vụ](modules/12-backup-export/BACKUP-01.md)

- [ ] **RQ-BACKUP-01-01** Backup mã hóa không đọc được plaintext và có thể giải bằng passphrase trên installation mới.
- [ ] **RQ-BACKUP-01-02** Snapshot khôi phục giữ đúng ID, balance và task/health, không mang token OAuth theo.

### BACKUP-02 Khôi phục an toàn và migration

Mức PERSONAL · [Prompt nhiệm vụ](modules/12-backup-export/BACKUP-02.md)

- [ ] **RQ-BACKUP-02-01** Restore sang installation khác đọc được dữ liệu bằng khóa mới, không cần khóa máy cũ.
- [ ] **RQ-BACKUP-02-02** Restore file hỏng/phiên bản chưa hỗ trợ không làm thay đổi workspace hiện tại.

### BACKUP-03 Xuất JSON CSV và xóa dữ liệu

Mức PERSONAL · [Prompt nhiệm vụ](modules/12-backup-export/BACKUP-03.md)

- [ ] **RQ-BACKUP-03-01** JSON/CSV mở được, số tiền/date/currency đối chiếu đúng và không rò token.
- [ ] **RQ-BACKUP-03-02** Xóa local xóa dữ liệu được chọn, không xóa tệp hoặc workspace khác.

## SYNC Đồng bộ nhiều thiết bị

### SYNC-01 Hợp đồng wire và change envelopes

Mức SAAS · [Prompt nhiệm vụ](modules/13-sync/SYNC-01.md)

- [ ] **RQ-SYNC-01-01** Client/server có một wire schema versioned, money không mất chính xác khi qua JSON.
- [ ] **RQ-SYNC-01-02** Duplicate/replay/conflict/rejected có response được định nghĩa trước khi code network sync.

### SYNC-02 Outbox và push/pull mobile

Mức SAAS · [Prompt nhiệm vụ](modules/13-sync/SYNC-02.md)

- [ ] **RQ-SYNC-02-01** Thay đổi ngoại tuyến vẫn tồn tại và gửi được sau restart/reconnect, không nhân đôi.
- [ ] **RQ-SYNC-02-02** Cursor chỉ tiến sau apply thành công, không làm mất trang khi crash.

### SYNC-03 Server change log và tenant isolation

Mức SAAS · [Prompt nhiệm vụ](modules/13-sync/SYNC-03.md)

- [ ] **RQ-SYNC-03-01** Push lặp chỉ tác động một lần và pull không chứa bản ghi workspace khác.
- [ ] **RQ-SYNC-03-02** Financial aggregate không bị tách hoặc mất entry khi sync.

### SYNC-04 Giải quyết xung đột nhiều thiết bị

Mức SAAS · [Prompt nhiệm vụ](modules/13-sync/SYNC-04.md)

- [ ] **RQ-SYNC-04-01** Hai thiết bị sửa một khoản không tạo hai sổ thu/chi hoặc hai reversal.
- [ ] **RQ-SYNC-04-02** Xung đột hiện tới người dùng và giải quyết xong có thể sync tiếp.

## SAAS Backend và thương mại hóa

### SAAS-01 Khởi tạo backend Kotlin và PostgreSQL

Mức SAAS · [Prompt nhiệm vụ](modules/14-saas/SAAS-01.md)

- [ ] **RQ-SAAS-01-01** Backend khởi động local, migration chạy được trên DB sạch và readiness báo đúng tình trạng DB.
- [ ] **RQ-SAAS-01-02** Thiếu env bắt buộc fail rõ; không tự dùng credential production hoặc dev secret ở production.

### SAAS-02 OIDC và quyền workspace

Mức SAAS · [Prompt nhiệm vụ](modules/14-saas/SAAS-02.md)

- [ ] **RQ-SAAS-02-01** User không đọc/ghi workspace khác bằng thay ID ở URL, JSON hoặc cursor.
- [ ] **RQ-SAAS-02-02** Token hết hạn/sai issuer/audience/signature bị từ chối; app local vẫn dùng khi SaaS logout.

### SAAS-03 Scheduler bản tin và nguồn cloud opt-in

Mức SAAS · [Prompt nhiệm vụ](modules/14-saas/SAAS-03.md)

- [ ] **RQ-SAAS-03-01** Một ngày/workspace có một logical briefing, chạy lại không nhân đôi notification.
- [ ] **RQ-SAAS-03-02** Không bật cloud consent thì backend không đọc email/sức khỏe hoặc nhận token nguồn đó.

### SAAS-04 AI gateway và quota theo workspace

Mức SAAS · [Prompt nhiệm vụ](modules/14-saas/SAAS-04.md)

- [ ] **RQ-SAAS-04-01** APK không cần chứa provider secret; vượt quota không gọi model tiếp.
- [ ] **RQ-SAAS-04-02** Request AI không được phép về email công việc bị từ chối trước khi gửi provider.

### SAAS-05 Subscription và entitlement server

Mức SAAS · [Prompt nhiệm vụ](modules/14-saas/SAAS-05.md)

- [ ] **RQ-SAAS-05-01** Entitlement thay đổi theo giao dịch đã xác minh, duplicate webhook không cộng gói hai lần.
- [ ] **RQ-SAAS-05-02** Hết gói vẫn xem/xuất dữ liệu và dùng local core theo feature matrix đã công bố.

### SAAS-06 Vận hành, backup server và xóa cloud

Mức SAAS · [Prompt nhiệm vụ](modules/14-saas/SAAS-06.md)

- [ ] **RQ-SAAS-06-01** Có restore drill thực tế ở staging và deletion job không ảnh hưởng tenant khác.
- [ ] **RQ-SAAS-06-02** Runbook nêu deployment/release gates còn thiếu; chưa deploy production không đánh đã triển khai.

## CHANNEL Telegram và thông báo laptop tùy chọn

### CHANNEL-01 Telegram opt-in và chống gửi trùng

Mức OPTIONAL · [Prompt nhiệm vụ](modules/15-extra-channels/CHANNEL-01.md)

- [ ] **RQ-CHANNEL-01-01** Không gửi Telegram khi chưa có opt-in và chat đã xác minh.
- [ ] **RQ-CHANNEL-01-02** Một briefing/ngày không bị gửi nhiều lần do worker retry.

### CHANNEL-02 Kênh laptop và chọn kênh chính

Mức OPTIONAL · [Prompt nhiệm vụ](modules/15-extra-channels/CHANNEL-02.md)

- [ ] **RQ-CHANNEL-02-01** Laptop có thể xem cùng briefing snapshot của tài khoản đã xác thực.
- [ ] **RQ-CHANNEL-02-02** Người dùng quản lý được kênh nhận và hiểu khả năng trùng với tác vụ Codex cũ.

## REL Nghiệm thu và phát hành

### REL-01 Nghiệm thu bản cá nhân ngoại tuyến

Mức PERSONAL · [Prompt nhiệm vụ](modules/16-release/REL-01.md)

- [ ] **RQ-REL-01-01** Tất cả requirement PERSONAL có bằng chứng hoặc trạng thái chưa đạt rõ ràng.
- [ ] **RQ-REL-01-02** Có APK cài được và backup/restore trên installation khác đã kiểm; chưa kiểm thì gate còn mở.

### REL-02 Nghiệm thu tích hợp trên thiết bị và tài khoản thật

Mức CONNECTED · [Prompt nhiệm vụ](modules/16-release/REL-02.md)

- [ ] **RQ-REL-02-01** Yêu cầu CONNECTED được đối chiếu thực tế, bao gồm nguồn lỗi và trạng thái partial.
- [ ] **RQ-REL-02-02** Những provider chưa được cấp quyền không bị đánh đã tích hợp hoàn chỉnh.

### REL-03 Nghiệm thu SaaS và bàn giao cuối

Mức SAAS · [Prompt nhiệm vụ](modules/16-release/REL-03.md)

- [ ] **RQ-REL-03-01** Không có requirement bắt buộc thiếu owner/task/evidence mà vẫn báo hoàn thiện.
- [ ] **RQ-REL-03-02** Hai tenant và hai thiết bị kiểm đúng; có kế hoạch release và rollback reviewable.

## Gate hoàn thiện cuối

- [ ] Mọi requirement bắt buộc trong release scope đã có PASS và evidence phù hợp, không chỉ stub/fixture.
- [ ] Mọi gate tài khoản/tenant/thiết bị chưa chạy được liệt kê rõ; không bị tính là đã nghiệm thu.
- [ ] Các kịch bản liên module trong 08_ACCEPTANCE_SCENARIOS đã đối chiếu expected/actual.
- [ ] Known issues, hướng dẫn backup/restore, phiên bản APK/server và phạm vi release đã ghi.
- [ ] Nếu gọi là FULL: PERSONAL, CONNECTED, SAAS đều đạt; OPTIONAL được chọn rõ trong release scope.

Không tự tick các dòng này khi kiểm tra bộ tài liệu. Chúng chỉ được cập nhật sau kiểm sản phẩm có code.
