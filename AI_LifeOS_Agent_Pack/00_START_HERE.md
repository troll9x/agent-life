# Cách sử dụng bộ yêu cầu

## Sản phẩm khi hoàn thiện

Người dùng quản lý việc, lịch, nhắc, sức khỏe, bữa ăn, tiền mặt và ngân hàng trên Android. Thông báo ngân hàng tạo giao dịch đề xuất; Microsoft email và lịch tạo bản tin công việc lúc 09:00, có tin công nghệ xác minh được. Dữ liệu tài chính, tính toán, biểu đồ và lời khuyên AI đều ở trong ứng dụng; không kết nối, đọc hoặc ghi Google Sheet khi app chạy. AI hỏi đáp và đề xuất có nguồn; mọi hành động nhạy cảm cần người dùng xác nhận. Bản SaaS bổ sung đăng nhập, nhiều thiết bị, scheduler cloud và gói dịch vụ.

## Chốt công nghệ cho repo mới

Kotlin + Jetpack Compose + Room + Android APIs, mở bằng Android Studio. Lý do chọn: nhiều chức năng phụ thuộc Android gốc như NotificationListenerService, Health Connect, AlarmManager, WorkManager và MSAL. User đã cho phép dùng Kotlin. SaaS không bắt buộc mobile dùng React Native.

Tài liệu v1.2 trước đây dùng React Native và SQL minh họa. Bộ 2.1 này thay thế **phương án triển khai, schema chi tiết và cách chia task**; các nhu cầu đã chốt tiếp tục được giữ. Không copy TypeScript interface hoặc DDL tài chính cũ vào dự án Kotlin. Nếu repo đã có React Native đáng kể, dừng việc tạo repo mới và lập báo cáo ánh xạ/migration để người dùng quyết định; không tự xóa hoặc viết lại toàn bộ.

## Mức hoàn thiện

| Mức | Ý nghĩa | Điều kiện |
| --- | --- | --- |
| PERSONAL | Lõi cá nhân chạy offline, backup và bảo mật; AI có OFF/FAKE và local tùy chọn | Hoàn tất yêu cầu mức PERSONAL, không bắt buộc cấp mọi OAuth |
| CONNECTED | Microsoft, ngân hàng, bản tin và Google hoạt động theo tính năng người dùng bật | Code/fixture và gate tài khoản/thiết bị thật đều có bằng chứng |
| SAAS | Backend, nhiều thiết bị, tenant, subscription và vận hành | Hoàn tất PERSONAL + CONNECTED + SAAS |
| OPTIONAL | Telegram/laptop thêm theo lựa chọn | Chỉ là gate bắt buộc nếu release scope bật kênh đó |

FULL trong bộ này là PERSONAL + CONNECTED + SAAS. Không được gọi bản PERSONAL là đã đủ sản phẩm cuối. Tính năng CONNECTED là khả năng của ứng dụng; người dùng có thể không cấp quyền hoặc không dùng một provider, và app phải giải thích được trạng thái đó. Kiểm provider thật là gate của chính provider, không phải lý do khóa việc nhập tiền mặt hoặc task offline.

## Một lượt làm việc với agent

1. Sao chép nguyên bộ vào repo, giữ cấu trúc module.
2. Mở PROMPT_START.md, thay task ID bằng file được chọn. FND-01 là task đầu cho repo trống.
3. Agent đọc 06_AGENT_RULES và task, kiểm prerequisite đã IMPLEMENTED. Nếu task trước còn live gate bên ngoài, chỉ tiếp tục nếu interface/code đã sẵn và ghi rõ gate còn mở.
4. Agent sửa đúng một phạm vi, chạy kiểm tra cần thiết, ghi bằng chứng.
5. Người điều phối so sánh requirement IDs và kết quả. Chỉ chọn task tiếp theo sau khi code task này có thể review.

Agent không tự bật hàng chục task, không đánh dấu requirement đã đạt nhờ có code stub. Nếu thiếu password/client ID/thiết bị, viết adapter+fixture đúng hợp đồng, ghi EXTERNAL_BLOCKED cho live gate và tiếp tục phần độc lập đã được giao. Không xin lại quyền cho thao tác đọc/sửa code đã được user cho phép.

## Ý nghĩa trạng thái

- TODO: chưa làm.
- IN_PROGRESS: đang triển khai.
- IMPLEMENTED: code và kiểm nội bộ đã xong, chưa đủ bằng chứng trên nguồn thật nếu có gate.
- VERIFIED: mọi điều kiện task và gate bắt buộc của nó đã đạt, có evidence.
- BLOCKED: phần code bắt buộc không thể tiến tiếp, nêu blocker và thông tin cần bổ sung.

Gate ngoài hệ thống dùng NOT_REQUIRED / NOT_RUN / PASS / EXTERNAL_BLOCKED / FAIL riêng trong TASK_BOARD. Không dùng EXTERNAL_BLOCKED để che lỗi code; lỗi code là FAIL hoặc BLOCKED. Không tick checklist sản phẩm cuối nếu gate bắt buộc còn NOT_RUN hoặc EXTERNAL_BLOCKED.
