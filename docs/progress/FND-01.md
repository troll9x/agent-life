# FND-01 Khởi tạo repo Kotlin và toolchain

## Kết quả

Implementation: VERIFIED (user xác nhận chạy app trong Android Studio ngày 2026-10-22).
External gate: NOT_REQUIRED.
Commit hoặc phiên bản kiểm: workspace không có `.git`; kiểm tại 2026-10-22.

## Prerequisite và hợp đồng

- Không có prerequisite. Trước sửa, workspace chỉ có đặc tả tại `AI_LifeOS_Agent_Pack/` và checklist ở gốc; không có `docs/agent-pack/`, `AGENTS.md`, dự án Android hay `.git`. Dùng bản đặc tả 2.1 đang hiện hữu, không di chuyển bộ tài liệu.
- Đã đọc README gốc, FND-01, 02_ARCHITECTURE, 03_DOMAIN_RULES, 04_DATA_CONTRACTS, 06_AGENT_RULES và mẫu evidence. FND-01 chỉ dựng module, chưa định nghĩa entity/interface nghiệp vụ; FND-02 và DATA-02 sẽ sở hữu phần đó.
- Live gate dependency: không có. Không cần client ID, tenant, ngân hàng hay API key để build.

## File đã thay đổi

- `android/settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`: ba module, repositories và version catalog khóa cụ thể.
- `android/gradlew`, `gradlew.bat`, `gradle/wrapper/*`: wrapper Gradle 9.6.0 đầy đủ.
- `android/app/build.gradle.kts`, `src/main/AndroidManifest.xml`, `src/main/kotlin/vn/lifeos/app/MainActivity.kt`: APK Compose tối thiểu với launcher và chữ “Agent Life”.
- `android/data/build.gradle.kts`, `android/domain/build.gradle.kts`: Android library và Kotlin/JVM thuần; app -> data/domain, data -> domain.
- `.gitignore`: loại build, local.properties, IDE state và khóa ký.
- `docs/engineering/TOOLCHAIN.md`: phiên bản, lý do và cách mở/build bằng Android Studio.
- `AI_LifeOS_Agent_Pack/TASK_BOARD.md`, `01_PRODUCT_CHECKLIST.md`: trạng thái FND-01 và RQ-FND-01-02.

## Đối chiếu yêu cầu

| Requirement ID | Trạng thái | Evidence | Giới hạn |
| --- | --- | --- | --- |
| RQ-FND-01-01 | PASS | `:app:assembleDebug` PASS; APK `android/app/build/outputs/apk/debug/app-debug.apk`, 29,405,251 bytes, SHA-256 `E6C3D7B333BECE365A1C3069FF2F9E918EE43B015C4368F223F1F686125FF198`. User xác nhận ngày 2026-10-22 đã mở dự án Android Studio và chạy app trên emulator, thấy chữ “Agent Life”. | Quan sát UI do user báo, không có screenshot trong repo. Gradle Sync thành công được suy từ việc Studio chạy được app. |
| RQ-FND-01-02 | PASS | Version catalog và wrapper khóa version; `rg` kiểm import ở domain không có Android/Room/Compose/SDK; `:domain` build trong assemble. Kiểm lại sau FND-02 khi domain đã có source thực. | Cần giữ ranh giới này ở các task sau. |

## Lệnh đã chạy

| Lệnh nguyên văn (rút gọn biến môi trường) | Môi trường | Kết quả quan sát |
| --- | --- | --- |
| `git status --short` | gốc workspace | exit 128: không có `.git`; không thể đối chiếu diff. |
| `rg --files -g AGENTS.md ...` | gốc workspace | Không có `AGENTS.md` trong workspace; không có code Android trước sửa. |
| `gradle.bat wrapper --gradle-version 9.6.0 --distribution-type bin` | Gradle 9.6 cache, JBR 25 | BUILD SUCCESSFUL. |
| `.\gradlew.bat :app:assembleDebug --console=plain` | JBR 25, SDK API 37 / Build Tools 36 | Lần 1 FAIL: domain yêu cầu toolchain JDK 17 không được cài. Đã chuyển sang JDK máy với bytecode target 17. Lần 2 BUILD SUCCESSFUL, 53 task executed, APK tạo thành công. |
| `rg -n '^(import (android\.|androidx\.|com\.microsoft|com\.google)|.*project\()' domain data app -g '*.kt' -g '*.kts'` | PowerShell | Chỉ thấy dependency đúng chiều trong app/data và import Android ở app; domain không có import bị cấm. |
| `adb devices -l`, `emulator -list-avds`, khởi động `Medium_Phone_API_37.0` headless | Android SDK local | AVD có sẵn nhưng `emulator-5554 offline`; `adb wait-for-device` không hoàn tất sau khoảng 2 phút, đã dừng. Không có UI smoke result. |

## Kịch bản và dữ liệu

- Input: không có tài khoản, API key hay dữ liệu cá nhân; build từ scaffold.
- Expected: tạo debug APK; khi mở app thấy màn hình Compose có chữ “Agent Life”.
- Actual: APK build được; user báo màn hình trắng với chữ “Agent Life” khi chạy từ Android Studio.
- Device/model/API/account type: AVD `Medium_Phone_API_37.0`; không dùng account. Lần thử headless của agent trước đó offline, lần chạy qua Studio do user thực hiện thành công.
- Screenshot/report: chưa có screenshot.

## Gate và phần chưa hoàn tất

- Gate FND-01 đã mở bằng xác nhận chạy thủ công của user; không có gate tài khoản/API ngoài.
- Chưa có `.git` trong workspace, nên chưa thể ghi commit hoặc xác nhận file theo git diff.
- Offline/fixture: build APK không phụ thuộc OAuth/API key. Không có tích hợp ngoài ở task này.

## Bàn giao

- Task tiếp theo sẵn sàng: FND-02, thêm kiểu domain và kiểm thử.
