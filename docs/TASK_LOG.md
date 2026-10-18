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
