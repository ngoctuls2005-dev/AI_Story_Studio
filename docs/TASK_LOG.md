# Nhật ký công việc

## A-01 — 18/10/2026

### Phạm vi và hiện trạng

- Đọc toàn bộ docs/BACKLOG_V1.md. Chỉ thực hiện nền tảng A-01 và các tài liệu người dùng yêu cầu; không thêm màn hình, Navigation, Room hay tính năng Roleplay/Novel.
- Dự án là một Android app module :app với scaffold Compose mẫu. Chưa có tính năng sản phẩm, Room, DataStore, Navigation hoặc provider AI.
- applicationId là com.hoangtu.aistorystudio; minSdk 26, targetSdk 37.
- Android SDK Platform 37, Build Tools 36.0.0, Android Platform Tools có ADB. local.properties có trên máy và không được theo dõi bởi Git.
- Android Gradle Plugin 9.4.1, Gradle Wrapper 9.6.0, Kotlin Compose plugin 2.2.10, Compose BOM 2026.02.01; Compose bật và Material 3 đã có.
- Gradle daemon JDK 25 được ghim trong gradle/gradle-daemon-jvm.properties. Android Studio JBR trên máy là OpenJDK 25.0.3. JDK mặc định trên PATH là Java 27; Gradle 9.6 chưa công bố hỗ trợ Java 27 làm JVM chạy daemon. Build dùng JBR 25 và TEMP/TMP ngắn tại C:\jtmp.
- Kiểm tra tương thích: AGP 9.4 yêu cầu Gradle 9.6, hỗ trợ API tối đa 37 và Build Tools 36.0.0; các phiên bản project khớp. AGP 9 có Kotlin tích hợp với KGP 2.2.10; Kotlin Compose plugin 2.2.10 cùng phiên bản. Build và test thực tế xác nhận cấu hình hiện tại biên dịch được. Java source/target 11 là mức bytecode riêng, không phải JDK chạy Gradle.
- Không nâng thư viện hoặc plugin; không có lỗi build cần thay đổi toolchain trong phạm vi A-01. Room tiếp tục để A-03.

### File tạo/sửa

- Tạo AGENTS.md với quy tắc backlog, bảo mật, comment code, kiểm thử và ghi bằng chứng.
- Tạo docs/ARCHITECTURE.md mô tả hiện trạng và kiến trúc đích; ghi rõ phần đích chưa triển khai.
- Tạo docs/ROADMAP.md với trạng thái thực tế, cổng backlog và thứ tự đề xuất.
- Tạo docs/TEST_PLAN.md với test A-01, test mẫu hiện có và ma trận kiểm thử V1.0.
- Tạo docs/DECISIONS.md với quyết định kiến trúc dựa trên yêu cầu và backlog; đánh dấu phần chưa triển khai.
- Cập nhật README.md sang tiếng Việt có dấu, hướng dẫn môi trường build và bảo mật.
- Cập nhật .gitignore để bỏ qua thư mục .kotlin do Kotlin Gradle plugin tự tạo trong quá trình build.
- Cập nhật docs/TASK_LOG.md để lưu bằng chứng của lượt này.
- Không sửa mã ứng dụng hay thêm thư viện. Người dùng yêu cầu các tài liệu kiến trúc trong A-01; đây là phần tài liệu được làm sớm, không đánh dấu A-06 hoàn tất vì cấu hình test runner/Compose test/MockWebServer của A-06 chưa làm.

### Lệnh và kết quả

Các lệnh Gradle chạy trong PowerShell với TEMP/TMP=C:\jtmp, JAVA_HOME trỏ tới Android Studio JBR 25 và Gradle daemon JDK 25 theo cấu hình project.

| Lệnh/kịch bản | Kết quả |
|---|---|
| gradlew.bat assembleDebug testDebugUnitTest --rerun-tasks | PASS — BUILD SUCCESSFUL; 42 actionable tasks executed. Tạo APK debug 12,052,400 bytes; SHA-256 23869500E361D2F64A6476D17BA1BE260FC89598C6547A59D8CE5CC59F1E26EC. Gradle cảnh báo không thể strip libandroidx.graphics.path.so và đóng gói nguyên thư viện. |
| Test unit trong lệnh trên | PASS — 1 test, 0 failure, 0 error; đây là test mẫu phép cộng, chưa kiểm tra nghiệp vụ ứng dụng. XML: app/build/test-results/testDebugUnitTest/TEST-com.hoangtu.aistorystudio.ExampleUnitTest.xml. |
| gradlew.bat lintDebug | PASS — BUILD SUCCESSFUL, 10 cảnh báo lint, 0 lỗi. Báo cáo: app/build/reports/lint-results-debug.html và app/build/reports/lint-results-debug.sarif. Cảnh báo gồm nhãn thừa, gợi ý dependency và tài nguyên chưa dùng trong scaffold. |
| gradlew.bat connectedDebugAndroidTest | PASS — 1 test, 0 failure, 0 error trên Medium_Phone_API_37.0 (emulator-5554), API 37. XML: app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17.xml. Test mẫu kiểm tra package context. Có cảnh báo deprecated Unsafe từ protobuf trong quá trình chạy; test không thất bại. |
| adb install -r app/build/outputs/apk/debug/app-debug.apk | PASS trên emulator — ADB trả Success. |
| adb shell am start -W -n com.hoangtu.aistorystudio/.MainActivity | PASS trên emulator — Status: ok, Activity MainActivity được resume. Lọc 500 dòng logcat gần nhất không thấy FATAL EXCEPTION của package. |
| adb devices -l | BLOCKED cho tiêu chí điện thoại thật — chỉ có emulator API 37 online; không có điện thoại vật lý kết nối. |
| git ls-files local.properties '*.jks' '*.keystore' '*secrets*' '.env*' | Không trả về file nào; các tệp cục bộ/bí mật đã kiểm tra không được theo dõi trong Git. |

### Điều kiện nghiệm thu và việc còn lại

- Build debug và test trên emulator đã được chạy lại trong lượt này; các kết quả trên là thực tế. Bộ unit/instrumentation hiện tại chỉ là test mẫu, không chứng minh tính năng ứng dụng.
- APK đã cài và mở trên emulator API 37. Tiêu chí A-01 yêu cầu mở trên điện thoại thật; chưa đạt vì không có điện thoại vật lý online trong ADB. Vì vậy A-01 **chưa đạt đầy đủ**.
- Lint có 10 cảnh báo mức warning nhưng không có lỗi; chưa nâng dependency chỉ để loại bỏ gợi ý phiên bản mới.
- A-02 chưa triển khai. Đề xuất sau khi hoàn tất smoke test trên điện thoại thật: A-02, tạo khung điều hướng và sáu màn hình theo backlog. Sau đó A-03 cần được ưu tiên để thiết lập schema Room và migration an toàn trước tính năng dữ liệu.

## A-02 — 18/10/2026

### Phạm vi, kiểm tra trước khi sửa và chia nhỏ

- Đọc AGENTS.md, toàn bộ docs/BACKLOG_V1.md, docs/ARCHITECTURE.md, docs/ROADMAP.md, docs/DECISIONS.md, docs/TEST_PLAN.md và nhật ký này.
- Backlog A-02 yêu cầu Material 3, Dark Mode, điều hướng sáu màn Home/Roleplay/Novel/Characters/Library/Settings; nghiệm thu bằng chuyển qua sáu màn, rời app rồi quay lại đúng trạng thái và không crash.
- Git sạch trên nhánh main tại checkpoint A-01 `a286729` trước khi bắt đầu. A-01 còn thiếu mở APK trên điện thoại thật. ADB có emulator API 37 nhưng không có thiết bị Android vật lý.
- Chia việc thành: (1) theme và shell edge-to-edge; (2) NavHost, Back và lưu/khôi phục đích chính; (3) sáu màn hình placeholder và thành phần dùng chung; (4) kiểm thử instrumentation, smoke và viewport thích ứng.
- Không thêm Room, DataStore, provider AI, memory, chat/editor thực tế hay thay đổi toolchain. Chỉ thêm Navigation Compose 2.10.2 và Material Icons Core; icon dùng Compose BOM sẵn có.

### Thay đổi

- Thay scaffold mẫu bằng composition root `MainActivity` gọi `StudioApp`; bổ sung theme Material 3 sáng/tối theo hệ thống và màu động trên API 31 trở lên.
- Thêm `StudioDestination`, một NavHost, NavigationBar cho màn hình hẹp và NavigationRail từ 600 dp; lưu/khôi phục state đích chính, bật `launchSingleTop`, để system Back đi theo back stack. Shell dùng safe drawing insets và `imePadding`; Activity giữ `adjustResize`.
- Tạo thành phần dùng chung và sáu màn hình tiếng Việt có Unicode, trạng thái rỗng/thông tin và liên kết điều hướng cơ bản. Nội dung là placeholder, chưa có nghiệp vụ.
- Thêm kiểm thử UI điều hướng/theme. Cập nhật README và các tài liệu hiện trạng, quyết định, kế hoạch test và lộ trình để không mô tả scaffold cũ.

### File tạo hoặc chỉnh sửa

- Sửa: `README.md`, `app/build.gradle.kts`, `app/src/main/java/com/hoangtu/aistorystudio/MainActivity.kt`, `app/src/main/java/com/hoangtu/aistorystudio/ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `gradle/libs.versions.toml`, `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `docs/TEST_PLAN.md`, `docs/DECISIONS.md`, `docs/TASK_LOG.md`.
- Tạo: `app/src/main/java/com/hoangtu/aistorystudio/core/navigation/StudioApp.kt`, `StudioDestination.kt`, `core/ui/StudioComponents.kt`; sáu màn `feature/home/HomeScreen.kt`, `feature/roleplay/RoleplayScreen.kt`, `feature/novel/NovelScreen.kt`, `feature/characters/CharactersScreen.kt`, `feature/library/LibraryScreen.kt`, `feature/settings/SettingsScreen.kt`; instrumentation tests `StudioNavigationInstrumentedTest.kt` và `StudioThemeInstrumentedTest.kt`.

### Build, test và bằng chứng

Các lệnh Gradle chạy bằng Gradle Wrapper với Android Studio JBR 25 và TEMP/TMP=`C:\jtmp`.

| Lệnh/kịch bản | Kết quả |
|---|---|
| `gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain` | PASS — `BUILD SUCCESSFUL`; 51 actionable tasks, 3 executed và 48 up-to-date. APK debug: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `E48E90F8F83AED89EE40D5CDF29219938CC2CD4D81F089A9BAC9E2EFBEA6C86B`. |
| Unit test hiện có | PASS — 1 test, 0 failure/error. XML: `app/build/test-results/testDebugUnitTest/TEST-com.hoangtu.aistorystudio.ExampleUnitTest.xml`. Đây là test mẫu phép cộng, không kiểm tra nghiệp vụ. |
| `gradlew.bat connectedDebugAndroidTest --console=plain` | PASS — 6 test, 0 failure/error/skip trên Medium Phone API 37 (`emulator-5554`): 1 context mẫu, 4 navigation và 1 theme. XML: `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17.xml`. |
| `gradlew.bat lintDebug` (trong lệnh kết hợp trên) | PASS — task thành công, 10 warning và 0 lỗi. Report: `app/build/reports/lint-results-debug.html` và `.sarif`. Warning gồm nhãn manifest dư, gợi ý phiên bản BOM/Kotlin mới hơn và 7 màu scaffold chưa dùng; không nâng thư viện chỉ để xóa gợi ý. |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` | PASS trên emulator — ADB trả `Success`. |
| `adb shell am start -W -n com.hoangtu.aistorystudio/.MainActivity` | PASS trên emulator — `Status: ok`, mở được MainActivity. |
| Smoke thủ công | PASS trên emulator — điều hướng tới Novel; `uiautomator` xác nhận marker “Chưa có dự án nào”; gửi KEYCODE_HOME, đưa Activity về foreground rồi xác nhận marker Novel vẫn còn. Ảnh: `app/build/ui-smoke/home-a02.png`, `novel-a02.png`, `resumed-a02.png`. |
| Viewport rộng | PASS trên emulator — tạm đặt kích thước 2400×2400, xác nhận NavigationRail và nội dung Novel không tràn ngang; ảnh `app/build/ui-smoke/wide-a02.png`. Đã reset về kích thước 1080×2400. |
| `adb devices -l` | Chỉ có emulator `emulator-5554`; không có điện thoại vật lý kết nối. |

Lệnh ADB được gọi qua `C:\Users\HOANG NGOC TU\AppData\Local\Android\Sdk\platform-tools\adb.exe`: `devices -l`, `install -r`, `shell am start -W`, `shell input tap 445 2275`, `shell input keyevent 3`, `shell uiautomator dump`, `shell screencap -p` và `shell wm size 2400x2400`/`wm size reset`. Lần mở Activity trước khi cài APK trả `Error type 3`; sau khi cài APK mới, lệnh mở trả `Status: ok` và smoke hoàn tất.

### Kết luận nghiệm thu

- **A-02 đạt trên emulator API 37:** sáu màn điều hướng, Back về Home, giữ màn sau khi tạo lại Activity và sau khi đưa app nền/quay lại đều đã có test/kịch bản chạy thực tế; không crash trong các lượt này. Theme sáng/tối, tiếng Việt và viewport rộng đã được kiểm tra.
- Chưa có ô nhập liệu trong phạm vi A-02, nên hành vi bàn phím chưa thể kiểm chứng bằng tương tác. `imePadding` và `adjustResize` đã cấu hình để các task sau tích hợp trường nhập liệu.
- **A-01 vẫn chưa đạt đầy đủ:** chưa smoke APK trên điện thoại thật vì không có thiết bị vật lý online; bằng chứng emulator không thay thế điều kiện này.
- Cảnh báo còn lại: 10 warning lint nêu trên; instrumentation hiển thị cảnh báo Java về `sun.misc.Unsafe` từ protobuf 3.25.5 nhưng không làm test thất bại. Không có lỗi build/test chưa giải quyết.
- Đề xuất bước tiếp theo: hoàn tất smoke A-01 trên điện thoại thật để đóng cổng nền tảng; sau đó thực hiện A-03 (schema Room, repository, CRUD và migration an toàn). Không triển khai A-03 trong lượt này.
