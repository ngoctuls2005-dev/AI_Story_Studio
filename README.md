# AI Story Studio

Ứng dụng Android cá nhân phát triển bằng Kotlin, Jetpack Compose và Material 3. A-02 tạo theme sáng/tối, khung điều hướng và sáu màn hình Home, Roleplay, Novel, Characters, Library, Settings. A-03 bổ sung Room schema V1 cho hồ sơ, hội thoại và dữ liệu Novel; các màn tính năng vẫn là khung, chưa có chat/editor thực tế, DataStore hay kết nối AI.

## Tài liệu dự án

- docs/BACKLOG_V1.md — phạm vi V1.0, thứ tự nhiệm vụ và tiêu chí nghiệm thu.
- AGENTS.md — quy tắc làm việc và kiểm thử.
- docs/ARCHITECTURE.md — kiến trúc mục tiêu và hiện trạng.
- docs/ROADMAP.md — thứ tự thực hiện và trạng thái.
- docs/TEST_PLAN.md — kế hoạch kiểm thử và mức chứng minh của test mẫu.
- docs/DECISIONS.md — các quyết định kiến trúc.
- docs/TASK_LOG.md — lệnh, bằng chứng và điều kiện nghiệm thu.

## Môi trường build hiện tại

| Thành phần | Cấu hình |
| --- | --- |
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.6.0 |
| Gradle daemon JDK | 25, ghim trong gradle/gradle-daemon-jvm.properties |
| Android SDK Platform | 37 |
| Android SDK Build Tools | 36.0.0 |
| Kotlin Compose plugin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| minSdk / targetSdk | 26 / 37 |
| applicationId | com.hoangtu.aistorystudio |

Compose bật trong module app bằng buildFeatures.compose = true; Material 3 và Compose UI dùng Compose BOM. Java source/target đặt mức 11. Room runtime/compiler/testing 2.8.5 được xử lý bằng KSP 2.3.12; schema version 1 được theo dõi trong `app/schemas/`.

AGP 9.4 yêu cầu Gradle 9.6, hỗ trợ API tối đa 37 và Build Tools 36.0.0. Các giá trị này khớp cấu hình hiện tại. Gradle daemon dùng JDK 25. JDK 27 mặc định trên PATH chưa được Gradle 9.6 công bố hỗ trợ làm JVM chạy Gradle; dùng JBR 25 đi kèm Android Studio. Build, unit test, lint và instrumentation test A-03 đã chạy trên cấu hình này với emulator API 37.

## Mở project và build trên Windows

1. Cài Android Studio, Android SDK Platform 37 và Build Tools 36.0.0.
2. Mở thư mục project trong Android Studio. Giữ local.properties ở máy cá nhân để chỉ đường dẫn Android SDK; không commit file này.
3. Chọn Gradle JDK 25 tại Settings > Build, Execution, Deployment > Build Tools > Gradle. Có thể dùng JBR đi kèm Android Studio.
4. Trong PowerShell, đặt JAVA_HOME tới JBR 25 (sửa đường dẫn nếu Android Studio cài ở vị trí khác), sau đó chạy:

    $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
    $env:Path = "$env:JAVA_HOME\bin;$env:Path"
    .\gradlew.bat assembleDebug
    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat lintDebug
    .\scripts\test-instrumentation.ps1 -DeviceSerial emulator-5554

## Chạy bộ kiểm thử A-06

Các script PowerShell trong `scripts/` kiểm tra JDK 25, Android SDK, ADB, emulator và số lượng test trong report. Log từng lần chạy nằm dưới `app/build/reports/a06/` (thư mục build bị Git bỏ qua). Instrumentation mặc định dùng runner trực tiếp để giữ app data.

    .\scripts\test-unit.ps1
    .\scripts\test-instrumentation.ps1 -DeviceSerial emulator-5554
    .\scripts\test-all.ps1 -DeviceSerial emulator-5554

Instrumentation chỉ chạy trên emulator. Mặc định script build APK rồi dùng `adb -s <serial> install -r` và AndroidJUnitRunner trực tiếp; thao tác này giữ app data. Khi có nhiều thiết bị online, bắt buộc chỉ định serial emulator. Thiết bị vật lý bị từ chối làm đích test. Có thể chọn `-UseGradleRunner` khi chỉ có một emulator và chấp nhận rằng `connectedDebugAndroidTest` của AGP gỡ APK app/test sau khi chạy, làm mất app data của AVD. Mọi lệnh dùng ADB server chuẩn 5037; không đổi cấu hình hệ thống. Không test bằng API key hoặc endpoint bên ngoài.

APK debug được tạo tại app/build/outputs/apk/debug/app-debug.apk.

## Cài và mở APK

Bật USB debugging, kết nối thiết bị và chạy:

    adb devices -l
    adb install -r .\app\build\outputs\apk\debug\app-debug.apk
    adb shell monkey -p com.hoangtu.aistorystudio -c android.intent.category.LAUNCHER 1

Tiêu chí điện thoại thật chỉ được ghi PASS sau khi cài và mở trên thiết bị vật lý đang ở trạng thái device.

Instrumentation emulator không thay cho smoke test điện thoại thật của A-01.

## Lỗi NIO loopback trong shell Codex trên Windows

Trong shell Codex Windows hiện tại, Gradle/JVM có thể báo Unable to establish loopback connection khi dùng thư mục TEMP mặc định. Khi lỗi này xuất hiện, tạo thư mục tạm ngắn và đặt biến môi trường cho đúng cửa sổ PowerShell:

    New-Item -ItemType Directory -Path 'C:\jtmp' -Force | Out-Null
    $env:TEMP = 'C:\jtmp'
    $env:TMP = 'C:\jtmp'

Nếu Gradle vẫn không dùng JBR 25, đặt JAVA_HOME và cập nhật PATH như phần trên. Đây là cách khắc phục cho môi trường shell đã quan sát; không sửa source project.

## Bảo mật và Git

Không commit local.properties, API key, key.properties, secrets.properties, .env, tệp .jks/.keystore hoặc signing secrets. Không đưa khóa API vào mã nguồn hay log. Bản debug dùng debug keystore do Android tạo trên máy; cấu hình ký release thuộc D-06.

Quy tắc làm việc theo backlog nằm trong AGENTS.md; bằng chứng build và test được ghi trong docs/TASK_LOG.md.

Tên repository khởi tạo trên GitHub: AI_Story_Studio.
