# DATA-02 Mã hóa DB và kho bí mật

## Kết quả

Implementation: **VERIFIED** ngày 2026-10-25 trên AVD Android 17/API 37. External gate: **NOT_REQUIRED**.
Source kiểm: các file DATA-02 trong commit của task này, sau DATA-01 (`7dbefa8`). Không dùng tài khoản, client ID, tenant hay dữ liệu thật.

## Prerequisite và hợp đồng

- DATA-01 đã có `AgentLifeDatabase` v2, `FoundationMigrations.from1To2` và `FoundationRepository`; không đổi public domain/schema. Repo sạch trước task, không có `AGENTS.md`.
- Room 2.8.5 + SQLCipher for Android 4.19.1 + AndroidX SQLite 2.7.0. API thực: `System.loadLibrary("sqlcipher")`, `SupportOpenHelperFactory(byte[])` qua Room 2 `openHelperFactory`. Đã đối chiếu [Zetetic Room integration](https://github.com/sqlcipher/sqlcipher-android#sqlcipher-for-android-room-integration) và [tọa độ bản Community](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-community/).
- Chi tiết vòng đời khóa, backup và tradeoff UI app lock/nền tại [ENCRYPTION.md](../engineering/ENCRYPTION.md). `EncryptedDatabaseProvider` là registration chung cho app và worker tương lai; không có factory Room plaintext trong production.

## File đã thay đổi

- `android/data/src/main/kotlin/vn/lifeos/data/security/`: Keystore AES-256/GCM bọc secret SQLCipher ngẫu nhiên; file wrapped key trong no-backup storage; factory Room/SQLCipher, trạng thái unlock/recovery và đường đóng DB.
- `android/data/src/androidTest/kotlin/vn/lifeos/data/security/EncryptedDatabaseProviderTest.kt`: file ciphertext, reopen từ coroutine nền, mất alias, DB plaintext cũ và DB file mất.
- `android/app/src/main/kotlin/vn/lifeos/app/{AgentLifeApplication,MainActivity}.kt`: provider dùng chung và UI loading/unlock/recovery; không mở DB trên main thread.
- `android/app/src/main/AndroidManifest.xml`, `android/app/src/main/res/xml/{backup_rules,data_extraction_rules}.xml`: loại app data khỏi cloud/D2D Auto Backup, gồm DB/token; wrapped key còn được noBackupFilesDir loại tự động.
- `android/app/src/androidTest/kotlin/vn/lifeos/app/{MainActivitySmokeTest,RecoveryScreenTest}.kt`: chờ startup bất đồng bộ và kiểm hướng dẫn recovery.
- `android/data/build.gradle.kts`, `android/gradle/libs.versions.toml`: pin SQLCipher 4.19.1 và AndroidX SQLite 2.7.0.
- `docs/engineering/{ENCRYPTION,TESTING,TOOLCHAIN}.md`, `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, `01_PRODUCT_CHECKLIST.md` và evidence này: mô tả và trạng thái.

## Đối chiếu yêu cầu

| Requirement ID | Trạng thái | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-DATA-02-01 | **PASS** | `ordinarySqliteCannotReadCiphertextAndBackgroundReopenKeepsProfile`: file DB không có header SQLite, Android SQLite `OPEN_READONLY` ném `SQLiteException`, SQLCipher mở lại và đọc đúng `Fixture User`. | Kiểm trên AVD x86_64/API 37; chưa thử thiết bị/ABI khác. |
| RQ-DATA-02-02 | **PASS** | AVD `RUNNING_UNLOCKED`, tắt màn hình bằng `adb shell input keyevent 26`, sau test `mWakefulness=Asleep`; data instrumentation 7/7 PASS. Provider mới đọc profile từ `Dispatchers.IO` sau khi provider trước đóng. Xóa Keystore alias giả cho `RecoveryRequired`, SHA-256 DB không đổi và wrapped key còn nguyên; app recovery screen test PASS. | Chưa có WorkManager job production để kiểm scheduler/Doze; test chứng minh khả năng mở nền qua provider sau unlock đầu. |

## Lệnh đã chạy

| Lệnh nguyên văn | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `.\gradlew.bat :app:assembleDebug :data:assembleDebugAndroidTest --console=plain` | Windows, Android Studio JBR 25, SDK 37 | BUILD SUCCESSFUL; Room 2.8.5 và SQLCipher 4.19.1 compile/package với `libsqlcipher.so`. |
| `.\gradlew.bat :data:connectedDebugAndroidTest --console=plain` | Ban đầu không có AVD | FAIL `No connected devices`; không tính là test. Sau start AVD nhưng chưa `sys.boot_completed=1`, runner báo `device is still booting` dù Gradle exit 0; không tính là PASS. |
| `.\gradlew.bat :data:connectedDebugAndroidTest --console=plain` | AVD `Medium_Phone_API_37.0`, `sys.boot_completed=1` | BUILD SUCCESSFUL; ban đầu 6/6. Sau thêm kiểm DB mất, 7/7, 0 failures/errors. Chạy lại khi `RUNNING_UNLOCKED` và màn hình `Asleep`, vẫn 7/7 PASS. |
| `.\gradlew.bat :app:connectedDebugAndroidTest --console=plain` | AVD màn hình sáng | Lần đầu FAIL do smoke test chạy trước startup IO hoàn tất và test host sai; sửa test, sau đó 2/2 PASS. Lần chạy tổng hợp với màn hình `Asleep` FAIL `No compose hierarchies`; bật màn hình và chạy riêng lại 2/2 PASS. |
| `.\gradlew.bat :domain:test :data:testDebugUnitTest :app:assembleDebug :data:lintDebug :app:lintDebug --console=plain` | Windows sau code cuối | BUILD SUCCESSFUL; domain 7/7, data JVM `NO-SOURCE`, APK build, data lint 0 issue; app lint 0 errors/5 warnings cũ. |
| `git diff --check` | Repo root | Không có lỗi whitespace. |

Report: `android/data/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17.xml` có `tests="7" failures="0" errors="0"`; app report tương ứng có `tests="2" failures="0" errors="0"`. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

## Kịch bản và dữ liệu

- Fixture: UUID canonical giả, `Fixture User`, alias Keystore chỉ dành test, không có token hay tài khoản thật. Test mất khóa xóa alias test sau khi đóng DB, không đụng alias production.
- Expected/actual: SQLCipher đọc profile sau restart provider; SQLite thường bị từ chối; mất alias hay chỉ còn một nửa cặp DB/wrapped key yêu cầu recovery, không tạo DB rỗng; màn hình recovery có hướng dẫn giữ dữ liệu và dùng bản sao đã xuất. Kết quả đúng trên AVD.
- Backup XML đã được Android build/merge; chưa chạy live cloud backup hoặc device transfer. Thiết lập `allowBackup=false` và exclude `root` cho cloud/D2D, wrapped key trong `noBackupFilesDir`.

## Gate và phần chưa hoàn tất

- Không có gate account/quyền tenant. Chưa kiểm thiết bị vật lý, ABI khác, Auto Backup transport thật hoặc restore portable; BACKUP-01 sở hữu export/restore.
- DATA-01 `RQ-DATA-01-01` vẫn chưa tick vì UI sản phẩm chưa quan sát profile/connection Flow, dù DATA-02 đã cung cấp database provider mã hóa.
- App lock UI và WorkManager job sẽ do task sở hữu thực hiện; key DB cố ý không yêu cầu biometric mỗi lần đọc nền.

## Bàn giao

- Task tiếp theo theo board: DATA-03 (nếu đi theo thứ tự kế hoạch) cần live connector gate; DATA-04 cũng có thể triển khai local. Không tự tiếp tục task khác trong lượt này.
