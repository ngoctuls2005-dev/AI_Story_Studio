# Kế hoạch kiểm thử AI Story Studio

## Phạm vi và quy tắc báo cáo

Kế hoạch bao phủ nền tảng A-01 và các luồng V1.0 được nêu trong docs/BACKLOG_V1.md. Kiểm thử chức năng sẽ được thêm cùng task triển khai. Hiện ứng dụng còn là Compose scaffold; test mẫu hiện tại không chứng minh chat, Room, memory, backup hay autosave hoạt động.

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
- Mỗi schema Room cần schema export vào repository, migration có tên và MigrationTestHelper xác nhận bản ghi/nội dung trước-sau.
- Cấm destructive migration fallback. Không xóa DB thật để làm test pass.
- Kiểm thử trộn scope với tối thiểu hai nhân vật, hai cuộc hội thoại, hai novel; kết quả truy vấn phải giữ đúng owner và project.

## Tiêu chí phát hành

Áp dụng Gate G0..G5 và T-01..T-14 trong backlog. Chất lượng câu văn và tính nhất quán của mô hình AI cần thêm đánh giá thủ công; test deterministic chỉ xác nhận dữ liệu/context được chọn chính xác, không chứng minh model luôn trả lời đúng.
