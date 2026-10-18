# Kế hoạch kiểm thử AI Story Studio

## Phạm vi và quy tắc báo cáo

Kế hoạch bao phủ nền tảng A-01 và các luồng V1.0 được nêu trong docs/BACKLOG_V1.md. A-03 hiện có instrumentation test thực cho thao tác Room, scope, persistence và schema V1; các luồng chat, memory retrieval/Context Builder, backup và autosave vẫn chưa được triển khai.

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
4. Khi emulator sẵn sàng, chạy connectedDebugAndroidTest, cài APK và mở launcher activity.
5. Kết nối điện thoại thật, xác nhận thiết bị device (không phải offline/unauthorized), cài APK và mở app. Ghi model và Android API level.
6. Kiểm tra repository không chứa local.properties, API key hoặc keystore; ghi SHA-256 APK/report nếu được tạo.

Lệnh Windows PowerShell:

    .\gradlew.bat assembleDebug
    .\gradlew.bat testDebugUnitTest
    .\gradlew.bat lintDebug
    .\gradlew.bat connectedDebugAndroidTest
    adb devices -l
    adb install -r .\app\build\outputs\apk\debug\app-debug.apk
    adb shell monkey -p com.hoangtu.aistorystudio -c android.intent.category.LAUNCHER 1

## Kiểm thử A-03 — Room V1

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

## Test dữ liệu và migration

- Dùng fixture riêng, định danh ổn định, không dựa vào DB thật của người dùng.
- Mỗi schema Room cần schema export vào repository, migration có tên và MigrationTestHelper xác nhận bản ghi/nội dung trước-sau khi xuất hiện migration thật. A-03 đã xác nhận tạo/validate schema V1; chưa giả lập đường nâng version.
- Cấm destructive migration fallback. Không xóa DB thật để làm test pass.
- Kiểm thử trộn scope với tối thiểu hai nhân vật, hai cuộc hội thoại, hai novel; kết quả truy vấn phải giữ đúng owner và project.

## Tiêu chí phát hành

Áp dụng Gate G0..G5 và T-01..T-14 trong backlog. Chất lượng câu văn và tính nhất quán của mô hình AI cần thêm đánh giá thủ công; test deterministic chỉ xác nhận dữ liệu/context được chọn chính xác, không chứng minh model luôn trả lời đúng.
