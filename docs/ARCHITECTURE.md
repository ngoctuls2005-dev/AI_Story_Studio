# Kiến trúc AI Story Studio

**Trạng thái:** thiết kế mục tiêu cho V1.0; chưa phải mô tả các tính năng đã triển khai.
**Phạm vi:** Roleplay và Novel chạy độc lập, dùng chung hồ sơ nhân vật khi cần; ứng dụng cá nhân, ưu tiên offline và không cần backend riêng.

## Hiện trạng dự án

- Dự án hiện có một Gradle module Android tên :app, Kotlin và Jetpack Compose với Material 3.
- A-02 đã thêm Compose Navigation, theme Material 3 sáng/tối, thanh điều hướng thích ứng và sáu màn hình khung. Các màn Roleplay, Novel, Characters, Library và Settings hiện chỉ hiển thị trạng thái rỗng/thông tin; chưa có nghiệp vụ sản phẩm.
- Navigation nằm trong `core.navigation`; thành phần giao diện dùng chung ở `core.ui`; từng màn hình thuộc package `feature.*`. UI hiện chưa có ViewModel theo feature.
- Room, DataStore, OkHttp/SSE, kho bí mật và tính năng chat/soạn thảo thực tế chưa được triển khai.
- A-01 đã xác nhận build được trên cấu hình hiện tại. Smoke test trên điện thoại thật vẫn còn thiếu vì ADB chỉ có emulator API 37; xem docs/TASK_LOG.md.

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
- **Room:** lưu nhân vật, persona, chat, message, variants, dự án, chương, cảnh, revisions, memories, summaries và timeline theo schema backlog.
- **DataStore:** giữ tùy chọn giao diện, endpoint/model đã chọn và metadata cấu hình. Nếu giữ token mã hóa ở đây, token rõ chỉ tồn tại trong bộ nhớ khi cần gửi request.
- **AiProvider:** API có thể phát sự kiện nội dung, kết thúc, lỗi và hủy. OkHttp đọc SSE thành các sự kiện có kiểm soát; Fake provider tạo chunk, độ trễ và lỗi xác định.

## Tách Roleplay và Novel

- Roleplay context chỉ chứa chỉ dẫn Roleplay, nhân vật/persona đã chọn, ký ức có scope phù hợp, summary của nhánh đang chọn và các tin nhắn gần nhất.
- Novel context chỉ chứa Story Bible, timeline, chương/cảnh hiện hành, summary của dự án và yêu cầu biên tập.
- Hồ sơ nhân vật có thể được hai chế độ tham chiếu, nhưng message, memory, summary, branch và history không tự chia sẻ. Mọi liên kết cần khóa ngoại hoặc ID scope rõ ràng.
- Context Builder phải giới hạn ngân sách và ưu tiên canon đã xác nhận trước dữ liệu suy luận; phải có test chứng minh không lẫn hai dự án.

## Bảo mật và vòng đời dữ liệu

- API key không đặt trong resource, BuildConfig, repository, log, test fixture chia sẻ hay APK.
- Mỗi token được mã hóa bằng AES-GCM với khóa không xuất được do Android Keystore quản lý; chỉ lưu ciphertext và metadata cần thiết. Xóa token phải xóa ciphertext và alias/key liên quan theo quy trình an toàn.
- Room migration giữ lại dữ liệu cũ. Export schema, migration test và backup/restore có kiểm tra thuộc các task D trong backlog.
- Khi lưu bản nháp, revision mới hoặc stream đang chạy, transaction và trạng thái phải thể hiện dữ liệu nào hoàn chỉnh, dang dở hoặc lỗi.

## Build và kiểm chứng

- Dùng Gradle Wrapper, AGP, Kotlin Compose plugin, Compose BOM, SDK và JDK đã chốt trong README và docs/TASK_LOG.md.
- AGP 9.4 yêu cầu Gradle 9.6, hỗ trợ tối đa API 37 và Build Tools 36.0.0; các giá trị hiện tại khớp bảng AGP. Wrapper dùng Gradle daemon JDK 25. JDK 27 trên PATH chưa được Gradle 9.6 công bố hỗ trợ để chạy daemon, nên cần giữ cấu hình daemon 25.
- AGP 9 có Kotlin tích hợp và khai báo phụ thuộc KGP 2.2.10; Kotlin Compose plugin hiện cũng là 2.2.10. Build thực tế xác nhận cấu hình này biên dịch được. Không nâng phiên bản nếu chưa có lỗi tái hiện hoặc quyết định nâng cấp có kiểm thử.
- Compose BOM và AndroidX là các phiên bản dự án hiện tại; thay đổi chúng cần lý do và kiểm thử hồi quy.

## Nguồn tham khảo

- [AGP 9.4: tương thích với Gradle, SDK, Build Tools và JDK](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [AGP 9: Kotlin tích hợp và KGP đi kèm](https://developer.android.com/build/releases/agp-9-0-0-release-notes)
- [Gradle: ma trận tương thích JVM](https://docs.gradle.org/current/userguide/compatibility.html)
- [Android: kiến trúc offline-first](https://developer.android.com/topic/architecture/data-layer/offline-first)
