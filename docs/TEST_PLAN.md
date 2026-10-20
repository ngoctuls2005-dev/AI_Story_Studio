# Kế hoạch kiểm thử AI Story Studio

## Phạm vi và quy tắc báo cáo

Kế hoạch bao phủ nền tảng A-01 và các luồng V1.0 được nêu trong docs/BACKLOG_V1.md. Room hiện có kiểm thử schema/migration V1→V2, CRUD, scope và persistence; C-01 bổ sung Compose UI cho hồ sơ nhân vật/Persona. Các luồng chat, memory retrieval/Context Builder, backup và autosave vẫn chưa được triển khai.

- **PASS:** lệnh/kịch bản chạy xong và có kết quả quan sát được.
- **FAIL:** đã chạy nhưng có assertion, build hoặc hành vi không đạt.
- **BLOCKED:** không chạy được vì SDK, thiết bị, môi trường hoặc phụ thuộc còn thiếu; kèm bằng chứng.
- Ghi lệnh, phiên bản runtime, thiết bị/API level, artifact/report và lỗi còn lại vào docs/TASK_LOG.md.
- Không ghi API key hoặc nội dung riêng tư vào log; dùng dữ liệu giả và endpoint mock trong test tự động.

## Kiểm thử đã có ở A-01

| Loại | Lệnh/kịch bản | Mức chứng minh |
|---|---|---|
| Build debug | gradlew.bat assembleDebug | Xác nhận scaffold hiện tại biên dịch và tạo APK. |
| Unit test mẫu | gradlew.bat testDebugUnitTest | Một assertion số học mẫu; chưa kiểm tra nghiệp vụ ứng dụng. |
| Instrumentation mẫu | gradlew.bat connectedDebugAndroidTest trên emulator API 37 | Một test mẫu package name; chưa bao phủ UI hoặc persistence. |
| Lint | gradlew.bat lintDebug | Phân tích static cơ bản; xem report và phân biệt warning có sẵn với lỗi mới. |
| Cài/mở APK | adb install -r ... và mở launcher activity | Xác nhận APK cài/mở trên emulator. Tiêu chí điện thoại thật chỉ PASS khi chạy trực tiếp trên điện thoại. |

Kết quả theo từng lần chạy, kể cả trạng thái thiết bị thật, được lưu tại docs/TASK_LOG.md.

## Kiểm thử A-02 — giao diện và điều hướng

| Loại | Kịch bản | Kết quả cần xác nhận |
|---|---|---|
| Instrumentation Compose | Mở lần lượt `home`, `roleplay`, `novel`, `characters`, `library`, `settings` qua thanh điều hướng | Mỗi màn có marker hiển thị; không crash. |
| Instrumentation Compose | Mở Roleplay rồi gọi system Back | Trở lại Home. |
| Instrumentation Compose | Chọn Library rồi tạo lại Activity | Library vẫn được chọn. |
| Instrumentation Compose | Chọn Novel, chuyển Activity xuống CREATED rồi RESUMED | Novel vẫn được chọn. |
| Instrumentation Compose | Đổi theme state từ sáng sang tối với dynamic color tắt | Màu nền hai theme khác nhau. |
| Smoke thủ công | Cài và mở APK, chuyển sang Novel, bấm Home rồi mở lại Activity | Nội dung Novel hiện lại; không crash. |
| Responsive thủ công | Chụp màn hình ở viewport điện thoại và viewport rộng | Thanh dưới dùng ở màn hình hẹp, Navigation Rail dùng ở màn hình rộng; nội dung không tràn ngang. |

Kết quả A-02 hiện có trên AVD Medium Phone API 37; đây là bằng chứng emulator, không thay thế yêu cầu smoke trên điện thoại thật của A-01. Các màn hình chưa có trường nhập liệu nên kiểm thử che bàn phím chưa áp dụng; shell đã cấu hình `imePadding` và Activity dùng `adjustResize` để hỗ trợ khi các trường nhập liệu được bổ sung.

## Quy trình A-01

1. Kiểm tra git status, backlog, local.properties và các file secret để bảo đảm không stage dữ liệu cục bộ.
2. Kiểm tra Gradle Wrapper, AGP, Kotlin/Compose plugin, SDK Platform/Build Tools và JDK daemon.
3. Chạy gradlew.bat assembleDebug, sau đó testDebugUnitTest và lintDebug nếu môi trường cho phép.
4. Khi emulator sẵn sàng, chạy instrumentation qua `scripts/test-instrumentation.ps1` để chọn serial an toàn; Gradle `connectedDebugAndroidTest` có thể gỡ app data khỏi AVD sau test.
5. Kết nối điện thoại thật, xác nhận thiết bị device (không phải offline/unauthorized), cài APK và mở app. Ghi model và Android API level.
6. Kiểm tra repository không chứa local.properties, API key hoặc keystore; ghi SHA-256 APK/report nếu được tạo.

Lệnh Windows PowerShell:

    .\gradlew.bat assembleDebug
    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat lintDebug
    .\scripts\test-instrumentation.ps1 -DeviceSerial emulator-5554
    adb devices -l
    adb install -r .\app\build\outputs\apk\debug\app-debug.apk
    adb shell monkey -p com.hoangtu.aistorystudio -c android.intent.category.LAUNCHER 1

## Kiểm thử A-03 — Room nền tảng

Instrumentation tests nằm tại `app/src/androidTest/java/com/hoangtu/aistorystudio/core/database/` và chạy bằng AndroidJUnitRunner. Dữ liệu test dùng in-memory DB hoặc tên DB riêng có UUID; test persistence chỉ xóa chính DB tạm do test tạo.

| Kịch bản | Bằng chứng cần có |
|---|---|
| Profile/persona insert, read, update, delete | DAO/repository trả đúng bản ghi và trạng thái sau xóa. |
| Unicode | Tên/nội dung tiếng Việt có dấu được lưu và đọc nguyên vẹn. |
| Đóng/mở database | Bản ghi còn nguyên sau khi đóng instance và mở lại cùng tên DB test. |
| Cô lập hội thoại và project | Hai hội thoại không lẫn message; hai project không lẫn chapter, summary, timeline hoặc liên kết nhân vật. |
| Thứ tự xác định | Message theo sequence; chapter/scene theo thứ tự riêng của owner rồi ID; kết quả được assertion. |
| Khóa ngoại/chính sách xóa | Owner không tồn tại bị từ chối; character đang được conversation tham chiếu bị RESTRICT; xóa conversation cascade message. |
| Transaction rollback | Xung đột ID khi thêm message đầu làm rollback conversation mới; message gốc không đổi. |
| Memory scope/tombstone | Chỉ nhận đúng một owner hợp lệ; không chuyển memory đã có sang owner khác; truy vấn khác scope rỗng; xóa đặt `deleted_at` và giữ tombstone thay vì xóa vật lý. |
| Database schema | Room mở được DB V1; MigrationTestHelper tạo và validate schema export V1. |

Schema V1 được xuất vào `app/schemas/com.hoangtu.aistorystudio.core.database.StudioDatabase/1.json`. Chưa có migration thật nên version vẫn là 1. Khi schema chuyển V1→V2: giữ file JSON version 1, thay đổi schema/entity và tăng version lên 2 trong cùng thay đổi, thêm `Migration(1, 2)` có tên vào registry, tạo DB V1 với dữ liệu đại diện qua MigrationTestHelper, chạy `runMigrationsAndValidate(..., 2, true, MIGRATION_1_2)` rồi xác nhận giá trị, quan hệ, index và FK của các hàng cũ. Không dùng destructive fallback hoặc xóa DB hiện hành để làm test đạt.

## Kiểm thử A-04 — AiProvider và FakeAiProvider

Unit test JVM tại `app/src/test/java/com/hoangtu/aistorystudio/core/ai/FakeAiProviderTest.kt` dùng JUnit 4 và `kotlinx-coroutines-test` 1.9.0. Test fake chỉ dùng fixture cố định và Flow/coroutine; không có HTTP client, API key, endpoint hoặc yêu cầu Internet.

| Kịch bản | Assertion |
|---|---|
| Thành công / HTTP 200 tương đương | Ghép các text delta khớp chuỗi mong đợi và chỉ có đúng một `Completed`. |
| Unicode tiếng Việt | Dấu tiếng Việt và ký tự emoji không đổi sau khi nhận các chunk. |
| 401 / 429 / 500 | Lần lượt trả lỗi domain `Unauthorized`, `RateLimited`, `ServerError`; không làm lộ HTTP response thô. |
| Timeout | `FakeAiScenario.Timeout` phát lỗi timeout sau delay ảo; `TestCoroutineScheduler` chạy tới terminal mà không chờ thời gian thật. |
| Ngắt stream | Giữ partial text, phát đúng lỗi `Interrupted`, không phát `Completed`. |
| Response rỗng | Cả scenario rỗng và success không có chunk đều phát `EmptyResponse`. |
| Hủy giữa stream | Collector nhận partial đầu, cancellation truyền tới `onCompletion`, không nhận chunk tiếp theo/completion và Job kết thúc. |
| Hai request đồng thời | Scenario theo từng `AiRequest` cho kết quả độc lập, không trộn chunk. |
| Delay giữa chunk | `advanceTimeBy` xác nhận chunk sau chỉ xuất hiện khi scheduler tới hạn; không sleep đồng hồ thật. |

Các test này xác nhận provider fake và giao kèo luồng; **không** xác nhận Chat UI hoặc Novel Editor đang chạy offline. Hai consumer giao diện chưa được triển khai. Chạy suite không cần mạng bằng `gradlew.bat testDebugUnitTest --offline --console=plain` sau khi các dependency đã được cache.

## Kiểm thử A-05 — Cấu hình provider và API key

Unit test JVM tại `app/src/test/java/com/hoangtu/aistorystudio/core/ai/settings/` kiểm thử validation URL/tham số và `DataStoreAiProviderSettingsRepository` bằng Preferences DataStore tạm cùng `FakeSecureKeyStore`. Fake chỉ phục vụ test; không thay thế bằng chứng Keystore của Android.

| Kịch bản | Bằng chứng/assertion |
|---|---|
| Cấu hình mặc định | Provider Experiential Labs, URL HTTPS và model ban đầu xuất hiện; chưa có key. |
| URL | HTTPS hợp lệ được chấp nhận; HTTP mặc định bị từ chối, chỉ được chấp nhận sau xác nhận rõ; URL sai, URL có userinfo/query hoặc port sai bị từ chối. |
| Lưu/đọc và restart | DataStore file được đóng/reopen; repository mới đọc được cấu hình và key fixture từ ciphertext. Instrumentation riêng dùng Android Keystore thật, seed key, `adb shell am force-stop`, mở lại launcher rồi đọc key từ process mới. |
| Không lưu plaintext | Unit test quét bytes của Preferences file; Compose instrumentation kiểm tra DataStore file thật sau lưu không chứa fixture dạng rõ. |
| Nhiều provider và cập nhật | Hai provider đọc đúng key riêng; thay khóa trả khóa mới và xóa alias cũ; lựa chọn provider active được lưu. |
| Xóa key | Sau khi xóa xác nhận, đọc trả Missing, ciphertext không còn, cấu hình provider vẫn tồn tại. |
| Ciphertext/Keystore lỗi | Instrumentation sửa ciphertext thật và xác minh GCM từ chối; unit test mô phỏng `KEYSTORE_UNAVAILABLE` và `KEY_INVALIDATED`, repository báo cần nhập lại, giữ cấu hình/ciphertext và không lộ exception/secret. |
| Cục bộ và UI | Settings thêm/sửa, ẩn/hiện input, lưu, kiểm tra cục bộ, xác nhận xóa và trạng thái sau Activity recreation. UI ghi rõ không kiểm tra mạng. |
| Android Backup | Instrumentation parse resource rules, xác nhận cấu hình DataStore bị loại khỏi cloud backup, device transfer và full backup cũ. Không có tính năng backup thủ công trong A-05. |

Kết quả A-05 lần này: 11 unit test A-05 (cùng 8 test A-04 và 1 test mẫu); AndroidJUnitRunner trực tiếp trên AVD API 37 chạy 18 test tổng cộng, 0 failure. `connectedDebugAndroidTest` qua Gradle báo BUILD SUCCESSFUL nhưng worker không lấy được device API level từ ADB port riêng và report không đủ test result; không tính trạng thái task đó là PASS. Lệnh ADB runner trực tiếp cùng test process-restart được ghi trong `docs/TASK_LOG.md`; log nằm trong `app/build/outputs/androidTest-results/connected/debug/` (thư mục build bị ignore).

### Kiểm thử A-06 — MockWebServer và runner có xác minh số test

Unit test JVM dùng JUnit 4 và MockWebServer 5.4.0, chỉ qua `testImplementation`. Server bind loopback; không gọi Internet, provider thật hay API key. Fixture chung nằm tại `app/src/test/.../testutil/` và dùng ID/timestamp cố định cho Character, Persona, Conversation, ChatMessage, NovelProject, Chapter, Scene, MemoryFact, Ai request/response và provider configuration.

| Kịch bản | Assertion |
|---|---|
| HTTP 200/400/401/429/500 và body rỗng | Server trả đúng status; số request được xác nhận. |
| Response trì hoãn và timeout | Body thành công sau delay; timeout phát `InterruptedIOException` trong giới hạn, không treo call. |
| Socket đóng giữa body | Client nhận `IOException` khi đọc response, không coi stream bị cắt là đầy đủ. |
| UTF-8 và SSE raw | Chuỗi tiếng Việt giữ nguyên; `text/event-stream` gồm nhiều chunk nhỏ và event `[DONE]` giữ nguyên thứ tự. |
| Bộ đếm JUnit/AndroidJUnitRunner | Harness chấp nhận số test dương, từ chối 0 test, report lỗi và skip; runner trực tiếp cần `OK (N tests)` và trạng thái pass khớp N. |
| Cô lập fixture | ID và scope hội thoại/dự án tách biệt, response AI và cấu hình provider ổn định. |
| Không mạng | FakeAiProvider và unit tests chạy cục bộ; MockWebServer chỉ dùng loopback. Không tuyên bố Chat UI hoặc Novel Editor chạy offline vì consumer chưa được triển khai. |

Lệnh lặp lại trên Windows PowerShell:

    .\scripts\test-unit.ps1
    .\scripts\test-instrumentation.ps1 -DeviceSerial emulator-5554
    .\scripts\test-all.ps1 -DeviceSerial emulator-5554

Script kiểm tra JDK 25, SDK/ADB và emulator; giữ ADB server ở cổng chuẩn 5037. Mặc định build/cài APK bằng `adb -s` và chạy AndroidJUnitRunner trực tiếp để giữ app data; khi có nhiều device, serial emulator phải được chọn tường minh. Thiết bị vật lý không được chọn làm đích. `-UseGradleRunner` chỉ chạy khi có một emulator online duy nhất; AGP sẽ gỡ APK app/test sau lượt connected task nên có thể làm mất app data trên AVD. Nếu report Gradle thiếu test, script chạy fallback trực tiếp và vẫn yêu cầu kết quả runner có test count dương. Log và report chỉ được tính khi phát sinh trong lần chạy hiện tại. `test-harness.ps1` xác minh 0 test/skip bị từ chối.

Instrumentation regression bao gồm các test A-02, A-03, A-05 và test mẫu hiện hữu; test Compose đã có từ A-02. Không thêm bài test UI nghiệp vụ mới vì Chat/Novel UI thực tế chưa thuộc A-06. Test process restart tạo Preferences DataStore tạm trong cache của app với ID provider riêng, dùng Android Keystore thật, seed → force-stop → launch → verify rồi xóa key và tệp test; không thay đổi cấu hình provider người dùng.

Trước A-06, Gradle từng hoàn tất với report 0 test khi Gradle worker và lệnh ADB dùng hai server port; lần chạy A-06 trên AVD cùng ADB mặc định 5037 tạo report 18 test. Lượt Gradle này gỡ APK app/test khỏi AVD theo log AGP; không có điện thoại vật lý online. Vì vậy runner trực tiếp trở thành mặc định để giữ app data. Không dựa vào dòng BUILD SUCCESSFUL nếu không có report/test count hợp lệ.

## Ma trận V1.0 sau khi có tính năng

| Tầng | Kịch bản chính | Tiêu chí |
|---|---|---|
| Unit | Context Builder, validation, SSE parsing, memory retrieval, token budget, export formatting | Fixture xác định; tiếng Việt Unicode không mất/lặp ký tự; scope và thứ tự ưu tiên đúng. |
| Repository/Room | CRUD, FK/index, transaction rollback, reopen DB, revisions, schema migration | Không trùng ID; transaction lỗi rollback; dữ liệu V1 được giữ nguyên khi nâng schema. |
| Network | Fake provider và MockWebServer: 200, 400, 401, 429, 500, timeout, EOF, hủy, UTF-8/SSE chunk split | Lỗi có thể phục hồi; stream không trùng/mất text; hủy đóng tài nguyên; không lộ key. |
| Compose/UI | Điều hướng sáu màn, Dark Mode, gửi/Stop/Retry, auto-scroll, editor/selection, keyboard, rotate, pause/resume | Không crash/ANR; trạng thái phù hợp; khi đọc nội dung cũ không bị cuộn cưỡng bức. |
| Persistence | Force-stop giữa chat stream; autosave sau debounce; khởi động lại app | Tin người dùng và partial có trạng thái đúng; bản thảo đã báo Saved còn nguyên. |
| Bảo mật | Quét source/log/APK artifacts, lưu/xóa API key, sai key, keystore invalidation | Không có plaintext key; lỗi khóa được báo rõ và có luồng nhập lại. |
| Backup/cập nhật | Backup/restore đúng, sai mật khẩu, checksum hỏng, cài APK V1 rồi cập nhật V2 | File lỗi không đổi DB hiện hành; cùng applicationId/certificate, versionCode tăng; giữ nguyên nội dung. |
| Thiết bị | Android 12 và 13 theo backlog; thêm API khác khi có thiết bị | Luồng trọng yếu chạy trên thiết bị thật; ghi model/API, kết quả và issue cụ thể. |
| Stress | 100+ chat turns, novel 20 chương/>200.000 ký tự, nhiều revisions | Không mất dữ liệu/ANR; lưu số đo baseline và ghi rõ cấu hình thiết bị. |

## Kiểm thử C-01 — hồ sơ nhân vật và Persona

| Loại | Kịch bản | Bằng chứng bắt buộc |
|---|---|---|
| Unit | Tên bắt buộc, tuổi dạng số và giới hạn tuổi trưởng thành 18–120; tên Persona hợp lệ | JUnit với kết quả lỗi xác định. |
| Room/instrumentation | Tạo, đọc, sửa, xóa; Unicode; trường tuổi/ngoại hình/bối cảnh/tính cách/xưng hô/quy tắc | Entity đọc lại khớp nội dung, không ghi dữ liệu khi validation thất bại. |
| Persona | Tạo/sửa/xóa, chuyển Persona mặc định, chọn ID không tồn tại | Tối đa một Persona mặc định; ID sai không làm mất lựa chọn cũ. |
| Foreign Key | Xóa hồ sơ/Persona đang có hội thoại tham chiếu | Repository trả `REFERENCED`; bản ghi cha và hội thoại còn nguyên. |
| Compose UI | Tạo, xem đầy đủ, sửa, xác nhận xóa, tìm theo tên và validation | Compose test trên emulator với in-memory Room; không truy vấn DAO trong Composable. |
| Khởi động lại | Seed database test riêng → force-stop app → mở lại → verify → cleanup | AndroidJUnitRunner trên emulator; database có tên fixture ngẫu nhiên, không chạm DB người dùng. |

## Kiểm thử C-02 — danh sách hội thoại và lịch sử tin nhắn

Instrumentation dùng in-memory Room fixture với hai Character, hai Persona, ba hội thoại và tối thiểu hai message cho mỗi hội thoại. Bài process-restart dùng database có tên `c02_restart_<UUID>.db` trong app-private storage; seed → force-stop → mở launcher process mới → verify → cleanup. Không dùng database production và không xóa app data.

| Loại | Kịch bản | Bằng chứng/assertion |
|---|---|---|
| Repository/Room | Tạo và truy vấn ba hội thoại độc lập | Mỗi ID trả đúng tin nhắn; message role/status/content/Unicode còn nguyên; hai hội thoại khác không xuất hiện. |
| Thứ tự | Tạo các message có cùng `createdAt` | Repository trả `sequence ASC, id ASC`; unique sequence theo hội thoại vẫn được giữ. |
| Tìm kiếm | Tìm theo tiêu đề, tên Character và Persona | Chỉ trả hội thoại phù hợp; danh sách được sắp `updated_at DESC, created_at DESC, id ASC`. |
| Owner/Persona | Generic save thử đổi Character hoặc Persona sau khi tạo | Repository từ chối và hàng gốc không đổi; tạo mới dùng đúng selection, không sửa các cuộc hội thoại cũ. |
| Foreign key/transaction | Tạo hội thoại trỏ tới Character/Persona thiếu hoặc trùng ID | Room từ chối; không có hàng dở dang và không ghi đè cuộc hội thoại hiện hữu. |
| Hoạt động/xóa | Thêm message mới; xác nhận xóa một hội thoại | `updated_at` không giảm; xóa chỉ cascade message của owner đã chọn, các owner khác còn nguyên. |
| Compose/Navigation | Tìm, mở lịch sử, tạo hội thoại với Character/Persona, Back hệ thống, xác nhận/hủy xóa | Nested route mở đúng ID, back quay về danh sách, list và Persona cũ giữ nguyên; UI không có composer AI trong C-02. |
| Khởi động lại | Seed ba hội thoại và nhiều message vào DB fixture → force-stop → launcher → verify → cleanup | Số lượng, liên kết, Unicode, scope và sequence sau process mới bằng fixture trước đó. |

Lệnh hồi quy trên Windows: `.\scripts\test-all.ps1 -DeviceSerial emulator-5554`. Script chỉ chấp nhận emulator và chạy AndroidJUnitRunner trực tiếp bằng `adb -s`; không dùng `connectedDebugAndroidTest`, `uninstall` hoặc `pm clear`. C-02 không đổi schema V2 và không tạo migration mới.

## Test dữ liệu và migration

- Dùng fixture riêng, định danh ổn định, không dựa vào DB thật của người dùng.
- Mỗi schema Room cần schema export vào repository, migration có tên và MigrationTestHelper xác nhận bản ghi/nội dung trước-sau khi xuất hiện migration thật. C-01 thêm migration V1→V2; dữ liệu V1 của nhân vật, Persona và hội thoại phải được kiểm tra trước/sau.
- Cấm destructive migration fallback. Không xóa DB thật để làm test pass.
- Kiểm thử trộn scope với tối thiểu hai nhân vật, hai cuộc hội thoại, hai novel; kết quả truy vấn phải giữ đúng owner và project.

## Tiêu chí phát hành

Áp dụng Gate G0..G5 và T-01..T-14 trong backlog. Chất lượng câu văn và tính nhất quán của mô hình AI cần thêm đánh giá thủ công; test deterministic chỉ xác nhận dữ liệu/context được chọn chính xác, không chứng minh model luôn trả lời đúng.

## Kiểm thử D-06 — ký release và cập nhật APK

- Build debug bằng `.\gradlew.bat assembleDebug`; debug dùng certificate riêng do Android SDK tạo.
- Build release bằng `.\scripts\build-release.ps1`. Script nhập đường dẫn/alias và mật khẩu trực tiếp qua prompt cục bộ, truyền mật khẩu trong process environment, dùng `--no-configuration-cache` và xác minh release APK bằng `apksigner`. Không ghi password vào command line, log hay repository.
- `app.versionCode` bắt đầu ở 1 trong `gradle.properties`; trước mỗi APK phát hành, tăng số này lên giá trị lớn hơn số cao nhất đã cài/phân phối. Không giảm hoặc tái sử dụng code. Override `-Papp.versionCode=2` chỉ phục vụ test V1→V2.
- Chuẩn bị một AVD mới/cô lập, xác nhận app ID chưa cài, rồi chạy `.\scripts\test-release-upgrade.ps1 -DeviceSerial <emulator-serial>`. Script build V1/V2 bằng cùng keystore, dùng `aapt` xác nhận `com.hoangtu.aistorystudio` và version code, `apksigner` xác nhận chữ ký/fingerprint, so chứng chỉ debug/release, cài V1 rồi cài V2 với `adb install -r`.
- Một test instrumentation lưu marker tổng hợp trong `targetContext.cacheDir` trước khi cài đè và đọc/xóa marker sau cập nhật. Đây là bằng chứng dữ liệu app-private còn tồn tại qua package update; chưa thay thế D-07 kiểm thử đầy đủ Room fixture/migration. Test không tạo hoặc thay đổi migration Room.
- Script chỉ nhận serial emulator, dừng nếu ứng dụng đã cài, và không dùng uninstall/clear data. Không chạy trên điện thoại thật hoặc emulator chứa dữ liệu cá nhân. Nếu không có emulator riêng, trạng thái cài đè và giữ dữ liệu phải ghi BLOCKED.
- Test APK được lấy từ androidTest debug variant rồi ký với chứng chỉ test release để instrumentation có thể chạy trong package release; APK này chỉ là harness kiểm thử. R8/shrinking release tắt do APK tối ưu trước đó crash runtime; chỉ bật lại sau khi có test runtime release.
- Thông tin tạo keystore và sao lưu an toàn được ghi trong `docs/RELEASE_SIGNING.md`; khóa thật do người dùng quản lý và không được đưa vào Git.
