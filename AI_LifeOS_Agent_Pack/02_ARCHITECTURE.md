# Kiến trúc Android và SaaS

## Quyết định đã chốt

Native Android Kotlin, UI Jetpack Compose/Material 3; coroutines + Flow/StateFlow; ViewModel; Room/SQLite là nguồn UI; WorkManager cho tác vụ trì hoãn; AlarmManager cho nhắc có thời gian; Health Connect cho nguồn sức khỏe; MSAL cho Microsoft. Bản đầu dùng dependency injection thủ công qua AppContainer để giảm cấu hình. Không thêm Hilt/Koin, KMP hay hàng chục feature Gradle module nếu chưa có nhu cầu đã chứng minh.

Ba module Gradle đủ cho bản cá nhân:

```text
repo/
  android/                        # mở thư mục này bằng Android Studio
    app/                          # Compose, ViewModel, navigation, receivers/services
      src/main/kotlin/vn/lifeos/app/
    domain/                       # Kotlin thuần, entity, use case, repository interfaces
      src/main/kotlin/vn/lifeos/domain/
    data/                         # Room, adapters, repositories, network, SDK
      src/main/kotlin/vn/lifeos/data/
      schemas/                    # Room schema history trong git
    gradle/libs.versions.toml
    gradlew  gradlew.bat
  server/                         # chỉ tạo khi bắt đầu SAAS-01
  contracts/                      # JSON/OpenAPI fixtures, không phụ thuộc Room
  fixtures/                       # dữ liệu giả và mẫu đã ẩn thông tin
  docs/agent-pack/                # bộ này
  docs/engineering/
  docs/setup/
  docs/verification/
  docs/progress/
```

Dependency: app -> domain + data; data -> domain; domain không import Android/Room/Compose/MSAL. UI -> ViewModel -> use case/repository -> Room Flow. Connector ghi DB; UI quan sát DB. Không hiển thị một danh sách từ network khác với sổ trong DB.

## Thành phần và trách nhiệm

| Thành phần | Trách nhiệm |
| --- | --- |
| domain/common | Money, UUID, Clock, time ranges, AppError, source states |
| domain/finance | Ledger, transfer, refund, reversal, budgets, matching rules |
| data/db | Room entities/DAOs/migrations, transactions, schema exports |
| data/security | Key wrapping, encrypted DB factory, secure token ownership |
| app/banknotifications | Android service entrypoint, package allowlist trước persistence |
| data/microsoft | Auth, mail/calendar fetch, cursors theo account/folder |
| domain/briefing | Snapshot và tổng hợp có trạng thái từng nguồn |
| data/healthconnect | Health Connect adapter và provenance |
| data/google | Gmail và Calendar theo scope được cấp |
| domain/finance/analytics + app/finance/charts | Tính toán và chart nội bộ từ ledger, không qua Sheets API |
| domain/finance/coaching | Lời khuyên AI dựa trên metric có nguồn và budget proposals |
| domain/actions | Pending proposals, validation và approve idempotent |
| data/sync | Outbox, cursor, retry và conflict |
| server | Ktor/JVM, PostgreSQL, auth, scheduler, AI gateway, subscription |

## Phiên bản và SDK

minSdk mục tiêu 28 để đơn giản hóa java.time và nền tảng Health Connect. FND-01 phải chọn compileSdk/targetSdk/Kotlin/AGP/JDK ổn định tương thích **ở thời điểm code**, khóa phiên bản vào catalog và TOOLCHAIN.md. Không suy phiên bản mới nhất từ tài liệu này.

Room và SQLCipher cần proof of compatibility tại DATA-02. Room API family/driver và SQLCipher adapter phải khớp. Không copy ví dụ SupportFactory cũ sang SupportOpenHelperFactory mới hoặc Room API khác mà không build/test. Không phát hành DB plaintext để né lỗi tích hợp. Nếu chưa có cặp tương thích, báo blocker cụ thể và đề xuất thay đổi kiến trúc có review.

## Android và lifecycle

NotificationListenerService chỉ nhận callback hệ thống sau khi người dùng cấp Notification access. Quyền này không phải POST_NOTIFICATIONS. App vẫn cần xử lý quyền POST_NOTIFICATIONS khi tự hiển thị thông báo trên các Android áp dụng.

Callback lọc package ngay, không network/AI nặng trên main thread. Sự kiện tối thiểu được đưa vào durable queue rồi worker xử lý. WorkManager có thể trễ vì pin/mạng/Doze; không dùng nó để cam kết lấy đủ email mới đúng 09:00. Alarm nhắc xem snapshot; exact permission là khả năng riêng có thể bị từ chối. Force-stop, máy tắt, work profile và OEM có giới hạn riêng cần ghi nhận trong kiểm thật.

UI app lock và khóa DB là hai khái niệm. Một DB key yêu cầu biometric cho mỗi truy cập sẽ làm nền không đọc được DB. Thiết kế nền mặc định dùng key bọc Keystore mở sau unlock; app lock bảo vệ UI. Backup portable có passphrase riêng, không mang key Keystore sang máy khác.

## Microsoft và Google

Microsoft Graph là nguồn ưu tiên cho email/lịch công việc trên loại hộp thư được hỗ trợ. Outlook 2016 là client, không là bằng chứng hộp thư thuộc Exchange Online. App không tự đọc laptop hoặc tự liệt kê toàn bộ account desktop. Mỗi account được kết nối vào LifeOS, có tenant/scopes/checkpoints riêng. Chính sách cơ quan có thể chặn consent hoặc cloud AI.

Google là provider bổ sung cho Gmail/Calendar. Tài chính không phụ thuộc Google account, Google Drive hoặc Sheets API. Sheet đã cung cấp chỉ tham khảo công thức/ý tưởng biểu đồ trong tài liệu; app không tải, import hay export Sheet. Mọi chart/report/coaching đọc cùng analytics snapshot từ Room/ledger, xem 13_FINANCE_ANALYTICS.md.

## SaaS và iOS

SaaS dùng REST/versioned JSON; backend Kotlin Ktor/JVM + PostgreSQL, migrations, OIDC, tenant isolation và outbox protocol. Mobile vẫn hoạt động khi SaaS tắt. Cloud xử lý email/health cần consent riêng và token lifecycle phía server chính thức.

Kotlin Android không tự chạy UI trên iOS. Nếu sau này có iOS, có thể xây client Swift/KMP sau một ADR riêng; domain contracts và backend giúp tái sử dụng quy tắc. Không dựng iOS hoặc KMP trong task hiện tại.
