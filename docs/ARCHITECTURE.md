# Kiến trúc AI Story Studio

**Trạng thái:** ghi nhận hiện trạng đã kiểm chứng và thiết kế mục tiêu cho V1.0.
**Phạm vi:** Roleplay và Novel chạy độc lập, dùng chung hồ sơ nhân vật khi cần; ứng dụng cá nhân, ưu tiên offline và không cần backend riêng.

## Hiện trạng dự án

- Dự án hiện có một Gradle module Android tên :app, Kotlin và Jetpack Compose với Material 3.
- A-02 đã thêm Compose Navigation, theme Material 3 sáng/tối, thanh điều hướng thích ứng và sáu màn hình. Roleplay, Novel, Library và Settings có khung/thiết lập hiện có; Characters đã có quản lý hồ sơ và Persona sau C-01.
- Navigation nằm trong `core.navigation`; thành phần giao diện dùng chung ở `core.ui`; từng màn hình thuộc package `feature.*`. C-01 áp dụng Compose UI → `CharacterManagementViewModel` → `CharacterManagementUseCases` → `CharacterRepository`.
- A-03 tạo Room 2.8.5, schema V1, 14 Entity, bốn DAO và repository theo nhóm dữ liệu. C-01 nâng an toàn lên schema V2 bằng migration V1→V2 có kiểm thử bảo toàn dữ liệu. Schema 1 và 2 được quản lý tại `app/schemas/com.hoangtu.aistorystudio.core.database.StudioDatabase/`.
- A-05 đã thêm Preferences DataStore riêng cho cấu hình provider, repository và Settings ViewModel/UI. API key chỉ lưu dưới dạng AES-GCM ciphertext; Android Keystore giữ từng khóa AES không xuất được. Chưa có OkHttp/SSE hoặc nghiệp vụ chat/soạn thảo.
- A-04 bổ sung abstraction `core.ai.AiProvider` nhận danh sách message trung lập với provider/model và trả `Flow<AiStreamEvent>`. `FakeAiProvider` dùng fixture offline; chưa có provider mạng, cấu hình endpoint hay kết nối từ UI.
- A-01 đã đạt theo smoke test trên điện thoại Xiaomi Android 13/API 33 ngày 20/10/2026; xem bằng chứng, phạm vi so sánh dữ liệu và giới hạn trong `docs/TASK_LOG.md`.
- C-01 thêm CRUD nhân vật/Persona, validation tuổi trưởng thành, chọn Persona mặc định lưu trong Room, xử lý xóa bị FK RESTRICT chặn và tìm theo tên. Các luồng chat, memory retrieval/Context Builder, backup và autosave chưa triển khai.

## Nguyên tắc

1. **Offline-first:** nội dung người dùng được lưu cục bộ; tính năng đọc, sửa và xem lịch sử không phụ thuộc mạng.
2. **Nguồn dữ liệu rõ ràng:** Room là nguồn chính cho hồ sơ, hội thoại, bản thảo và ký ức. DataStore giữ tùy chọn và metadata cấu hình.
3. **Hai ngữ cảnh riêng:** mọi cuộc hội thoại thuộc Roleplay; mọi dự án/chương/cảnh thuộc Novel. Context Builder truy vấn theo scope rõ ràng để tránh trộn lịch sử hoặc ký ức.
4. **Provider có thể thay:** UI và nghiệp vụ gọi AiProvider; triển khai mạng theo API tương thích OpenAI và có FakeAiProvider cho kiểm thử offline.
5. **An toàn dữ liệu:** migration được kiểm thử, không có fallback xóa DB. Bản thảo và lịch sử phiên bản chỉ thay đổi theo thao tác đã xác nhận.
6. **Không phụ thuộc backend riêng:** kết nối AI đi trực tiếp từ ứng dụng tới endpoint người dùng cấu hình. Khóa do người dùng nhập, không có khóa dùng chung trong ứng dụng.

## Sơ đồ phụ thuộc mục tiêu

Compose UI theo feature
↓ sự kiện và UiState
ViewModel với StateFlow và Coroutines
↓
Use Cases cho quy tắc nghiệp vụ và điều phối
↓
Repository interfaces trong domain
↓
Data implementations:
- Room cho nội dung và lịch sử
- DataStore cho tùy chọn và metadata
- Kho an toàn cho token đã mã hóa bằng Android Keystore
- AiProvider với OkHttp/SSE hoặc FakeAiProvider

Chiều phụ thuộc đi từ UI về abstraction/domain. UI không truy cập trực tiếp DAO, OkHttp hay Android Keystore. I/O chạy ngoài main thread bằng Coroutines; Flow phát trạng thái lưu trữ và UI.

## Test harness A-06

- Các fixture dùng chung cho entity, phạm vi Roleplay/Novel, request/response AI và cấu hình provider nằm trong `app/src/test/.../testutil/`; định danh, timestamp và nội dung đều cố định, không chứa dữ liệu người dùng hoặc khóa thật.
- `MockWebServer` 5.4.0 cùng OkHttp chỉ là `testImplementation`. Harness kiểm tra HTTP response cục bộ, độ trễ/timeout/ngắt body, UTF-8 tiếng Việt và raw SSE chia chunk có `[DONE]`; chưa có parser SSE hoặc provider mạng trong production.
- Script PowerShell chỉ chấp nhận Android emulator và mặc định chạy AndroidJUnitRunner trực tiếp với `adb -s`; `install -r` giữ app data và chỉ cài vào serial đã xác minh. Gradle runner là tùy chọn riêng, chỉ cho phép một emulator online vì `connectedDebugAndroidTest` tự gỡ APK app/test khi kết thúc. Mọi đường chạy xác nhận số test dương, không có test skip/fail/error. Cách này ngăn Gradle báo BUILD SUCCESSFUL nhưng không chạy test nào bị hiểu nhầm là PASS.
- Script dùng ADB server chuẩn 5037 nhưng không đổi cấu hình hệ thống. Runner trực tiếp không gỡ ứng dụng hoặc xóa app data; test chạy trên emulator, không chỉnh cài đặt điện thoại thật.

## Tổ chức mã đề xuất

Giữ một module :app ban đầu; chia package theo trách nhiệm để giới hạn độ phức tạp build và phát hành APK cá nhân.

- com.hoangtu.aistorystudio.core.database: Room database, entity, DAO, migrations.
- com.hoangtu.aistorystudio.core.datastore: Preferences DataStore.
- com.hoangtu.aistorystudio.core.security: Android Keystore và mã hóa token.
- com.hoangtu.aistorystudio.core.ai: AiProvider, giao thức stream, lỗi chung.
- com.hoangtu.aistorystudio.core.navigation: đích điều hướng và NavHost.
- com.hoangtu.aistorystudio.core.common: thành phần dùng chung, không chứa nghiệp vụ feature.
- com.hoangtu.aistorystudio.feature.home
- com.hoangtu.aistorystudio.feature.roleplay: danh sách chat, chat, variants, bộ dựng ngữ cảnh.
- com.hoangtu.aistorystudio.feature.novel: dự án, chương, cảnh, editor, revisions, export.
- com.hoangtu.aistorystudio.feature.characters: hồ sơ dùng chung và liên kết theo scope.
- com.hoangtu.aistorystudio.feature.library
- com.hoangtu.aistorystudio.feature.settings
- com.hoangtu.aistorystudio.app: Application, Activity, theme và composition root.

Không tạo sẵn package rỗng chỉ để khớp sơ đồ. Tạo từng phần khi backlog yêu cầu và thêm test cùng với hành vi đó.

## Lớp và luồng dữ liệu

- **UI:** Compose hiển thị UiState, gửi intent của người dùng và giữ state ngắn hạn như selection/scroll. State cần khôi phục sau khi đóng app phải lưu ở tầng dữ liệu.
- **ViewModel:** chuyển intent thành lời gọi Use Case; công bố StateFlow. Hủy stream theo lifecycle hoặc thao tác Stop.
- **Use Case:** điều phối quy tắc, transaction cần thiết, Context Builder và luồng AI. Không chứa chi tiết Compose, SQL hay HTTP.
- **Repository:** domain khai báo interface; data cung cấp implementation. DAO chỉ được gọi qua repository/use case.
- **Room hiện tại:** `StudioDatabase` version 2, export schema bật; schema V1 còn trong Git và có migration V1→V2 có tên cùng test dữ liệu cũ. Các lệnh DAO one-shot dùng `suspend`, truy vấn quan sát dùng `Flow`. Không cấu hình destructive fallback.
- **Repository hiện tại:** bốn nhóm repository bọc DAO; `withTransaction` bảo vệ tạo hội thoại/message đầu, thêm message theo sequence, cấp số thứ tự novel và cập nhật summary. Memory chỉ chấp nhận đúng một owner, và repository chặn việc đổi owner của memory/message/summary.
- **Xóa dữ liệu:** hội thoại được tham chiếu bởi hồ sơ/persona dùng `RESTRICT`; con của hội thoại/chapter/project dùng `CASCADE`; liên kết Novel-character bị xóa cùng project hoặc character. Xóa project xóa toàn bộ nội dung con của project. Xóa nhân vật đang được hội thoại tham chiếu bị từ chối. Memory fact được đánh dấu tombstone bằng `deleted_at`; truy vấn thông thường bỏ qua fact đã xóa.
- **DataStore hiện tại:** file Preferences `ai_provider_settings.preferences_pb` lưu provider ID/name, URL, model, tham số, provider đang chọn và ciphertext/IV/alias. Không lưu API key dạng rõ. File này bị loại khỏi cloud backup, device transfer và legacy Auto Backup rules; khi bổ sung archive backup thủ công, phải tiếp tục loại trừ toàn bộ cấu hình provider.
- **SecureKeyStore hiện tại:** Android Keystore sinh khóa AES-256/GCM riêng cho mỗi lần lưu hoặc thay khóa; mỗi payload có IV ngẫu nhiên và AAD là provider ID. Repository mã hóa/giải mã ngoài main thread, chỉ trả ciphertext và byte array tạm; byte array tạm được xóa sau khi dùng trong Settings. Nếu alias mất, key bị invalidated hoặc authentication tag sai, ciphertext được giữ lại, cấu hình báo cần nhập lại key; không xóa Room hay dữ liệu truyện.
- **Settings hiện tại:** danh sách, chọn provider đang dùng, thêm/sửa cấu hình, nhập/ẩn/hiện API key, tham số temperature/top-p/max output tokens, lưu và xóa key có xác nhận. Kiểm tra tại màn hình chỉ xác thực cục bộ; không tạo request mạng hoặc thông báo kết nối thành công.
- **AiProvider:** API phát `TextDelta`, `Completed` hoặc `Failed` với nhóm lỗi dùng chung. Cancellation là cancellation của coroutine và không chuyển thành sự kiện lỗi. OkHttp/SSE sau này ánh xạ kết quả mạng vào API này; Fake provider tạo chunk, độ trễ ảo và lỗi xác định mà không truy cập mạng.
- **FakeAiProvider hiện tại:** chọn scenario mặc định hoặc theo `AiRequest`, hỗ trợ thành công (tương đương HTTP 200), unauthorized (401), rate limit (429), server error (500), timeout, ngắt sau partial và phản hồi rỗng. Delay dùng `kotlinx.coroutines.delay`, nên test kiểm soát bằng `TestCoroutineScheduler`.
- Interface dùng chung cho request Roleplay và Novel thông qua lịch sử `SYSTEM`/`USER`/`ASSISTANT`; chi tiết UI, HTTP, JSON và model được cấu hình ở implementation/use case phù hợp. Có thể truyền implementation bằng constructor injection; hiện chưa có framework DI hoặc consumer UI.

## Tách Roleplay và Novel

- Roleplay context chỉ chứa chỉ dẫn Roleplay, nhân vật/persona đã chọn, ký ức có scope phù hợp, summary của nhánh đang chọn và các tin nhắn gần nhất.
- Novel context chỉ chứa Story Bible, timeline, chương/cảnh hiện hành, summary của dự án và yêu cầu biên tập.
- Hồ sơ nhân vật có thể được hai chế độ tham chiếu, nhưng message, memory, summary, branch và history không tự chia sẻ. Mọi liên kết cần khóa ngoại hoặc ID scope rõ ràng.
- Context Builder phải giới hạn ngân sách và ưu tiên canon đã xác nhận trước dữ liệu suy luận; phải có test chứng minh không lẫn hai dự án.

## Bảo mật và vòng đời dữ liệu

- API key không đặt trong resource, BuildConfig, Room, log hoặc source của production. Test chỉ dùng fixture giả.
- Mỗi token được mã hóa bằng AES-256-GCM với khóa không xuất được do Android Keystore quản lý; IV và authentication tag được lưu cùng ciphertext trong Preferences DataStore riêng. Xóa token xóa ciphertext/IV/alias khỏi DataStore rồi xóa entry Keystore; nếu thao tác dọn alias thất bại, ciphertext vẫn đã bị xóa và UI báo việc dọn dẹp còn lại.
- Thay key tạo alias mới trước, cập nhật ciphertext/alias bằng một lần ghi DataStore, sau đó xóa alias cũ. Nếu ghi DataStore thất bại/đã bị hủy, không xóa alias mới để tránh trường hợp transaction đã commit nhưng ciphertext mất khóa; có thể còn alias Keystore mồ côi nhưng không có payload tham chiếu. Không có cơ chế backup/import cho các alias này.
- DataStore Preferences mặc định được đưa vào Android Auto Backup và device-to-device transfer; A-05 thêm rule loại trừ file cấu hình này cho cloud, device transfer và Android 11 trở xuống. Đây không phải chính sách backup hoàn chỉnh cho dữ liệu truyện; D-05 vẫn phải kiểm soát dữ liệu người dùng theo lựa chọn sản phẩm.
- Room migration phải giữ dữ liệu cũ. C-01 thêm migration V1→V2: tuổi cũ để `NULL`, trường mô tả mới dùng chuỗi rỗng, Persona cũ không tự thành mặc định; MigrationTestHelper xác nhận hồ sơ, Persona, hội thoại và khóa liên kết V1 vẫn còn. Không có destructive migration fallback.
- Khi lưu bản nháp, revision mới hoặc stream đang chạy, transaction và trạng thái phải thể hiện dữ liệu nào hoàn chỉnh, dang dở hoặc lỗi.

## Build và kiểm chứng

- Dùng Gradle Wrapper, AGP, Kotlin Compose plugin, Compose BOM, SDK và JDK đã chốt trong README và docs/TASK_LOG.md.
- AGP 9.4 yêu cầu Gradle 9.6, hỗ trợ tối đa API 37 và Build Tools 36.0.0; các giá trị hiện tại khớp bảng AGP. Wrapper dùng Gradle daemon JDK 25. JDK 27 trên PATH chưa được Gradle 9.6 công bố hỗ trợ để chạy daemon, nên cần giữ cấu hình daemon 25.
- AGP 9 có Kotlin tích hợp và khai báo phụ thuộc KGP 2.2.10; Kotlin Compose plugin hiện cũng là 2.2.10. Build thực tế xác nhận cấu hình này biên dịch được. Không nâng phiên bản nếu chưa có lỗi tái hiện hoặc quyết định nâng cấp có kiểm thử.
- Compose BOM và AndroidX là các phiên bản dự án hiện tại; thay đổi chúng cần lý do và kiểm thử hồi quy.
- `applicationId` cố định là `com.hoangtu.aistorystudio`; `app.versionCode` nằm trong `gradle.properties` và phải tăng trước mỗi bản phát hành mới. Release signing chỉ được cấu hình từ biến môi trường process; thiếu input hoặc keystore nằm trong repository thì release build fail closed. Debug tiếp tục dùng debug keystore riêng do Android SDK quản lý.
- Release task không dùng Gradle Configuration Cache (`--no-configuration-cache`) để giảm việc credential ký đi vào task graph/cache; mật khẩu không truyền trên dòng lệnh hoặc ghi log. Hướng dẫn và quy trình cập nhật nằm trong `docs/RELEASE_SIGNING.md`.
- R8/shrinking hiện tắt cho release. Khi thử bật tối ưu hóa package scope, APK bị crash runtime trên AVD; cấu hình chỉ được bật lại sau khi có kiểm thử release thực tế bao gồm mở ứng dụng và luồng cài đè.
- Kiểm thử APK V1→V2 dùng test fixture trong app-private cache trên emulator cô lập; không dùng Room migration giả. Kiểm thử không gỡ app, và runner từ chối emulator đã có ứng dụng cùng ID.

## Nguồn tham khảo

- [AGP 9.4: tương thích với Gradle, SDK, Build Tools và JDK](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [AGP 9: Kotlin tích hợp và KGP đi kèm](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [Gradle: ma trận tương thích JVM](https://docs.gradle.org/current/userguide/compatibility.html)
- [Android: kiến trúc offline-first](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Android Keystore](https://developer.android.com/privacy-and-security/keystore)
- [Preferences DataStore và Android Backup](https://developer.android.com/topic/libraries/architecture/datastore#device-backup-and-restore)
