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

## A-03 — 18/10/2026

### Phạm vi và kiểm tra trước khi sửa

- Đọc toàn bộ AGENTS.md và docs/BACKLOG_V1.md, ARCHITECTURE.md, DECISIONS.md, ROADMAP.md, TEST_PLAN.md, TASK_LOG.md. Backlog A-03 yêu cầu Room schema V1, FK/index và repository/transaction cho nhân vật, persona, chat, message, variants, novel, chapter, scene, revisions, memory, summary, timeline; tiêu chí là CRUD/FK, persistence sau đóng/mở, ID không trùng và rollback transaction.
- Git sạch trên nhánh main tại checkpoint A-02 `f2b473385076ffd6416e2d3a6ab3a66fb048c227` trước khi làm. Giữ nguyên checkpoint này.
- A-01 vẫn thiếu smoke trên điện thoại thật; không được đánh dấu hoàn thành. Trong lượt này ADB nhận Xiaomi 21061119AG / API 33, nhưng thiết bị từ chối cài APK với `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`. Kiểm thử A-03 được chuyển qua emulator API 37 bằng ADB server riêng để không thay đổi thiết lập hay dữ liệu trên điện thoại.
- Toolchain hiện tại: AGP 9.4.1, Gradle 9.6.0, Kotlin Compose plugin 2.2.10, Gradle daemon/Android Studio JBR 25, Android SDK 37 và Build Tools 36.0.0. Dùng Room 2.8.5 + KSP 2.3.12; không nâng AGP/Gradle/Kotlin/SDK.
- Chia A-03 thành: (1) kiểm tra tương thích Room/KSP và export schema; (2) Entity V1, FK/index và quy tắc xóa; (3) DAO/repository với Coroutines/Flow/transaction; (4) test CRUD, scope, persistence, FK, rollback và MigrationTestHelper; (5) cập nhật tài liệu, chạy lại kiểm thử và checkpoint.

### Thiết kế và thay đổi

- Tạo `StudioDatabase` version 1, tên file cục bộ `ai_story_studio.db`, `exportSchema=true`, singleton builder có registry migration rỗng và không dùng `fallbackToDestructiveMigration`/`clearAllTables`.
- Tạo 14 Entity có UUID String làm khóa chính: `CharacterProfile`, `UserPersona`, `Conversation`, `ChatMessage`, `MessageVariant`, `ConversationSummary`, `NovelProject`, `NovelChapter`, `NovelScene`, `SceneRevision`, `NovelCharacterCrossRef`, `MemoryFact`, `NovelSummary`, `TimelineEvent`.
- FK dùng RESTRICT khi xóa character/persona còn được conversation tham chiếu; CASCADE cho dữ liệu con thuộc conversation/chapter/project; liên kết novel-character bị xóa theo một trong hai owner. Index hỗ trợ tìm kiếm, owner scope và thứ tự; message sequence, variant order, revision number, liên kết cặp project-character và timeline order được ràng buộc duy nhất ở các trường phù hợp.
- Memory fact có 3 FK owner nullable; repository bắt buộc đúng một owner khớp `ownerType`, kiểm tra FK bởi SQLite và chặn thay đổi owner của bản ghi đã tạo. Xóa memory đặt tombstone `deleted_at`, không xóa vật lý; query thông thường loại fact đã xóa. Query luôn giới hạn theo khóa owner tương ứng. Không lưu API key trong schema.
- Tạo bốn DAO: `CharacterDao`, `ConversationDao`, `NovelDao`, `MemoryFactDao`; các ghi/đọc một lần là `suspend`, các luồng quan sát trả `Flow`. Tạo bốn repository tương ứng. `withTransaction` cấp sequence/order và bảo vệ thao tác nhiều bước; message/summary/memory không được đổi owner khi cập nhật qua repository.
- Room plugin xuất schema có thể theo dõi tại `app/schemas/com.hoangtu.aistorystudio.core.database.StudioDatabase/1.json`; Room schema folder được plugin đưa vào assets của androidTest. Chưa có schema V2/migration thật nên DB version vẫn 1; `StudioDatabaseMigrations.ALL` chỉ đăng ký các migration thật hiện có và đang rỗng. Không tạo migration 1→2 giả. MigrationTestHelper tạo và validate schema V1; cách làm V1→V2 có fixture bảo toàn dữ liệu được ghi trong TEST_PLAN.
- Tương thích runtime test phát hiện Room MigrationTestHelper 2.8.5 dùng serialization core 1.8.1, trong khi Navigation/Lifecycle giải quyết xuống 1.7.3; lần chạy đầu lỗi `AbstractMethodError`. Dependency insight xác nhận xung đột. Thêm dependency core 1.8.1 để căn phiên bản toàn bộ variant; chạy lại MigrationTestHelper thành công. Đây là thay đổi hẹp cần cho test schema, không đổi Compose/Navigation/toolchain.
- Không sửa Navigation, Composable, DataStore, AI provider, streaming, Memory Engine, backup hoặc signing. Không thao tác DB chính của ứng dụng; bài reopen tạo DB test có tên UUID rồi đóng/mở và chỉ xóa chính file test đó.

### File tạo hoặc chỉnh sửa

- Sửa `app/build.gradle.kts`, `gradle/libs.versions.toml`.
- Tạo `app/schemas/com.hoangtu.aistorystudio.core.database.StudioDatabase/1.json`.
- Tạo DB/migration: `app/src/main/java/com/hoangtu/aistorystudio/core/database/StudioDatabase.kt`, `StudioDatabaseMigrations.kt`.
- Tạo 14 Entity trong `core/database/entity/`: `CharacterProfileEntity.kt`, `UserPersonaEntity.kt`, `ConversationEntity.kt`, `ChatMessageEntity.kt`, `MessageVariantEntity.kt`, `ConversationSummaryEntity.kt`, `NovelProjectEntity.kt`, `NovelChapterEntity.kt`, `NovelSceneEntity.kt`, `SceneRevisionEntity.kt`, `NovelCharacterCrossRefEntity.kt`, `MemoryFactEntity.kt`, `NovelSummaryEntity.kt`, `TimelineEventEntity.kt`.
- Tạo DAO trong `core/database/dao/`: `CharacterDao.kt`, `ConversationDao.kt`, `NovelDao.kt`, `MemoryFactDao.kt`.
- Tạo repository trong `core/database/repository/`: `CharacterRepository.kt`, `ConversationRepository.kt`, `NovelRepository.kt`, `MemoryFactRepository.kt`.
- Tạo instrumentation tests: `app/src/androidTest/java/com/hoangtu/aistorystudio/core/database/A03RoomInstrumentedTest.kt`, `A03MigrationInstrumentedTest.kt`.
- Cập nhật `README.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/ROADMAP.md`, `docs/TEST_PLAN.md`, `docs/TASK_LOG.md`.

### Lệnh và kết quả

Gradle chạy bằng Wrapper với `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` (JBR 25), `TEMP/TMP=C:\jtmp`. ADB ở `C:\Users\HOANG NGOC TU\AppData\Local\Android\Sdk\platform-tools\adb.exe`.

| Lệnh/kịch bản | Kết quả |
|---|---|
| `gradlew.bat help --console=plain` sau khi thêm Room/KSP/plugin | PASS — Gradle cấu hình và resolve plugin Room/KSP thành công. |
| `gradlew.bat :app:kspDebugKotlin --console=plain` | PASS — KSP sinh DAO và `copyRoomSchemas` sinh schema V1. |
| `gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain` | PASS — `BUILD SUCCESSFUL`; 1 unit test mẫu, 0 failure/error. Báo cáo unit: `app/build/test-results/testDebugUnitTest/TEST-com.hoangtu.aistorystudio.ExampleUnitTest.xml`. APK: `app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `26BF0A75BEB45766BD1C885842AC239BF0C71437ECBAB49C87408EA4CE451C60`. |
| `gradlew.bat connectedDebugAndroidTest --console=plain` với ADB mặc định (Xiaomi API 33 và emulator cùng online) | BLOCKED trên Xiaomi: Android từ chối cài instrumentation APK (`INSTALL_FAILED_USER_RESTRICTED`). Không kết luận đây là lỗi test Room và không thay đổi tùy chọn bảo mật điện thoại. |
| `gradlew.bat connectedDebugAndroidTest --console=plain` với `ANDROID_ADB_SERVER_PORT=5038` | PASS — ADB server cô lập chỉ thấy `Medium_Phone_API_37.0(AVD) - 17`; 14 test, 0 failure/error/skip. XML: `app/build/outputs/androidTest-results/connected/debug/TEST-Medium_Phone_API_37.0(AVD) - 17.xml`. Bao gồm 6 test A-03 (CRUD, Unicode, persistence, scope/thứ tự, memory, FK/delete/rollback) và 2 test migration/schema; 6 test A-02 hiện hữu cũng chạy lại. |
| `gradlew.bat lintDebug` (trong lệnh build/test kết hợp) | PASS — 0 lỗi, 11 warning. HTML/text: `app/build/reports/lint-results-debug.html`, `app/build/reports/lint-results-debug.txt`. Warning là redundant manifest label, gợi ý phiên bản mới hơn và resource scaffold chưa dùng; không có lỗi lint mới ở database. |
| `adb devices -l` | Có Xiaomi `21061119AG` API 33 và emulator API 37. ADB server riêng port 5038 chỉ chạy emulator để Gradle test không thử cài lên điện thoại. |

Trong quá trình sửa, build ban đầu bắt lỗi dùng method reference tới hàm `suspend` trong `also`; chuyển sang lời gọi suspend tường minh trong transaction. MigrationTestHelper ban đầu lỗi binary incompatibility nêu trên; căn serialization core 1.8.1 và chạy lại toàn suite đã pass. Gradle log có cảnh báo protobuf gọi `sun.misc.Unsafe` bị deprecated và thư viện native `libandroidx.graphics.path.so` được đóng gói nguyên vì không strip được; không làm test thất bại.

### Kết luận nghiệm thu A-03 và giới hạn dữ liệu

- **A-03 đạt theo tiêu chí backlog trên emulator API 37:** create/read/update/delete, Unicode, persistence sau đóng/mở, scope hai hội thoại/hai project, uniqueness/FK, thứ tự ổn định, CASCADE/RESTRICT và rollback đều có assertion instrumentation chạy thành công; database hiện tại mở theo schema V1 và MigrationTestHelper xác nhận schema được export có thể tạo/validate.
- A-01 vẫn chưa đạt đầy đủ do điện thoại Xiaomi từ chối cài APK. Đây không làm thay đổi trạng thái nghiệm thu A-03; không đánh dấu A-01 hoàn thành.
- Rủi ro còn lại: chưa có migration V1→V2 thật vì chưa có schema mới; task D-01 sau A-03 cần tạo fixture V1, migration thật và so sánh toàn bộ dữ liệu. Dữ liệu cục bộ vẫn có thể mất khi gỡ app/xóa app data/hỏng thiết bị; backup/restore thuộc các task D và chưa triển khai. Không có cơ chế migration phá hủy hoặc lệnh reset DB trong app.
- Đề xuất tiếp theo: hoàn tất smoke test A-01 trên điện thoại thật sau khi người dùng cho phép cài APK qua USB; sau đó A-04 theo trình tự backlog. Không thực hiện task kế tiếp trong lượt A-03 này.

## Đồng bộ GitHub sau A-03 — 18/10/2026

### Kiểm tra lịch sử và hợp nhất

- Working tree sạch trước thao tác; nhánh local `main` ở `ac5f623` (A-03), với các checkpoint A-01 `31b206e`, `a286729` và A-02 `f2b4733`.
- Thêm `origin` với URL đích do người dùng cung cấp và `git fetch origin` thành công. `origin/main` trỏ tới `e1fa742` (Initial commit), chỉ gồm README và `.gitignore`; không có merge-base với lịch sử local (`git rev-list --left-right --count main...origin/main` trả `4 1`).
- Hợp nhất bằng `git merge origin/main --allow-unrelated-histories`. README và `.gitignore` có xung đột add/add; giải quyết bằng cách giữ README Android hiện tại và ghi nhận tên repository khởi tạo, đồng thời ghép toàn bộ quy tắc ignore GitHub với quy tắc Android. Không xóa file nguồn hoặc viết lại lịch sử.

### Bảo mật và artifact

- Rà soát 86 đường dẫn được theo dõi cùng nội dung hiện tại và patch lịch sử local chưa có trên remote. Không phát hiện đường dẫn thuộc nhóm local.properties, .env, khóa ký, APK/AAB, database người dùng, build hoặc cache; không phát hiện chuỗi khớp mẫu API/token/private-key. Giá trị nhạy cảm không được in ra terminal.
- Xác nhận Room schema cần quản lý phiên bản vẫn được theo dõi tại `app/schemas/com.hoangtu.aistorystudio.core.database.StudioDatabase/1.json`.
- Chưa chạy instrumentation test trong lượt đồng bộ này; bằng chứng A-03 gần nhất vẫn là 14 test trên emulator API 37 như mục A-03 ở trên. A-01 vẫn thiếu smoke test trên điện thoại thật và không được đánh dấu hoàn thành.

### Build, test và quy trình đồng bộ

- Toolchain chạy với Android Studio JBR 25; Android Gradle Plugin/Gradle/Kotlin/SDK giữ nguyên. `adb devices -l` nhận điện thoại Xiaomi API 33; không cài APK trong lượt đồng bộ.
- `gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain`: BUILD SUCCESSFUL, 53 actionable tasks (8 executed, 45 up-to-date). `lintDebug` hoàn tất với 0 lỗi, 11 warning. Unit test ban đầu là up-to-date nên chạy thêm lệnh bên dưới để có lần thực thi mới.
- `gradlew.bat testDebugUnitTest --rerun-tasks --console=plain`: BUILD SUCCESSFUL, 26 tasks executed; XML `app/build/test-results/testDebugUnitTest/TEST-com.hoangtu.aistorystudio.ExampleUnitTest.xml` ghi nhận 1 test, 0 failure, 0 error, 0 skipped (test mẫu phép cộng).
- Tạo `docs/GIT_WORKFLOW.md` và cập nhật `AGENTS.md` để quy định thứ tự hoàn thành → kiểm thử → TASK_LOG → commit → rà soát bảo mật → push có yêu cầu → xác minh remote. Chưa coi commit local là bằng chứng push thành công.
- `git push -u origin HEAD:main` hoàn tất thành công; GitHub nhận merge commit `e5aefa64c98b94128e3dbfe22789584aa5b5a449` trên `main`.
- Sau push, `git fetch origin` và `git ls-remote origin refs/heads/main` xác nhận remote SHA bằng local HEAD. Kiểm tra `origin/main` xác nhận A-01 (`31b206e`, `a286729`), A-02 (`f2b4733`) và A-03 (`ac5f623`) vẫn là ancestor; Android Manifest/MainActivity, Room schema V1, README, AGENTS.md và các tài liệu kiến trúc/test/task log đều có trong remote tree.

## A-04 — 18/10/2026

### Phạm vi và kiểm tra trước khi sửa

- Đọc toàn bộ `AGENTS.md`, `docs/BACKLOG_V1.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/TEST_PLAN.md`, `docs/TASK_LOG.md` và `docs/GIT_WORKFLOW.md`. Backlog xác định A-04 P0: `AiProvider`, fake stream offline có độ trễ/lỗi chủ động; mô phỏng thành công/200, 401, 429, 500, timeout và ngắt stream.
- Working tree sạch trên `main` tại checkpoint GitHub `12fcb85857c81ec4f6de2c25b4930b8b7aa2ee19`; A-01/A-02/A-03 vẫn giữ nguyên. Không push trong nhiệm vụ này.
- A-01 vẫn chưa đủ điều kiện nghiệm thu: chưa có smoke test cài và mở APK trên điện thoại thật. ADB nhận Xiaomi API 33 nhưng không cài/mở APK trong lượt này.
- Toolchain: AGP 9.4.1, Gradle Wrapper 9.6.0, Kotlin Compose plugin 2.2.10, SDK 37, Gradle daemon JDK 25. JDK PATH mặc định là 27; dùng Android Studio JBR 25 và `TEMP/TMP=C:\jtmp` khi build. Giữ nguyên AGP/Gradle/Kotlin/SDK.
- Coroutines core được giải quyết sẵn về 1.9.0 trong runtime classpath. Thêm khai báo trực tiếp core và test 1.9.0 để provider có API Flow trực tiếp và unit test điều khiển thời gian; không nâng phiên bản runtime đã resolve.
- Chia việc: (1) mô hình request/event/lỗi domain; (2) fake scenarios cố định, streaming và delay; (3) kiểm thử scheduler, cancellation, timeout, lỗi và đồng thời; (4) cập nhật kiến trúc/test plan/task log; (5) chạy build, unit, lint và instrumentation regression.

### Thiết kế và thay đổi

- `AiRequest` mang danh sách lịch sử `SYSTEM`/`USER`/`ASSISTANT`, dùng chung được cho Roleplay và Novel, không có trường provider/model/API key.
- `AiProvider` nằm trong `core.ai`, độc lập UI/HTTP và trả `Flow<AiStreamEvent>`. Sự kiện gồm `TextDelta`, `Completed` và `Failed`; lỗi domain phân biệt Unauthorized, RateLimited, ServerError, Timeout, Interrupted và EmptyResponse, không chứa HTTP response thô. Cancellation của coroutine không bị bắt hoặc chuyển thành lỗi.
- `FakeAiProvider` chọn scenario mặc định hoặc theo `AiRequest`; mặc định trả nội dung tiếng Việt cố định. Hỗ trợ thành công, 401, 429, 500, timeout có delay ảo, ngắt stream sau partial và phản hồi rỗng. Độ trễ giữa chunk dùng `delay`, có thể điều khiển bằng `TestCoroutineScheduler`; fake không dùng OkHttp hay gọi mạng.
- Chưa tích hợp provider vào Chat UI hoặc Novel Editor vì các consumer đó chưa có nghiệp vụ; không tuyên bố hai màn chạy offline.

### File tạo hoặc chỉnh sửa

- Tạo `app/src/main/java/com/hoangtu/aistorystudio/core/ai/AiProvider.kt` và `FakeAiProvider.kt`.
- Tạo `app/src/test/java/com/hoangtu/aistorystudio/core/ai/FakeAiProviderTest.kt` với 8 test cho Unicode/ghép chunk/completion một lần, lỗi 401/429/500/rỗng, timeout ảo, ngắt stream, delay giữa chunk, cancellation và hai request đồng thời.
- Sửa `app/build.gradle.kts` và `gradle/libs.versions.toml` để khai báo coroutines core/test 1.9.0.
- Cập nhật `docs/ARCHITECTURE.md`, ADR-004 trong `docs/DECISIONS.md`, và thêm mục A-04 vào `docs/TEST_PLAN.md`.
- Tạo report instrumentation thủ công trong thư mục build bị ignore: `app/build/outputs/androidTest-results/connected/debug/manual-instrumentation-a04.txt`.

### Build, test và bằng chứng

Gradle chạy bằng Wrapper với Android Studio JBR 25 và `TEMP/TMP=C:\jtmp`. ADB dùng server riêng port 5038 để chỉ nhắm AVD, không cài test APK lên điện thoại Xiaomi.

| Lệnh/kịch bản | Kết quả |
|---|---|
| `gradlew.bat testDebugUnitTest --offline --rerun-tasks --console=plain` | PASS — `BUILD SUCCESSFUL`, 26 task thực thi; 9 test tổng cộng (8 A-04 + 1 test mẫu), 0 failure/error/skipped. Chạy offline sau khi dependency đã cache. XML: `app/build/test-results/testDebugUnitTest/TEST-com.hoangtu.aistorystudio.core.ai.FakeAiProviderTest.xml` và `TEST-com.hoangtu.aistorystudio.ExampleUnitTest.xml`. |
| `gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain` | PASS — `BUILD SUCCESSFUL`, 53 actionable tasks (5 executed, 48 up-to-date); APK debug build thành công. Unit task up-to-date trong lệnh kết hợp; lần chạy đầy đủ và mới nhất được xác nhận bằng lệnh offline ở trên. |
| `gradlew.bat lintDebug` (trong lệnh kết hợp) | PASS — 0 lỗi, 13 warning. Report: `app/build/reports/lint-results-debug.html` và `.txt`. Có 11 warning scaffold/toolchain đã có và 2 gợi ý nâng coroutines 1.9.0; giữ phiên bản 1.9.0 vì đang khớp runtime đã resolve và không có lỗi tương thích cần nâng cấp. |
| `gradlew.bat connectedDebugAndroidTest --console=plain` | BLOCKED ở khâu runner/output: Gradle báo `BUILD SUCCESSFUL` nhưng report tạo 0 test; worker không tìm thấy serial emulator qua ADB và không cài instrumentation APK. Không xem đây là PASS. Đã thử với AVD API 37 ở serial 5556 và 5554, gồm một lần `--no-daemon`. |
| Cài APK app + test lên AVD `Medium_Phone_API_37.0`, rồi `adb -s emulator-5554 shell am instrument -w -r com.hoangtu.aistorystudio.test/androidx.test.runner.AndroidJUnitRunner` | PASS — output trực tiếp `OK (14 tests)`, thời gian 18.61 giây; 0 failure/error. Gồm hồi quy điều hướng/theme A-02 và database/migration A-03. Log: `app/build/outputs/androidTest-results/connected/debug/manual-instrumentation-a04.txt`. A-04 provider được kiểm thử bằng unit test JVM, không có instrumentation riêng. |
| Rà soát test kết quả và diff | PASS — XML unit xác nhận 9 test, 0 failure/error/skipped; manual runner xác nhận 14 test; `git diff --check` không có lỗi whitespace ở diff nhiệm vụ. |

Lệnh Gradle `connectedDebugAndroidTest` hoàn tất với exit code 0 nhưng chưa tạo test result (Gradle HTML ghi 0 test, worker stderr báo thiết bị không tồn tại). Vì vậy đã chạy AndroidJUnitRunner trực tiếp qua ADB server đang thấy AVD và xác nhận kết quả thật là 14 test. Không thay đổi ADB hoặc cài đặt trên điện thoại thật.

### Kết luận nghiệm thu và giới hạn

- Các điều kiện provider-level của A-04 được kiểm thử offline: stream xác định, Unicode, event terminal, các lỗi, timeout theo virtual time, cancellation giữa stream, không phát thêm sau hủy và cách ly hai request đồng thời.
- A-04 đạt ở phạm vi abstraction/Fake/test harness. Điều kiện backlog về việc dùng thử trực tiếp Chat UI và Novel Editor hoàn toàn offline chưa thể kiểm chứng vì hai consumer chưa được triển khai; không tuyên bố hai màn đã chạy offline. Đây là giới hạn tích hợp được ghi nhận, không phải kiểm thử UI đạt.
- `connectedDebugAndroidTest` qua Gradle hiện cho report rỗng trong cấu hình ADB tách server; instrumentation vẫn được xác nhận bằng runner trực tiếp trên AVD. Cảnh báo `sun.misc.Unsafe` từ protobuf vẫn xuất hiện nhưng không làm 14 test thất bại.
- A-01 tiếp tục chưa hoàn thành vì chưa smoke test trên điện thoại thật. Không triển khai A-05, C-03, C-04 hoặc nhiệm vụ khác.

## A-05 — 19/10/2026

### Phạm vi và kiểm tra trước khi sửa

- Đọc toàn bộ `AGENTS.md`, `docs/BACKLOG_V1.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/TEST_PLAN.md`, `docs/TASK_LOG.md` và `docs/GIT_WORKFLOW.md`. Tiêu chí backlog A-05: cấu hình API theo provider/model, khóa không rõ trong DB/log/repository, dùng lại sau khởi động, có kiểm tra và xóa key.
- Git sạch trước khi làm trên `main`, HEAD là checkpoint A-04 `e1dac58`; `origin/main` vẫn ở `12fcb85`, local ahead 1 commit. Không sửa lịch sử A-01..A-04, không push.
- A-01 vẫn chưa hoàn thành: chưa có smoke test điện thoại thật. Mọi cài đặt instrumentation A-05 chỉ nhắm AVD API 37 bằng ADB server riêng; không cài test lên Xiaomi.
- Toolchain giữ nguyên: AGP 9.4.1, Gradle 9.6.0, Kotlin Compose plugin 2.2.10, SDK 37, Gradle daemon JDK 25 (Android Studio JBR). PATH mặc định Java 27 nên dùng JBR 25 và `TEMP/TMP=C:\jtmp`.
- DataStore chưa có; thêm AndroidX Preferences DataStore 1.2.1 theo tài liệu Android hiện hành và `lifecycle-viewmodel-ktx` cùng dòng version lifecycle 2.11.0 đang dùng. Không đổi AGP/Gradle/Kotlin/SDK, Room schema, Room dependency hoặc hợp đồng A-04.
- Chia nhỏ: (1) provider model/URL/parameter validation; (2) Android Keystore AES-GCM và DataStore Repository; (3) ViewModel, Settings UI và composition-root injection; (4) backup exclusion; (5) unit/instrumentation/process-restart tests; (6) build, lint, rà soát bảo mật, cập nhật tài liệu và checkpoint.

### Thiết kế và thay đổi

- Cấu hình provider gồm ID ổn định, tên, Base URL, model, temperature/top-p/max output tokens và cờ HTTP không mã hóa. Default là Experiential Labs, `https://api.experientiallabs.ai/v1`, `deepseek-v4.1-flash-uncensored`; không có API key mặc định. Provider tùy chỉnh dùng ID UUID; ID không sửa khi cập nhật.
- URL phải hợp lệ, có host, không có userinfo/query/fragment; HTTPS được chấp nhận mặc định. HTTP chỉ được lưu sau khi người dùng bật cờ xác nhận rủi ro, cờ mặc định false. Màn Settings chỉ kiểm tra cấu hình cục bộ và nói rõ chưa kiểm tra mạng; không có “test connection” giả.
- UI → `SettingsViewModel` → `AiProviderSettingsRepository` → Preferences DataStore/`SecureKeyStore`. API key nhập ở state Compose không dùng `rememberSaveable`, không hiển thị lại sau lưu, byte array đầu vào được xóa sau khi repository hoàn tất. Màn hình hỗ trợ thêm/sửa/chọn provider, show/hide, tham số, lưu và xóa key có xác nhận.
- Android Keystore sinh AES-256 key không xuất được riêng cho mỗi lần lưu; `AES/GCM/NoPadding`, IV ngẫu nhiên 96-bit, tag 128-bit, provider ID làm AAD. Preferences DataStore giữ IV/ciphertext/alias và metadata, không có field plaintext. Room schema không thay đổi.
- Khi thay key: mã hóa dưới alias mới → thay metadata/ciphertext trong một lần cập nhật DataStore → xóa alias cũ. Khi xóa: gỡ payload khỏi DataStore trước rồi xóa alias. Keystore không sẵn sàng, alias mất/invalidated hoặc tag sai trả mã lỗi đã làm sạch; trạng thái chuyển thành cần nhập lại, không crash/không tự xóa ciphertext hoặc nội dung truyện. Exception message và secret không được đưa vào UI/log.
- Nếu DataStore ghi bị lỗi/hủy sau khi alias mới tạo, không xóa alias đó vì không thể biết chắc transaction đã commit; trường hợp này có thể để alias Keystore mồ côi nhưng tránh ciphertext đã commit mất khóa. Xóa alias thất bại sau khi ciphertext đã xóa cũng có thể để alias mồ côi; UI báo trạng thái dọn dẹp.
- Đặt file DataStore riêng `files/datastore/ai_provider_settings.preferences_pb`, loại khỏi cloud backup, device-to-device transfer và legacy full backup. Chưa có file backup thủ công; task backup tương lai phải tiếp tục loại cấu hình provider/key.

### File tạo hoặc chỉnh sửa

- Tạo domain/settings: `app/src/main/java/com/hoangtu/aistorystudio/core/ai/settings/AiProviderConfiguration.kt`, `AiProviderSettingsRepository.kt`, `DataStoreAiProviderSettingsRepository.kt`.
- Tạo DataStore binding: `app/src/main/java/com/hoangtu/aistorystudio/core/datastore/AiProviderDataStore.kt`.
- Tạo bảo mật: `app/src/main/java/com/hoangtu/aistorystudio/core/security/SecureKeyStore.kt`, `AndroidKeyStoreSecureKeyStore.kt`.
- Tạo app wiring: `app/src/main/java/com/hoangtu/aistorystudio/StudioApplication.kt`, `feature/settings/SettingsViewModel.kt`, `SettingsViewModelFactory.kt`; cập nhật `MainActivity.kt`, `core/navigation/StudioApp.kt`, `feature/settings/SettingsScreen.kt`.
- Sửa `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `data_extraction_rules.xml`.
- Unit test: `app/src/test/java/com/hoangtu/aistorystudio/core/ai/settings/FakeSecureKeyStore.kt`, `AiProviderConfigurationTest.kt`, `DataStoreAiProviderSettingsRepositoryTest.kt`.
- Instrumentation: `app/src/androidTest/java/com/hoangtu/aistorystudio/A05CredentialRestartInstrumentedTest.kt`, `A05SecurityInstrumentedTest.kt`, `A05SettingsInstrumentedTest.kt`.
- Cập nhật `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` (ADR-005), `docs/ROADMAP.md`, `docs/TEST_PLAN.md` và nhật ký này. Không sửa `app/schemas/` hoặc Room.

### Build, test và bằng chứng

Gradle chạy bằng Wrapper/JBR 25. AVD `Medium_Phone_API_37.0` dùng ADB server port 5038 để không nhắm điện thoại Xiaomi.

| Lệnh/kịch bản | Kết quả |
|---|---|
| `gradlew.bat testDebugUnitTest --offline --rerun-tasks --console=plain` | PASS — `BUILD SUCCESSFUL`, 26 task thực thi; 20 test, 0 failure/error/skipped: 8 A-04, 4 URL/configuration A-05, 7 repository/DataStore A-05, 1 test mẫu. Chạy offline sau khi dependency đã cache. XML: `app/build/test-results/testDebugUnitTest/` gồm `FakeAiProviderTest.xml`, `AiProviderConfigurationTest.xml`, `DataStoreAiProviderSettingsRepositoryTest.xml`, `ExampleUnitTest.xml`. |
| `gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain` | PASS — lần chạy cuối trên trạng thái sẽ commit: `BUILD SUCCESSFUL`, 53 actionable tasks (3 executed, 50 up-to-date); tạo/giữ APK debug `app/build/outputs/apk/debug/app-debug.apk`. |
| `lintDebug` | PASS — 0 lỗi, 13 warning: nhãn manifest/resource scaffold, gợi ý version dependency cũ; không nâng cấp toolchain/dependency chỉ để loại warning. Báo cáo `app/build/reports/lint-results-debug.html`, `.sarif`, `.txt`. |
| `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --console=plain` | PASS — build APK app và instrumentation APK sau khi thêm test process restart. |
| `gradlew.bat connectedDebugAndroidTest --console=plain` với `ANDROID_ADB_SERVER_PORT=5038` | Không xác nhận PASS: Gradle in `BUILD SUCCESSFUL` nhưng worker báo không truy xuất được API level (`adb.exe: device 'emulator-5554' not found`) khi thu thập kết quả; thư mục report không có XML test đáng tin cậy. Không dựa vào exit code/build status này. |
| Cài hai APK vào AVD rồi `adb -P 5038 -s emulator-5554 shell am instrument -w -r com.hoangtu.aistorystudio.test/androidx.test.runner.AndroidJUnitRunner` | PASS — chạy lại sau chỉnh fixture cuối: output trực tiếp `OK (18 tests)`, 0 failure; gồm 4 test A-05 và 14 hồi quy A-02/A-03/mẫu. Bằng chứng: `app/build/outputs/androidTest-results/connected/debug/manual-instrumentation-a05.txt`. |
| Kiểm tra process restart thật trên AVD | PASS — chạy test `phase=seed`, `adb shell am force-stop com.hoangtu.aistorystudio`, mở lại bằng `adb shell monkey -p com.hoangtu.aistorystudio -c android.intent.category.LAUNCHER 1` (process PID sau mở là 4360), rồi chạy test `phase=verify`; test đọc đúng fixture bằng DataStore + Android Keystore và xóa key. Output seed/verify: `manual-process-seed-a05.txt`, `manual-process-verify-a05.txt` cùng thư mục trên. |
| Kiểm tra backup rule và lưu trữ | PASS — instrumentation parse rule cloud/device transfer/legacy backup; UI instrumentation lưu fixture, xác nhận raw DataStore file không chứa plaintext, Activity recreation giữ trạng thái, xác nhận xóa key. |

Lần instrumentation đầu tiên có một lỗi assertion trong test backup: expected string của test thiếu `domain=file`; XML rule production đã đúng. Đã sửa assertion, build lại và AndroidJUnitRunner sau sửa đạt 18/18. Sau đó đổi một fixture giả sang chuỗi trung tính để tránh scanner nhận diện nhầm mẫu key; build debug/unit/lint và runner 18/18 đã chạy lại trên trạng thái cuối. ADB/Gradle `connectedDebugAndroidTest` còn vấn đề thu thập report trên server riêng, nên runner trực tiếp là bằng chứng instrumentation được dùng.

Gradle phát cảnh báo protobuf DataStore gọi `sun.misc.Unsafe` đã deprecated; APK báo không strip được một số native library và đóng gói nguyên. Chúng không làm unit/instrumentation thất bại.

### Kết luận nghiệm thu và rủi ro

- Các tiêu chí provider cấu hình, URL HTTPS mặc định, lưu DataStore, AES-256-GCM/Android Keystore, không plaintext, cô lập nhiều provider, thay/xóa key, lỗi ciphertext/Keystore, backup rule và Settings cục bộ đã có test đạt trên JVM/AVD API 37.
- Kiểm thử seed → force-stop → mở lại ứng dụng → verify đã xác nhận sử dụng key sau khi process mới khởi động. Không dùng API key cá nhân, mạng hoặc backend.
- Rủi ro còn lại: API key plaintext hiện diện tạm trong bộ nhớ trong lúc nhập/giải mã và app/process được phép sử dụng Keystore key; thiết bị root hoặc process bị xâm nhập có thể gọi thao tác giải mã. Keystore mồ côi có thể còn nếu ghi/xóa metadata gặp lỗi; ciphertext không được giữ sống khi xóa được xác nhận. Chưa có backup thủ công, provider HTTP thật, SSE hoặc kết nối API (C-03/C-04 ngoài phạm vi).
- A-05 đạt trong phạm vi backlog và prompt. Không tuyên bố API provider kết nối thành công. A-01 vẫn chưa hoàn tất do thiếu smoke test điện thoại thật. Không triển khai A-06 hoặc C-03.

## A-06 — 19/10/2026

### Phạm vi và trạng thái trước khi sửa

- Đọc đầy đủ `AGENTS.md`, `docs/BACKLOG_V1.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/TEST_PLAN.md`, `docs/TASK_LOG.md` và `docs/GIT_WORKFLOW.md`; đọc cả yêu cầu A-06 được người dùng đính kèm. Tiêu chí backlog A-06: lệnh build/test lặp lại được, Codex có AGENTS, test runner/checklist, unit và Compose test cấu hình đúng, có MockWebServer.
- Git sạch trên `main`, HEAD `27e3aace` (`27e3aac`, checkpoint A-05). Lịch sử A-01..A-05 còn nguyên; `e1dac58` A-04 và `27e3aac` A-05 có trong lịch sử. Không sửa commit cũ, không push.
- A-01 vẫn chưa hoàn thành: thiếu smoke test điện thoại thật. Kiểm tra ADB trước và sau chạy chỉ thấy `emulator-5554`, AVD `Medium_Phone_API_37.0`, Android API 37; không có điện thoại vật lý online.
- Toolchain được giữ nguyên: JBR/JDK 25, Gradle Wrapper 9.6.0, AGP 9.4.1, Kotlin Compose plugin 2.2.10, Android SDK 37. ADB 37.0.1; AVD API 37. Không nâng cấp AGP/Gradle/Kotlin/SDK.
- Chia subtask: (1) đọc backlog/checkpoint và kiểm tra runner/device; (2) thêm MockWebServer và fixture dùng chung; (3) viết test HTTP cục bộ; (4) tạo script kiểm tra môi trường, chọn emulator, xác minh test count; (5) build/unit/lint/instrumentation, rà bảo mật; (6) cập nhật tài liệu và checkpoint.

### Nguyên nhân runner cũ và chính sách chạy mới

- TASK_LOG A-05 có bằng chứng `connectedDebugAndroidTest` từng báo `BUILD SUCCESSFUL` nhưng Gradle worker báo không tìm thấy `emulator-5554` và report không có test đáng tin cậy. Các lượt lỗi cũ dùng ADB server port 5038; A-06 chạy lại trên ADB server chuẩn/chung 5037, JBR 25 và `ANDROID_SERIAL=emulator-5554`, Gradle worker kết nối được và tạo report 18 test. Bằng chứng này cho thấy sự lệch ADB server port/tiến trình là nguyên nhân thực tế trong môi trường đó; không kết luận AGP runner luôn lỗi.
- Log `--info` của AGP xác nhận `connectedDebugAndroidTest` tự gỡ app APK và test APK khỏi AVD sau khi chạy. Lượt đó chỉ có emulator online, không có điện thoại thật. Có thể mất app data trên AVD khi uninstall. Vì vậy `test-instrumentation.ps1` mặc định chạy AndroidJUnitRunner trực tiếp theo serial đã xác minh là emulator, qua `adb -s ... install -r`; Gradle runner chỉ là tùy chọn `-UseGradleRunner`, giới hạn một emulator online và cảnh báo rõ hành vi uninstall. Runner trực tiếp không gỡ app hoặc xóa app data.
- Đường chọn nhiều thiết bị được cài đặt: khi nhiều thiết bị online cần chỉ định serial; serial phải là emulator (`ro.kernel.qemu=1`), không chọn điện thoại thật. Môi trường chỉ có một AVD nên nhánh nhiều thiết bị chưa được chạy thực tế.

### Thay đổi

- Thêm MockWebServer3/OkHttp 5.4.0 qua `testImplementation` để không thêm HTTP vào production. Fixture server có HTTP 200/400/401/429/500, rỗng, delay tùy chỉnh, timeout, đóng socket giữa response, UTF-8 và SSE raw chunk có `[DONE]`.
- Thêm fixture dữ liệu cố định, tái sử dụng cho Character, Persona, Conversation, ChatMessage, NovelProject, Chapter, Scene, MemoryFact, AI request/response và provider config. ID/timestamp xác định; không chứa dữ liệu người dùng hoặc API key.
- Thêm 5 test MockWebServer chạy HTTP loopback thật, cùng 2 test kiểm tra fixture. Test timeout xác minh `InterruptedIOException` trong giới hạn; response bị cắt phải ném `IOException`; teardown đóng OkHttp client và MockWebServer.
- Thêm `test-unit.ps1`, `test-instrumentation.ps1`, `test-all.ps1`, helper `Test-Common.ps1` và `test-harness.ps1`. Script xác minh JDK/SDK/ADB/serial emulator, kiểm tra exit code, chỉ đọc report mới, từ chối 0 test/failure/error/skip và xác minh `OK (N tests)` cùng status count của AndroidJUnitRunner. Có chế độ unit `-Offline`. Không chạy trên thiết bị vật lý, không gọi `pm clear`/uninstall và không đổi cấu hình ADB hệ thống.
- Cô lập hồi quy bảo mật A-05: `A05SettingsInstrumentedTest` snapshot/khôi phục cấu hình và key provider mặc định kể cả khi assertion lỗi; `A05CredentialRestartInstrumentedTest` dùng Preferences DataStore tạm trong cache cùng Android Keystore thật. Script chạy seed/force-stop/launch/verify qua process thật, lọc đúng test class, rồi xóa key và tệp test.
- Cập nhật `AGENTS.md`, README, kiến trúc, quyết định ADR-012 và TEST_PLAN về dependency test-only, cách chọn device, server port, số test, giới hạn runner và hành vi uninstall của AGP.

### Build và kiểm thử

| Lệnh/kịch bản | Kết quả quan sát được |
|---|---|
| `scripts/test-harness.ps1` | PASS — chấp nhận fixture 1 test; chủ động từ chối JUnit 0 test, JUnit có skip, AndroidJUnitRunner `OK (0 tests)` và runner có test skip. |
| `scripts/test-unit.ps1 -Offline` | PASS — Gradle `BUILD SUCCESSFUL`; 27 test, 0 failure/error/skipped. Chạy không tải dependency/mạng; gồm A-04 (8), A-05 (11), fixture mới (7) và test mẫu (1). XML mới trong `app/build/test-results/testDebugUnitTest/`; log `app/build/reports/a06/20261019-005438-069-unit.log`. |
| `scripts/test-all.ps1 -DeviceSerial emulator-5554` (lượt cuối) | PASS — assembleDebug, unit 27/27, lint, instrumentation 18/18 và process restart seed 1/1 + verify 1/1 + cleanup 1/1; mọi failure/error/skip đều 0. APK được cài bằng `adb -s emulator-5554 install -r`; không uninstall. Log `app/build/reports/a06/20261019-011801-055-all-build-unit-lint.log`, `20261019-011928-134-instrumentation-direct-build.log`, `20261019-011928-134-instrumentation-direct-runner.log`, `20261019-011928-134-process-restart-seed.log`, `20261019-011928-134-process-restart-verify.log`, `20261019-011928-134-process-restart-cleanup.log`. |
| `scripts/test-instrumentation.ps1 -DeviceSerial emulator-5554 -ForceDirectRunner` | PASS trên phiên bản script lúc chạy trước khi thêm process restart tự động — direct runner trả `OK (18 tests)`, passed-status 18/18, failure 0, skip 0. Tùy chọn cũ được đổi thành direct runner mặc định và `-UseGradleRunner` opt-in. |
| `scripts/test-all.ps1 -DeviceSerial emulator-5554` (lượt Gradle trước khi đặt direct làm mặc định) | PASS — build/unit/lint thành công, unit 27/27; `connectedDebugAndroidTest` trên emulator API 37 tạo XML 18 test, 0 failure/error/skip. Log `app/build/reports/a06/20261019-005057-009-instrumentation-gradle.log`, XML `app/build/outputs/androidTest-results/connected/debug/Medium_Phone_API_37.0(AVD) - 17/TEST-emulator-5554.xml`. AGP gỡ app/test khỏi AVD sau lượt này; không có điện thoại vật lý kết nối. |
| `gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain` | PASS — chạy lại sau pipeline cuối, exit 0, `BUILD SUCCESSFUL`, 53 actionable tasks (1 executed, 52 up-to-date). Unit test được thực thi đầy đủ trong pipeline phía trên. APK debug tại `app/build/outputs/apk/debug/app-debug.apk`. |
| `lintDebug` | PASS — 0 error, 15 warning. Không phát hiện lỗi nghiêm trọng mới; các cảnh báo gồm label/resource scaffold, dependency có phiên bản mới hơn (trong đó OkHttp/MockWebServer 5.5.0), không nâng cấp không cần thiết. Report `app/build/reports/lint-results-debug.html`, `.sarif`, `.txt`. |
| PowerShell | PASS — cả 5 script được phân tích cú pháp; `test-all.ps1` chạy thành công hoàn chỉnh, gồm harness guard và process restart. |
| `git diff --check` | PASS — không có lỗi whitespace; Git chỉ cảnh báo một số file hiện dùng LF sẽ được đổi sang CRLF theo cấu hình làm việc. |

Trong các lượt trung gian, runner phát hiện assertion A-05 giả định nhãn provider là duy nhất sau khi thêm fixture provider; đã đổi sang truy vấn toàn bộ node. Lượt process restart đầu truyền phase nhưng chưa lọc class nên vô tình chạy cả 18 test; đã thêm bộ lọc `-e class` cho riêng `A05CredentialRestartInstrumentedTest`. Pipeline cuối sau hai sửa đổi đạt toàn bộ số test ở bảng trên.

### Rà soát bảo mật và giới hạn

- Unit suite offline; MockWebServer bind loopback, không có request Internet. Fixture và log không chứa API key thật, khóa, dữ liệu truyện/hội thoại riêng tư hoặc database người dùng. Thay đổi phụ thuộc chỉ ở `testImplementation`/version catalog; không có API provider hoặc SSE parser production.
- Lượt Gradle instrumentation đã uninstall app package khỏi AVD theo hành vi AGP nêu trên; không có điện thoại vật lý online, và APK app/test được cài lại qua lượt direct runner cuối. Dữ liệu app cũ trên AVD không được khôi phục. Đây là tác động môi trường cần lưu ý; script mặc định đã đổi để tránh lặp lại.
- Pha cleanup cuối đạt 1/1; kiểm tra độc lập bằng `adb -s emulator-5554 shell run-as com.hoangtu.aistorystudio ls cache/a06-provider-settings-test.preferences_pb` xác nhận file không còn. Lệnh dò ban đầu dùng `adb shell sh -c 'test ! -e ...'` bị lỗi truyền đối số qua shell; đã xác minh lại trực tiếp bằng `ls`, không còn file DataStore test.
- Chưa kiểm thử lựa chọn giữa nhiều emulator vì hiện chỉ có một AVD online. Chưa có Chat UI/Novel Editor, nên test chỉ xác nhận provider/fake và harness, không tuyên bố màn hình sản phẩm chạy offline. A-01 vẫn thiếu smoke test điện thoại thật.
- Cảnh báo runtime protobuf `sun.misc.Unsafe` và việc AGP không strip được một số native library vẫn xuất hiện từ dependency/toolchain hiện hữu; build/test đạt. Lint gợi ý cập nhật test-only OkHttp/MockWebServer nhưng không có lỗi lint mới nghiêm trọng.

### Kết luận

- A-06 đạt tiêu chí trong phạm vi backlog: build, regression unit/instrumentation, MockWebServer hoạt động, runner xác minh số test, zero/skip guard, offline tests, không lỗi lint nghiêm trọng mới và quy trình Windows có thể lặp lại.
- A-01 vẫn ở trạng thái đạt một phần cho đến khi smoke test trên điện thoại thật. Không triển khai A-07, C-01, C-03 hoặc nghiệp vụ ứng dụng.

## D-06 — Signing APK release và kiểm thử cập nhật — 19/10/2026

### Phạm vi và hiện trạng trước khi làm

- Đọc `AGENTS.md`, backlog, architecture, decisions, roadmap, test plan, task log và Git workflow; backlog yêu cầu giữ applicationId, khóa release ngoài repository, tăng versionCode và kiểm chứng cập nhật V1→V2 cùng chữ ký bằng `adb install -r`.
- Git ban đầu sạch trên `main`, HEAD `27e3aace` (checkpoint A-06); không push. `applicationId` là `com.hoangtu.aistorystudio`, `versionCode` hiện tại là 1.
- Toolchain giữ nguyên: JBR 25, Gradle Wrapper 9.6.0, AGP 9.4.1, Android SDK 37, Build Tools 36.0.0. ADB server 5037.
- ADB chỉ thấy emulator `emulator-5554` (AVD đang có) và AVD thử nghiệm riêng `emulator-5556`; không có điện thoại vật lý online. Emulator 5554 chỉ nhận APK debug qua `install -r`; không chạy `connectedDebugAndroidTest`.
- Không tìm thấy cấu hình signing release hoặc keystore release sẵn có. Chỉ tạo keystore PKCS12 ngẫu nhiên dùng một lần dưới `C:\jtmp` để xác minh pipeline; không tạo hoặc thay khóa production của người dùng.

### Thay đổi

- `app/build.gradle.kts`: đọc `app.versionCode` từ Gradle property, kiểm tra miền version hợp lệ, giữ applicationId ổn định, tách debug/release signing, chỉ đọc signing values từ bốn biến process `ASTORY_RELEASE_*`; release build từ chối khi thiếu biến, keystore không tồn tại, đường dẫn tương đối hoặc nằm trong repository. Release task không bật signing nếu cấu hình thiếu.
- `gradle.properties`: thêm `app.versionCode=1`.
- `.gitignore`: bổ sung `*.p12`, `*.pfx`, `*.pem`, `*.key`, `release-signing.properties`; giữ ignore cho `.jks`, `.keystore`, `key.properties`, `secrets.properties`, local.properties, APK và build output.
- `scripts/build-release.ps1`: prompt đường dẫn/alias, nhận mật khẩu ẩn, đặt biến chỉ trong process khi gọi Gradle Wrapper với `--no-configuration-cache`, xác minh APK bằng `apksigner` rồi khôi phục/xóa môi trường tạm.
- `scripts/test-release-upgrade.ps1`: chỉ nhận emulator, từ chối nếu app đã cài, dựng V1/V2 bằng cùng keystore và version code tăng tạm một đơn vị, kiểm tra manifest/chứng chỉ, cài bằng `adb install -r`, seed/verify/delete fixture app-private, quét APK/log/file Git tracked và untracked để không chứa keystore/signing secrets. Không uninstall hoặc clear data.
- `app/src/androidTest/java/com/hoangtu/aistorystudio/D06ApkUpgradeInstrumentedTest.kt`: fixture giả có phase seed/verify/cleanup; không dùng Room hoặc dữ liệu người dùng.
- Cập nhật `AGENTS.md`, `README.md`, `docs/RELEASE_SIGNING.md`, architecture, decisions, roadmap và test plan về ký, bảo quản/backup keystore, version, test harness và giới hạn.
- Tắt R8/shrinking cho release hiện tại. Cấu hình tối ưu trước đó làm APK thật crash runtime; không phát hành APK chỉ vì Gradle báo build thành công.

### Build và kiểm thử có bằng chứng

| Lệnh/kịch bản | Kết quả quan sát |
|---|---|
| `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain` | PASS — `BUILD SUCCESSFUL`, 53 actionable tasks; build cuối có 1 executed/52 up-to-date. Unit tests đã được chạy thực tế trong pipeline test-all dưới đây. |
| `.\scripts\test-all.ps1 -DeviceSerial emulator-5554` | PASS — build/debug/unit/lint thành công; 27 unit tests, 0 failure/error/skip; lint 0 error, 15 warning. Direct AndroidJUnitRunner đạt 19/19 instrumentation tests; process restart regression seed/verify/cleanup mỗi pha 1/1. Dùng `adb install -r`, không dùng Gradle connected runner. Log: `app/build/reports/a06/20261019-020225-916-all-build-unit-lint.log`, `20261019-020355-575-instrumentation-direct-build.log`, `20261019-020355-575-instrumentation-direct-runner.log`, `20261019-020355-575-process-restart-seed.log`, `...-verify.log`, `...-cleanup.log`. |
| `.\scripts\test-release-upgrade.ps1 -DeviceSerial emulator-5556` | PASS ở lượt cuối — release V1 build thành công (102 task), instrumentation APK build thành công; seed 1/1; release V2 build thành công (49 task); verify 1/1 và cleanup 1/1. APK snapshot: `app/build/reports/a06/20261019-015807-711-release-upgrade/release-v1.apk`, `release-v2.apk`, cùng test APK của harness. |
| `aapt dump badging` + `apksigner verify --print-certs` trên V1/V2 | PASS — cả hai APK `com.hoangtu.aistorystudio`; versionCode 1→2; `apksigner` exit 0; fingerprint SHA-256 của cả hai bằng `07BA2937C7110829CED4167C8BAF4118A48CC3CEB23BD144AC5F79A1708C8ABF`. |
| Cài và giữ fixture app-private V1→V2 | PASS — log `20261019-015806-650-d06-release-v1-install.log` và `...-d06-release-v2-install.log` đều có `Success`; test seed/verify/cleanup trả `OK (1 test)` từng pha. Test APK instrumentation cũng cài được và được ký với chứng chỉ tạm cùng release APK. |
| Khởi chạy release V2 sau update | PASS trên AVD — `MainActivity` resumed; 0 fatal crash trong logcat mới. |
| Release task khi thiếu signing variables | PASS fail-closed — `:app:assembleRelease --no-configuration-cache --console=plain` exit 1 và hiện thông báo `Release signing is required`; không có password được in. |
| PowerShell AST + `git diff --check` | PASS — hai script D-06 parse không lỗi; diff không lỗi whitespace (Git chỉ cảnh báo line ending LF/CRLF). |
| Bảo mật Git/artifact | PASS — không có file signing trong tracked/untracked non-ignored working tree; các đuôi keystore và release-signing properties được `.gitignore` bỏ qua. Pipeline quét giá trị password dùng một lần trong APK, log và file repository, không phát hiện. |

### Lỗi trung gian và sửa nguyên nhân

- Lượt đầu, `pm path` trả exit 1 khi app chưa cài trên AVD sạch; đổi preflight sang `cmd package list packages`, lệnh này thành công khi package vắng mặt và vẫn phát hiện đúng package đã cài.
- `assembleReleaseAndroidTest` không tồn tại vì dự án chỉ có androidTest variant debug. Script dùng `assembleDebugAndroidTest`, ký lại test APK riêng bằng certificate của lượt test để instrumentation có thể chạy với release target; không đổi `testBuildType` toàn dự án.
- Release APK có R8 optimization bật đã crash trên AVD với `IllegalAccessError` trong `kotlin.sequences`; khi chạy instrumentation còn thiếu Kotlin runtime trong target classloader. Bổ sung `android.r8.gradual.support` không sửa được. Tắt optimization cho release đã làm V1/V2 mở và chạy instrumentation được. Không bật lại cho đến khi có kiểm thử R8/runtime riêng.
- Có các lượt test trung gian không đạt trước các sửa đổi trên; chúng không được tính PASS. Lượt chạy cuối được liệt kê trong bảng là bằng chứng nghiệm thu hiện tại.

### Giới hạn và trạng thái nghiệm thu

- Khóa test dùng một lần không phải khóa phát hành. Người dùng vẫn cần tự tạo keystore production ngoài repository, lưu mật khẩu trong password manager và xác minh bản backup thứ hai theo `docs/RELEASE_SIGNING.md`. Không có backup production để nghiệm thu; không gửi mật khẩu vào chat.
- Sáu keystore PKCS12 kiểm thử được tạo một lần dưới `C:\jtmp`, ngoài repository; password ngẫu nhiên chỉ tồn tại tạm trong process, không được lưu/in. Chúng không phải khóa production. Lệnh dọn các tệp kiểm thử bị tool safety policy chặn nên tệp mã hóa còn tại đó; có thể xóa thủ công sau khi xác nhận đúng tên test-only.
- Physical-device smoke test BLOCKED vì ADB chỉ thấy emulator. A-01 tiếp tục đạt một phần; D-06 không thay thế điều kiện đó.
- D-06 hạ tầng ký/update đã được kiểm chứng trên AVD, nhưng trạng thái tổng thể là **đạt một phần** cho đến khi có khóa production và bản sao lưu thật. Không làm D-07, C-01 hoặc task khác.

## A-01 — Smoke test bổ sung trên điện thoại thật — 20/10/2026

### Phạm vi và tiền kiểm

- Đọc `AGENTS.md`, toàn bộ `docs/BACKLOG_V1.md` và `docs/TASK_LOG.md`. Tiêu chí A-01 trong backlog: `assembleDebug` thành công, APK mở trên điện thoại thật, không commit `local.properties`, API key hoặc `.jks`. Chỉ chạy smoke test A-01; không sửa tính năng ứng dụng.
- Git trước khi ghi kết quả sạch trên `main`, HEAD `2e4adc2` (checkpoint D-06), nhánh local ahead `origin/main` 4 commit. Không push.
- Dùng ADB trong Android SDK tại `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` với serial `101819060506`. ADB `37.0.1-15733141`; thiết bị trả `device`, `sys.boot_completed=1`, model `21061119AG`, product `selene`, Android 13, API 33. `adb` chưa có trong `PATH`; gọi trực tiếp ADB của SDK.
- Gói `com.hoangtu.aistorystudio` đã cài trước đó, `versionCode=1`, `firstInstallTime=2026-10-18 23:29:39`. `run-as` hoạt động. Tiền kiểm chỉ đếm mục ở thư mục riêng tư, không đọc nội dung: `databases` không có/không truy cập được, `shared_prefs` 0 mục, `files` 1 mục.

### Build, đối chiếu chữ ký và cài đặt

| Lệnh / kiểm tra | Kết quả quan sát |
|---|---|
| `.\gradlew.bat assembleDebug --console=plain` | PASS — `BUILD SUCCESSFUL`, 37 actionable tasks đều `UP-TO-DATE`; APK hiện tại tại `app/build/outputs/apk/debug/app-debug.apk`. |
| `aapt dump badging` | PASS — package `com.hoangtu.aistorystudio`, `versionCode=1`, `versionName=1.0`, `compileSdkVersion=37`. |
| `apksigner verify --print-certs` | PASS — APK mới và APK đã cài đều xác minh được; SHA-256 chứng chỉ giống nhau: `fd16e9b6fea51fa047810bb1a959a81cb4c1f8e58015f9aac09081fc8da712ff`. SHA-256 APK mới: `E522B695BC5F67272D9BAC29B6DB0B5969B38E6332EFB89B7DF3555CBEA4E29D`. |
| `adb -s 101819060506 install -r app/build/outputs/apk/debug/app-debug.apk` | PASS — ADB trả `Success`. Không chạy `adb uninstall` hoặc `pm clear`. Sau cập nhật, `firstInstallTime` vẫn giữ nguyên và `lastUpdateTime=2026-10-20 13:41:04`; `versionCode=1`. |
| Đếm lại mục dữ liệu riêng tư | Sau khi mở app: `databases` không có/không truy cập được, `shared_prefs` 0 mục, `files` 2 mục. Số mục `files` không giảm. Không so sánh byte nội dung; không đưa nội dung riêng tư vào log. Cài cập nhật dùng cùng chữ ký và `install -r`, không gỡ/cài mới. |

### Smoke test giao diện và vòng đời

- `adb shell am start -W -n com.hoangtu.aistorystudio/.MainActivity` trả `Status: ok`, `Activity: com.hoangtu.aistorystudio/.MainActivity`; `LaunchState: COLD` ở lần mở đầu.
- Dùng thanh điều hướng trên điện thoại và xác minh tiêu đề bằng cây giao diện: Trang chủ, Nhập vai, Tiểu thuyết, Nhân vật, Thư viện, Cài đặt — cả 6/6 tiêu đề xuất hiện sau khi chọn.
- Quan sát trực quan: nhãn mục điều hướng “Tiểu thuyết” bị rút gọn trên màn hình điện thoại này; thao tác chọn vẫn mở đúng màn Tiểu thuyết. Không sửa giao diện trong lượt chỉ hoàn tất smoke test A-01.
- Chọn Nhập vai → đưa app xuống nền bằng `KEYCODE_HOME` → mở lại bằng ADB. Màn Nhập vai vẫn được hiển thị. Nhấn `KEYCODE_BACK` từ Nhập vai trở về Trang chủ.
- Quét `adb logcat -d -v threadtime -t 4000`: 0 khối fatal gắn với package ứng dụng, 0 mục ANR gắn với package; tiến trình ứng dụng còn sống (`pidof` trả PID). Không chạy instrumentation test trên điện thoại thật.
- `uiautomator dump` trên MIUI in stack trace thiếu `theme_compatibility.xml` nhưng vẫn tạo XML; tiêu đề các màn hình đọc được. Đây là lỗi của công cụ dump/ROM, không phải lỗi tiến trình ứng dụng. Lượt kiểm tra logcat đầu dùng tên biến PowerShell `$pid` chỉ đọc; đã chạy lại bằng `$appProcessId` và nhận kết quả nêu trên.
- APK cài đặt có SHA-256 nêu trên. Kiểm tra Git: không có đường dẫn nhạy cảm được theo dõi (`local.properties`, keystore, `.env`, secrets); tìm mẫu khóa phổ biến `sk-...` và Google API key trả 0 tệp. Không thấy `local.properties`, API key hoặc `.jks` trong file Git được theo dõi.

### Kết luận và giới hạn

- Theo tiêu chí backlog A-01, smoke test điện thoại thật đã đạt: build debug thành công, package đúng chữ ký được cập nhật bằng `install -r`, app mở được và các màn hình yêu cầu hiển thị; không phát hiện crash/ANR trong lần kiểm tra này. A-01 **PASS**.
- Việc kiểm tra bảo toàn dữ liệu không đọc nội dung hoặc đối chiếu hash từng file; bằng chứng là cùng chữ ký, `install -r`, `firstInstallTime` không đổi và số mục `files` không giảm. Không tuyên bố kiểm chứng byte-for-byte.
- Ảnh chụp giao diện tạm được lưu trong `app/build/a01-device-preflight/screen-home.png` (đường dẫn bị Git ignore); ảnh có banner thông báo hệ thống cá nhân nên không được thêm vào tài liệu/Git. Yêu cầu xóa artifact tạm bị công cụ thực thi chặn; file hiện vẫn nằm cục bộ. XML hierarchy và APK đã pull vào cùng thư mục build cũng là artifact ignored.
- Không chạy unit/instrumentation regression trong lượt này; đã thực hiện đúng build và smoke test A-01. Không triển khai nhiệm vụ khác.

## C-01 — Quản lý nhân vật AI và User Persona — 20/10/2026

### Kiểm tra trước khi triển khai

- Đọc `AGENTS.md`, toàn bộ `docs/BACKLOG_V1.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/ROADMAP.md`, `docs/TEST_PLAN.md` và `docs/TASK_LOG.md`. Backlog C-01 yêu cầu CRUD hồ sơ có tuổi trưởng thành, tính cách, xưng hô và quy tắc nhập vai; CRUD/chọn Persona; dữ liệu còn sau khi đóng/mở; hồ sơ gốc không bị prompt tự sửa.
- Git trước khi sửa: working tree sạch trên `main`, HEAD `972eeb8` (`Record physical navigation smoke observation`), local ahead `origin/main` 6 commit. Không push.
- Bằng chứng nhiệm vụ nền đã đọc trong TASK_LOG: A-01 PASS sau cài/mở APK trên Xiaomi Android 13/API 33; A-02 có hồi quy 6 route/Back/lifecycle trên emulator; A-03 có Room CRUD/FK/persistence/schema V1; A-05 có Settings/Keystore/DataStore test; A-06 có test harness và emulator regression. A-04 đã kiểm thử abstraction/FakeAiProvider, nhưng chính TASK_LOG ghi Chat UI và Novel Editor chưa tồn tại nên tiêu chí consumer offline chưa được kiểm chứng; không coi phần tích hợp đó là PASS.
- Môi trường lượt này: Windows 11, Gradle Wrapper 9.6.0, Android Studio JBR/Gradle daemon JDK 25, Android SDK 37. Có điện thoại `101819060506` và emulator `emulator-5554`; instrumentation chỉ chạy trên emulator API 37 theo guard của `scripts/test-all.ps1`.
- Schema V1 được kiểm tra trực tiếp: nhân vật có mô tả chung, personality, relationship và speaking style; chưa có tuổi riêng, ngoại hình, bối cảnh, roleplay rules. Persona chưa có trạng thái mặc định. Do các trường có validation/hiển thị riêng và backlog yêu cầu chọn Persona, C-01 thêm migration thật V1→V2; không nhét dữ liệu có cấu trúc vào `description`, không đổi ID cũ và không dùng destructive migration.

### Chia nhỏ và triển khai

1. Đối chiếu entity/DAO/repository, FK `RESTRICT` và schema V1.
2. Thêm cột V2 với giá trị mặc định bảo toàn, export schema và MigrationTestHelper kiểm tra dữ liệu V1.
3. Thêm use case, validation, ViewModel/StateFlow và kết nối repository qua composition root.
4. Thay màn hình placeholder bằng UI tiếng Việt cho danh sách/tìm kiếm, xem hồ sơ, tạo/sửa, xóa có xác nhận và quản lý Persona.
5. Bổ sung unit, repository, migration, process-restart và Compose instrumentation tests; chạy hồi quy; rà secrets/diff.

### Thay đổi chính

- `CharacterProfileEntity` thêm `age` nullable, `appearance`, `background`, `roleplayRules`; `UserPersonaEntity` thêm `isDefault`. Tuổi V1 được để `NULL` vì không thể suy ra tuổi cũ; form ghi mới chỉ chấp nhận 18–120. Các trường chuỗi mới dùng `''` cho bản ghi V1.
- `StudioDatabase.VERSION` tăng 1→2; `StudioDatabaseMigrations.MIGRATION_1_2` chỉ `ALTER TABLE`, được đăng ký trong database. Lưu `app/schemas/.../1.json` hiện có và schema mới `app/schemas/.../2.json`.
- `CharacterDao` tìm theo tên và chọn Persona mặc định trong transaction. ID không tồn tại không xóa lựa chọn cũ; khi chọn Persona khác, một câu `UPDATE` đặt tối đa một hàng mặc định.
- `CharacterRepository` trả `DELETED`/`NOT_FOUND`/`REFERENCED`; lỗi FK `RESTRICT` được ánh xạ thành kết quả nghiệp vụ, không log nội dung hồ sơ. Hàng nhân vật/Persona đang được hội thoại tham chiếu được giữ nguyên.
- Thêm `CharacterManagementUseCases`, `CharacterManagementViewModel`, validator và factory. UI gọi sự kiện qua ViewModel; không gọi DAO trong Composable. ID mới do UUID sinh; khi sửa, giữ nguyên ID/createdAt; nếu bản ghi đã bị xóa giữa lúc mở form và lưu thì không tái tạo ngầm.
- `CharactersScreen` có tab Nhân vật/Persona, tìm theo tên, danh sách rỗng/loading/error, form cuộn được với `imePadding`, xem đầy đủ thông tin và hộp xác nhận xóa. Theme Material 3 sáng/tối và UTF-8 tiếng Việt hiện có được dùng lại.
- `scripts/Test-Common.ps1` bổ sung pha process-restart cho Room trên database fixture riêng; script chỉ chạy khi `Select-A06Emulator` xác nhận serial là emulator.
- Cập nhật `docs/ARCHITECTURE.md`, `docs/DECISIONS.md` (ADR-014) và `docs/TEST_PLAN.md` theo schema/UI hiện tại.

### Build và kiểm thử cuối cùng

| Lệnh / kịch bản | Kết quả quan sát |
|---|---|
| `.\scripts\test-all.ps1 -DeviceSerial emulator-5554` | PASS ở lượt cuối `20261020-140810-100`: harness chấp nhận fixture 1/1 và từ chối 0 test/skip; Gradle build + unit + lint thành công; AndroidJUnitRunner trực tiếp trên AVD API 37. Script dùng `adb install -r`, không dùng `uninstall` hoặc `pm clear`. |
| `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug` (được gọi bởi script với `--rerun-tasks --no-daemon --console=plain`) | PASS — `BUILD SUCCESSFUL`; unit test 29, failures 0, errors 0, skipped 0; lint 0 error, 15 warning. Cảnh báo gồm advisory phiên bản, resource mẫu chưa dùng và redundant manifest label; không đổi thư viện theo advisory. Có cảnh báo JDK `sun.misc.Unsafe` từ DataStore protobuf trong test log. |
| `:app:assembleDebugAndroidTest` (direct runner build) | PASS — `BUILD SUCCESSFUL`, APK app và test được tạo/cài trên `emulator-5554`. |
| AndroidJUnitRunner toàn bộ suite | PASS — 26 test, 26 passed, 0 failure, 0 skip; gồm migration V1→V2 bảo toàn dữ liệu, C01 CRUD/FK/default/reopen/Compose, cùng regression Navigation, Settings, Room và FakeAiProvider. |
| A-05 process restart regression | PASS — `seed`, `verify`, `cleanup` mỗi pha 1/1 sau force-stop/launch; log dưới `app/build/reports/a06/20261020-140810-100-process-restart-*.log`. |
| C-01 Room process restart | PASS — seed profile/Persona giả vào DB có tên ngẫu nhiên riêng, force-stop ứng dụng, mở lại, đọc lại tuổi/danh tính/Persona mặc định, cleanup; seed/verify/cleanup mỗi pha 1/1. Log: `app/build/reports/a06/20261020-140810-100-c01-restart-*.log`. DB kiểm thử không dùng tên DB production và được script dọn thành công. |
| `git diff --check` | PASS — không có whitespace error; Git báo cảnh báo chuyển line ending LF→CRLF cho các file Kotlin/PowerShell đã sửa. |
| Rà secrets và artifact | PASS — quét file thay đổi không thấy mẫu API key/token/private key/mật khẩu; không có `local.properties`, `.env`, keystore hoặc APK/build/cache được thêm vào Git. `app/schemas/2.json` nằm trong thay đổi cần commit. |

### Lỗi trung gian đã sửa

- Lượt instrument đầu có 2/26 test lỗi do matcher `assertTextContains` không khớp semantics có dấu như dự kiến và test Persona chọn tag không duy nhất. Đổi sang so khớp text chính xác và content description duy nhất; lượt cuối đạt 26/26.
- Lượt kế tiếp không biên dịch androidTest vì Compose test artifact hiện tại không có `onLast()`; thay bằng `onNodeWithContentDescription` cụ thể. Đây là lỗi test harness đã sửa; không có lỗi build/test còn lại ở lượt cuối.

### Kết luận nghiệm thu

- CRUD nhân vật/Persona, xem hồ sơ, tìm theo tên, validation tuổi trưởng thành, chọn Persona mặc định, FK RESTRICT, Unicode, giữ nguyên dữ liệu V1 và khôi phục database sau process restart đã có test thực chạy trên emulator API 37.
- **C-01 đạt phần triển khai và kiểm thử quản lý dữ liệu, nhưng chưa đóng đầy đủ tiêu chí backlog.** Tiêu chí “prompt không tự sửa hồ sơ gốc” chưa thể kiểm chứng vì chưa có prompt/context consumer; A-04 cũng đã ghi nhận Chat UI/Novel Editor chưa tồn tại. Không thêm Chat UI hoặc ContextBuilder trong C-01 để tạo bằng chứng giả. Cần kiểm tra lại tính bất biến hồ sơ khi có prompt/context consumer thuộc task phù hợp.
- Không kiểm thử instrumentation trên điện thoại cá nhân; không thay đổi dữ liệu ứng dụng trên điện thoại. Không nâng AGP, Gradle, Kotlin, SDK hoặc thư viện.
- Checkpoint Git cục bộ được tạo sau rà soát diff; không push.

## C-02 — Quản lý hội thoại và lịch sử tin nhắn — 20/10/2026

### Kiểm tra trước khi triển khai

- Đọc đầy đủ `AGENTS.md`, backlog, architecture, decisions, roadmap, test plan, task log và Git workflow. Backlog C-02 nghiệm thu ba chat thuộc hai nhân vật không lẫn tin nhắn, thứ tự ổn định và mở lại đúng lịch sử; prompt bổ sung yêu cầu chọn Persona, tìm kiếm, tạo/mở/xóa xác nhận và process restart.
- Trạng thái Git đầu lượt: working tree sạch, `main` tại `eb5f38a` (checkpoint C-01), ahead `origin/main` 7 commit; remote là repository AI Story Studio. Schema export V1 và V2 có trong Git, `StudioDatabase.VERSION=2`; không có schema/migration C-02 cần thêm.
- A-01 đã PASS theo smoke test trên Xiaomi Android 13/API 33, được ghi ở phần A-01 phía trên. C-01 vẫn đạt một phần: tiêu chí “prompt không tự sửa hồ sơ gốc” được giữ deferred đến khi có prompt/context consumer; không tuyên bố đạt tiêu chí này.
- Môi trường: JDK/Gradle daemon 25, Gradle Wrapper 9.6.0, Android SDK 37, ADB 37.0.1; `emulator-5554` là emulator API 37. Không chạy instrumentation trên điện thoại cá nhân.
- Chia C-02 thành: (1) rà Room V2 và bảo đảm sequence/scope; (2) bổ sung truy vấn header/tìm kiếm và tạo insert transaction; (3) thêm Use Cases/ViewModel; (4) nối Roleplay list, tạo hội thoại và nested history route; (5) thêm fixture Room/Compose/process restart; (6) hồi quy, ghi bằng chứng, security review và checkpoint.

### Thay đổi

- Không thay Room schema V2. `ChatMessageEntity.sequence` và unique index theo `(conversation_id, sequence)` đã có; query lịch sử tiếp tục giới hạn theo `conversationId` và sắp `sequence ASC, id ASC`.
- Thêm `ConversationHeader` projection, DAO query join tên Character/Persona, search title/character/persona và thứ tự danh sách xác định. `createConversation` dùng UUID từ use case, `INSERT` trong transaction để trùng ID/FK không upsert đè dữ liệu.
- Generic `saveConversation` giữ cố định Character và Persona đã gắn; thử đổi owner/Persona bị từ chối. Thêm cập nhật `updated_at` đơn điệu khi lưu message, dùng `MAX` để timestamp cũ đến muộn không làm lùi hoạt động gần nhất.
- Thêm `ConversationManagementUseCases`, `RoleplayViewModel` với StateFlow, loading/error/retry, selection Character/Persona, xác nhận xóa và quan sát header/message. Lỗi cancellation được ném lại; UI không gọi DAO.
- Màn Roleplay hiển thị danh sách/tìm/empty state; tạo hội thoại chọn Character/Persona; route lịch sử lồng trong Roleplay hiển thị message chỉ đọc theo sequence, status và thời gian. System Back quay lại danh sách; tab Roleplay vẫn được chọn. Không có composer, request AI, streaming, retry hoặc regenerate.
- `Test-Common.ps1` chạy thêm pha process restart C-02 bằng database fixture UUID riêng: seed → force-stop app → launcher → verify → cleanup. Không dùng DB production, `uninstall` hoặc `pm clear`.
- Cập nhật `docs/ARCHITECTURE.md`, ADR-015 trong `docs/DECISIONS.md`, trạng thái C-02 và giới hạn C-01 trong `docs/ROADMAP.md`, cùng ma trận kiểm thử C-02 trong `docs/TEST_PLAN.md`.

### Build và kiểm thử cuối cùng

| Lệnh/kịch bản | Kết quả quan sát |
|---|---|
| `scripts/test-all.ps1 -DeviceSerial emulator-5554` | PASS — pipeline hoàn tất exit 0; harness guard pass, assembleDebug/unit/lint/instrumentation và process restart đều đạt. Run ID `20261020-143150-701` (build/unit/lint), instrumentation `20261020-143353-843`. |
| Gradle `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug --rerun-tasks --no-daemon --console=plain` | PASS — `BUILD SUCCESSFUL`, 53 tasks executed; 29 unit test, 0 failure/error/skip. Lint: 0 error, 15 warning. Log `app/build/reports/a06/20261020-143150-701-all-build-unit-lint.log`; report `app/build/reports/lint-results-debug.html` và `.txt`. |
| Direct runner build `:app:assembleDebug :app:assembleDebugAndroidTest --no-daemon --console=plain` | PASS — `BUILD SUCCESSFUL`, 70 tasks, 5 executed/65 up-to-date. Log `app/build/reports/a06/20261020-143353-843-instrumentation-direct-build.log`. |
| AndroidJUnitRunner trực tiếp trên `emulator-5554` | PASS — `OK (35 tests)`, 35 status pass, 0 failure, 0 skip. Bao gồm C02 Compose list/search/create/history/system Back/delete confirm, Room scope/search/FK/rollback/immutable Persona và các regression Navigation, Character, Settings, Room, FakeAiProvider. Raw log `app/build/reports/a06/20261020-143353-843-instrumentation-direct-runner.log`. APK app/test được cài bằng `adb -s emulator-5554 install -r`; không gỡ hoặc clear data. |
| Process restart A-05, C-01, C-02 | PASS — mỗi luồng seed/verify/cleanup 1/1. C-02 xác nhận ba hội thoại, tên Character/Persona, message Unicode, conversation scope và sequence sau force-stop/launcher process mới. Log `app/build/reports/a06/20261020-143353-843-c02-restart-seed.log`, `...-verify.log`, `...-cleanup.log`; database UUID fixture được cleanup thành công. |
| `git diff --check` | PASS — không có whitespace error; Git chỉ cảnh báo line endings LF sẽ chuẩn hóa sang CRLF theo cấu hình máy. |

### Lỗi trung gian và giới hạn

- Lần gọi Gradle trực tiếp ngoài script lỗi `Unable to establish loopback connection`; script dự án thiết lập JDK/SDK/TEMP/ADB chuẩn và build sau đó thành công. Lượt instrumentation đầu tiên phát hiện icon ngoài Material Icons Core, marker navigation Roleplay bị bỏ, assertion TextField chưa phù hợp semantics và kỳ vọng search restart sai; đã sửa nguyên nhân. Lượt cuối nêu trên chạy lại trọn suite và đạt.
- 15 cảnh báo lint còn lại là warning hiện có/advisory tài nguyên và phiên bản dependency, 0 lỗi; không nâng thư viện. JUnit có warning deprecated `sun.misc.Unsafe` từ protobuf DataStore hiện có.
- C-02 không thay schema/migration; dữ liệu người dùng không bị xóa hoặc reset. Chưa kiểm thử trên điện thoại thật trong C-02; instrumentation chạy trên emulator API 37. Các luồng message composer/AI vẫn thuộc nhiệm vụ sau.
- C-01 vẫn deferred riêng đối với tính bất biến hồ sơ trong prompt. Không đánh dấu A-01 hay C-01 lại ngoài bằng chứng đã nêu.
- Checkpoint mã nguồn C-02: `07fc1e4` (`Implement C-02 conversation history management`). Trước push, local `main` có 8 commit ahead `origin/main` tại `12fcb85`; `origin/main` là ancestor, nên đồng bộ fast-forward, không rewrite/force push.
- Security pre-push: Gitleaks không được cài sẵn; kiểm tra tên file Git không thấy `local.properties`, `.env`, keystore, database người dùng, APK, build hoặc cache. Quét mẫu khóa/token phổ biến trên working tree và snapshots commit chưa đồng bộ không thấy private key/API token. Hai literal dài được gắn biến `secret`/`testApiKey` nằm trong `A05SecurityInstrumentedTest.kt` và `A05SettingsInstrumentedTest.kt`, là fixture xác định trong `androidTest` (prefix test); không phải credential production. Không in giá trị fixture. `app/schemas/.../1.json`, `2.json`, README và docs cần thiết được giữ trong Git.
- GitHub: `git push -u origin HEAD:main` thành công, fast-forward `12fcb85..07fc1e4`. `git fetch origin` và `git ls-remote origin refs/heads/main` xác nhận remote `main` tại `07fc1e4dfdcbfda635bc732ce4dc79e67c7fb5ac`; `git ls-tree origin/main` xác nhận README, tài liệu backlog/kiến trúc/workflow/task log/test plan và schema Room V1/V2 tồn tại. Remote chứa các checkpoint A-04, A-05, A-06, D-06, A-01 smoke, C-01 và C-02. Bản cập nhật task log ghi bằng chứng đồng bộ này sẽ là commit tài liệu kế tiếp.
