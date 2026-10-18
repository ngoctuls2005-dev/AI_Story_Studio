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
