# Nguồn kỹ thuật chính thức

Đối chiếu ngày 19/10/2026. Các liên kết là điểm kiểm tra, không đóng băng phiên bản SDK hoặc chính sách. FND/SDK task phải ghi version thật đã chọn, ngày kiểm và build thực. Không copy số version alpha từ ví dụ tài liệu làm mặc định production.

| Nội dung | Nguồn |
| --- | --- |
| Android architecture, Compose, Flow, repository | https://developer.android.com/topic/architecture/recommendations |
| Offline source of truth | https://developer.android.com/topic/architecture/data-layer |
| Room migrations và schema history | https://developer.android.com/training/data-storage/room/migrating-db-versions |
| Room testing | https://developer.android.com/training/data-storage/room/testing-db |
| SQLCipher Android adapter migration | https://www.zetetic.net/sqlcipher/sqlcipher-for-android-migration/ |
| SQLCipher Android source | https://github.com/sqlcipher/sqlcipher-android |
| WorkManager | https://developer.android.com/topic/libraries/architecture/workmanager |
| AlarmManager và quyền exact | https://developer.android.com/develop/background-work/services/alarms |
| NotificationListenerService | https://developer.android.com/reference/android/service/notification/NotificationListenerService |
| Notification access settings | https://developer.android.com/reference/android/provider/Settings |
| Health Connect availability | https://developer.android.com/health-and-fitness/health-connect/availability |
| Health Connect triển khai | https://developer.android.com/health-and-fitness/health-connect/get-started |
| MSAL Android | https://learn.microsoft.com/en-us/entra/msal/android/ |
| MSAL multiple account | https://learn.microsoft.com/en-us/entra/msal/android/single-multi-account |
| Microsoft Graph mail | https://learn.microsoft.com/en-us/graph/outlook-mail-concept-overview |
| Graph permissions | https://learn.microsoft.com/en-us/graph/permissions-reference |
| Graph mail delta | https://learn.microsoft.com/en-us/graph/api/message-delta |
| Graph calendar view | https://learn.microsoft.com/en-us/graph/api/user-list-calendarview |
| Entra consent policy | https://learn.microsoft.com/en-us/entra/identity/enterprise-apps/configure-user-consent |
| Gmail scopes | https://developers.google.com/workspace/gmail/api/auth/scopes |
| Gmail sync | https://developers.google.com/workspace/gmail/api/guides/sync |
| Gmail push | https://developers.google.com/workspace/gmail/api/guides/push |
| Google Calendar sync | https://developers.google.com/workspace/calendar/api/guides/sync |
| Ktor server | https://ktor.io/docs/server-create-a-new-project.html |
| PostgreSQL | https://www.postgresql.org/docs/ |
| Play Billing | https://developer.android.com/google/play/billing |
| Telegram Bot API | https://core.telegram.org/bots/api |

Tài liệu chính thức hỗ trợ cơ chế/API. Lựa chọn Kotlin native, ba module và thứ tự công việc trong bộ là quyết định thiết kế cho nhu cầu này, không phải khẳng định mọi ứng dụng đều phải theo cấu trúc đó.
