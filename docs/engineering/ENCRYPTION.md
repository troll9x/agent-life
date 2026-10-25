# DATA-02: mã hóa database và quản lý khóa

## Cặp thư viện đã kiểm

- Room **2.8.5**: `androidx.room:room-runtime`, không dùng Room 3 driver.
- SQLCipher for Android Community **4.19.1**: `net.zetetic:sqlcipher-android` và AndroidX SQLite **2.7.0**: `androidx.sqlite:sqlite`.
- Code gọi `System.loadLibrary("sqlcipher")`, rồi `Room.databaseBuilder(...).openHelperFactory(SupportOpenHelperFactory(passphrase))` và đăng ký `FoundationMigrations.from1To2`. Đây là API Room 2 mà [Zetetic hướng dẫn](https://github.com/sqlcipher/sqlcipher-android#sqlcipher-for-android-room-integration); [Zetetic Community](https://www.zetetic.net/sqlcipher/sqlcipher-for-android-community/) công bố tọa độ/phiên bản. Không dùng `SupportFactory` của thư viện cũ hoặc Room 3 `SQLCipherDriver`.
- Đã build APK và chạy instrumentation trên AVD Android 17/API 37, x86_64. Chưa kiểm ABI/thiết bị vật lý khác.

## Đường mở database

`AgentLifeApplication.databaseProvider` là instance chung cho UI và worker sau này. `EncryptedDatabaseProvider.open()` chạy I/O ngoài main thread, chỉ trả `Ready` khi Room/SQLCipher đã mở và query thử thành công. DB lưu trong credential-protected storage mặc định. Trước lần mở khóa đầu sau reboot, `UserManager.isUserUnlocked == false` trả `UnlockDeviceFirst`; không tạo khóa hoặc DB trong direct boot.

Ở lần khởi tạo sạch, `DatabaseKeyManager` sinh 32 byte bằng `SecureRandom`. Khóa bọc AES-256/GCM không xuất được được tạo trong `AndroidKeyStore`; IV do Cipher sinh. File wrapped key có magic/version, IV và ciphertext, ghi nguyên tử trong `noBackupFilesDir`. Passphrase SQLCipher là hex ASCII của 32 byte ngẫu nhiên; raw secret được xóa khỏi byte array sau khi tạo factory. Passphrase ở trong bộ nhớ khi DB mở để SQLCipher mở connection; `close()` xóa byte array do provider giữ. Không ghi key, token hoặc passphrase vào source, DataStore, DB, log hay backup.

Keystore key đặt `setUserAuthenticationRequired(false)` và `setUnlockedDeviceRequired(false)`: worker có thể mở DB sau khi user đã mở khóa thiết bị ít nhất một lần, kể cả khi màn hình khóa lại. Đổi lại, app lock UI không phải khóa mật mã DB; tiến trình app và Keystore vẫn có thể mở DB trong phiên đã unlock. App lock UI sẽ do task UI sở hữu, không dùng biometric-per-read cho key này. Thiết bị chưa từng unlock sau reboot không đọc được credential-protected DB.

## Mất khóa và dữ liệu cũ

Nếu có DB mà wrapped key không có, hoặc có wrapped key mà DB không có, hoặc Keystore alias mất / GCM xác thực thất bại / SQLCipher không mở được, provider trả `RecoveryRequired`. Không xóa DB, key file hay gọi `fallbackToDestructiveMigration`. Database plaintext có sẵn cũng không được tự thay bằng DB mã hóa rỗng; phải có quy trình migration/khôi phục được review riêng. UI hiển thị hướng dẫn giữ nguyên ứng dụng và dùng bản sao lưu đã xuất. BACKUP-01 sở hữu backup portable và khôi phục sang installation khác; DATA-02 không tuyên bố đã có chức năng restore.

## Backup hệ thống

Manifest đặt `allowBackup=false`, `fullBackupContent=@xml/backup_rules` cho Android 11 trở xuống, `dataExtractionRules=@xml/data_extraction_rules` cho Android 12 trở lên. Cả cloud backup và device transfer loại `root` app data, gồm raw DB, shared preferences/token và file thường; wrapped key nằm trong `noBackupFilesDir`, [Android luôn loại thư mục này](https://developer.android.com/identity/data/autobackup). Auto Backup không thể mang Keystore key sang máy khác, nên không được tạo bản sao DB không mở được. Chưa kiểm live backup/restore transport; BACKUP-01 sẽ làm backup do người dùng xuất với passphrase riêng.

## Kiểm thực tế và giới hạn

- AVD `Medium_Phone_API_37.0`, Android 17/API 37: SQLCipher DB giữ profile qua `close()` và instance provider mới; Android SQLite `OPEN_READONLY` từ chối file, header không phải `SQLite format 3`.
- Xóa Keystore alias giả trong instrumentation khiến `RecoveryRequired`; SHA-256 DB không đổi, wrapped key còn nguyên. DB plaintext hoặc thiếu file DB cũng không bị thay bằng DB rỗng.
- `adb shell input keyevent 26` đưa AVD vào `mWakefulness=Asleep`; user state vẫn `RUNNING_UNLOCKED`. `:data:connectedDebugAndroidTest` 7/7 PASS; test mở DB từ `Dispatchers.IO` sau khi provider trước đã đóng.
- App instrumentation 2/2 PASS: launcher mở encrypted DB rồi hiển thị app; recovery state hiển thị hướng dẫn. Chưa có WorkManager job production trong DATA-02, nhưng provider dùng chung không phụ thuộc UI và đã được gọi từ đường nền trong test.
- Report và lệnh nguyên văn tại [DATA-02 evidence](../progress/DATA-02.md).
