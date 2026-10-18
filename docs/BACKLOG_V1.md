# AI Story Studio — Backlog kỹ thuật V1.0

**Ngày cập nhật:** 18/10/2026  
**Mục tiêu:** APK Android cá nhân; Kotlin + Jetpack Compose, Codex + Android Studio; không cần thuê backend.  
**Phạm vi:** Hai chế độ Roleplay / Novel; dùng chung hồ sơ nhân vật nhưng tách biệt lịch sử và ngữ cảnh; AI Provider tương thích OpenAI, khóa API do người dùng tự nhập.  
**Ưu tiên:** **P0** = bắt buộc, bảo vệ dữ liệu và ổn định; **P1** = chức năng cốt lõi V1.0; **P2** = cải tiến sau V1.0.  
**Trạng thái:** Tất cả nhiệm vụ dưới đây ở trạng thái **TODO**; chưa giả định đã triển khai.

## 0. Phạm vi, kiến trúc, quy tắc nghiệm thu

**V1.0 bắt buộc có:** (1) trò chuyện AI có streaming, chỉnh sửa/tạo lại câu trả lời và xử lý lỗi mạng; (2) trí nhớ sự kiện có thể xem, sửa, ghim và truy xuất đúng bối cảnh; (3) dự án tiểu thuyết nhiều chương, trình soạn thảo, autosave, viết tiếp/viết lại và lịch sử phiên bản; (4) Room migration, sao lưu/phục hồi và cập nhật APK mà không xóa dữ liệu.

**Ngoài phạm vi V1.0:** giọng nói/TTS, avatar động, tạo ảnh, cloud sync, đăng nhập, mạng xã hội, thanh toán, backend riêng, vector database và embedding cloud. Hỗ trợ chủ đề trưởng thành với các nhân vật trưởng thành; không cần thiết kế riêng hệ thống phát hành công khai.

**Stack:** Kotlin, Jetpack Compose + Material 3, MVVM + Repository + Use Cases, Coroutines/Flow, Room, DataStore, OkHttp/SSE, Kotlin Serialization, Android Keystore, Storage Access Framework (SAF); bắt đầu bằng một Gradle app module và chia package theo feature. Không khóa cứng vào một provider/model. Kiểm thử thiết bị Android 12 và 13; chọn minSdk sau khi kiểm tra dự án thực tế.

**Definition of Done (mọi nhiệm vụ):**
1. Có code, test và bằng chứng build/test thực tế. Nếu môi trường không cho phép chạy test, ghi rõ **BLOCKED**, không tự nhận PASS.
2. Không crash/ANR, không truy vấn DB/network trên main thread, không có lỗi lint nghiêm trọng mới.
3. Không ghi API key, nội dung riêng tư hoặc thông tin nhạy cảm vào log, repository, artifact báo lỗi.
4. Khi đổi schema: lưu schema được xuất, viết migration và test; **không dùng destructive migration fallback**.
5. Cập nhật `docs/TASK_LOG.md`: mã nhiệm vụ, file sửa, lý do, test đã chạy và kết quả, rủi ro còn lại. Mỗi nhiệm vụ có checkpoint Git để hoàn tác.
6. Chỉ chuyển nhiệm vụ tiếp theo sau khi đạt tiêu chí nghiệm thu của nhiệm vụ hiện tại.

## 1. Backlog chi tiết

### EPIC A — Nền tảng và cấu trúc dự án (Gate G0)

| ID | Ưu tiên | Công việc / phụ thuộc | Tiêu chí nghiệm thu có thể kiểm tra |
|---|---|---|---|
| **A-01** | P0 | Tạo dự án Kotlin/Compose; cấu hình Gradle/JDK/SDK tương thích; `.gitignore`, README, Git. | `assembleDebug` thành công; APK mở trên điện thoại thật; không commit `local.properties`, API key hoặc `.jks`. |
| **A-02** | P1 | Material 3, Dark Mode, Navigation; màn Home, Roleplay, Novel, Characters, Library, Settings. Sau A-01. | Chuyển qua lại 6 màn hình, đổi app rồi quay lại đúng trạng thái; không crash. |
| **A-03** | P0 | Room schema V1, khóa ngoại/index và repository transaction: nhân vật, persona, chat, messages, variants, novel, chapter, scene, revisions, memories, summaries, timeline. | CRUD và ràng buộc FK pass; tắt app/mở lại dữ liệu còn; không trùng ID; transaction lỗi thì rollback. |
| **A-04** | P0 | `AiProvider` interface, `FakeAiProvider` tạo stream có độ trễ/lỗi chủ động để kiểm thử không dùng mạng. | Có thể thử chat và editor hoàn toàn offline, mô phỏng 200/401/429/500/timeout/ngắt stream. |
| **A-05** | P0 | Cấu hình API theo provider/model; lưu khóa qua AES-GCM với khóa được quản lý trong Android Keystore; metadata trong DataStore. | Key không ở dạng rõ trong DB/log/repo; khởi động lại dùng được; có nút kiểm tra và xóa key. |
| **A-06** | P0 | Viết `AGENTS.md`, `ARCHITECTURE.md`, `DECISIONS.md`, `TEST_PLAN.md`, `TASK_LOG.md`; cấu hình unit/Compose tests và MockWebServer. | Lệnh build/test chạy lại được; Codex đọc được AGENTS; có test runner và checklist chuẩn. |

**Gate G0:** A-01 đến A-06, đồng thời hoàn thành **D-01** (schema/migration từ đầu) và **D-06** (khóa ký trước bản release đầu tiên).

### EPIC C — Hội thoại AI mượt, bền vững (Gate G1)

| ID | Ưu tiên | Công việc / phụ thuộc | Tiêu chí nghiệm thu có thể kiểm tra |
|---|---|---|---|
| **C-01** | P1 | CRUD nhân vật/persona: hồ sơ, tuổi trưởng thành, tính cách, xưng hô, quy tắc nhập vai. Phụ thuộc A-03. | Tạo/sửa/xóa, chọn persona, tắt/mở app vẫn còn dữ liệu; prompt không tự sửa hồ sơ gốc. |
| **C-02** | P1 | Nhiều cuộc hội thoại độc lập; danh sách, tìm và mở lại lịch sử; lưu `role/content/status/createdAt`. | 3 chat của 2 nhân vật không lẫn tin nhắn; thứ tự ổn định; mở lại đúng lịch sử. |
| **C-03** | P1 | Provider tương thích OpenAI; URL/model/tham số cấu hình, xác thực kết nối và xử lý response HTTP. | Mock test 200/400/401/429/500; kiểm tra thực tế bằng khóa riêng (nếu khả dụng); không log khóa. |
| **C-04** | P0 | Parser SSE chịu được chunk bị cắt giữa byte UTF-8, JSON và dòng; `[DONE]`, EOF, cancellation. Phụ thuộc A-04, C-03. | Ít nhất 20 fixture bao gồm tiếng Việt bị tách byte; chuỗi cuối đúng, không mất/trùng ký tự; hủy đóng tài nguyên. |
| **C-05** | P1 | Chat UI: tin nhắn streaming, ô nhập, Stop, Retry, auto-scroll có điều kiện, ViewModel + StateFlow. | Tin nhắn hiện dần; không chặn main thread; Stop ngừng cập nhật; khi đang đọc tin cũ không bị giật xuống cuối. |
| **C-06** | P0 | Xử lý mất mạng, timeout, HTTP 401/429/5xx, response rỗng, retry; chống gửi trùng khi bấm liên tiếp. | 10 tình huống lỗi giả lập đều có thông báo và đường phục hồi; không crash, không nhân đôi tin người dùng. |
| **C-07** | P0 | Lưu user message trước request; AI message có trạng thái `streaming/interrupted/completed/failed`; ghi transaction và khôi phục partial. | Force-stop giữa stream: user message và phần AI đã lưu còn nguyên, AI partial không bị hiển thị nhầm là hoàn chỉnh. |
| **C-08** | P1 | Sửa tin nhắn, regenerate, lưu variants và nhánh hội thoại; ContextBuilder chỉ đọc nhánh đang chọn. | Đổi nhánh không xóa nhánh cũ; đổi variants được; prompt không lấy nội dung của nhánh không active. |

**Gate G1 — Chat MVP:** Kiểm tra 100+ lượt chat, 3 phiên độc lập, 10 lỗi mạng và force-stop giữa stream. Không crash/ANR hay mất lịch sử. Đo độ mượt UI bằng fake provider; không đặt cam kết tốc độ cho server AI bên ngoài.

### EPIC M — Bộ nhớ dài hạn và tính nhất quán (Gate G2)

| ID | Ưu tiên | Công việc / phụ thuộc | Tiêu chí nghiệm thu có thể kiểm tra |
|---|---|---|---|
| **M-01** | P0 | Thiết kế memory fact có owner/scope (nhân vật, cuộc trò chuyện, novel), nguồn, timestamp, status, pinned, tombstone; summaries có phạm vi tin nhắn. | FK/scope đúng; query không lấy ký ức của dự án khác. |
| **M-02** | P1 | Màn xem/thêm/sửa/ghim/xóa ký ức; phân biệt **canon** do người dùng xác nhận và **inferred** do AI suy đoán. | 10 ký ức giữ nguyên sau khởi động lại; thay đổi phản ánh ở prompt sau; xóa không tự tái tạo khi chưa được duyệt. |
| **M-03** | P1 | AI đề xuất memory candidate theo JSON schema; xác thực dữ liệu và cho người dùng Accept/Reject. | JSON lỗi không ghi DB; suy đoán không biến thành sự thật; không âm thầm chỉnh sửa hồ sơ nhân vật. |
| **M-04** | P1 | Retrieval: FTS/keyword + phạm vi + pinned + liên quan + thời gian; trả các fact phù hợp ngân sách token. | Bộ 20 câu hỏi mẫu truy xuất đúng fact >=18/20; **20/20 không trộn ký ức giữa hai dự án**. |
| **M-05** | P1 | Tóm tắt hội thoại theo mốc và vùng bao phủ, chống tóm tắt trùng và ghi đè canon; lưu lịch sử summary. | Trên 200 messages, không summary trùng vùng; canon vẫn còn sau nhiều lần nén. |
| **M-06** | P0 | `ContextBuilder` tách Roleplay/Novel, thứ tự ưu tiên: quy tắc chế độ, canon, facts phù hợp, summary, tin gần đây; giới hạn ngân sách token. | Test vượt budget vẫn giữ canon ghim, loại bớt phần ít liên quan; không nhập nhằng active branch hay novel. |
| **M-07** | P1 | Bộ benchmark 20 tình huống về tính cách, quan hệ, timeline, thông tin mâu thuẫn; đánh giá retrieval và nội dung trả lời. | Retrieval đạt M-04; lưu kết quả và lỗi AI riêng, không tuyên bố model nhớ đúng 100%; regression sau mỗi lần đổi prompt. |

**Gate G2:** Bộ test tự động xác nhận việc **chọn đúng thông tin**; chất lượng câu trả lời của model còn phải kiểm tra thủ công vì AI vẫn có thể suy đoán hoặc nhớ sai.

### EPIC N — Sáng tác và biên tập tiểu thuyết (Gate G3)

| ID | Ưu tiên | Công việc / phụ thuộc | Tiêu chí nghiệm thu có thể kiểm tra |
|---|---|---|---|
| **N-01** | P1 | CRUD dự án tiểu thuyết, Story Bible, dàn ý, chương, cảnh, sắp xếp thứ tự; nhân vật liên kết theo scope. | 3 dự án, mỗi dự án 10+ chương; đổi tên/sắp xếp được; không lấy lịch sử roleplay khi chưa chọn. |
| **N-02** | P1 | Editor hỗ trợ tiếng Việt Unicode, selection, Undo/Redo, đếm từ/ký tự, soạn offline. | Nhập/dán 20.000 ký tự trong một chương, cuộn/sửa/lưu ổn, không crash khi xoay màn. |
| **N-03** | P0 | Autosave nội dung với debounce và trạng thái Saved/Unsaved; mục tiêu lưu trong <=2 giây sau lần gõ cuối; xử lý lifecycle và race. | Gõ, đợi trạng thái Saved, force-stop rồi mở lại: văn bản đầy đủ; bản cũ không ghi đè bản mới. |
| **N-04** | P1 | Revisions/snapshots trước khi chấp nhận AI rewrite; khôi phục bản cũ và Undo/Redo. | Viết lại 5 lần một cảnh, khôi phục được từng bản; bản gốc không bị xóa. |
| **N-05** | P1 | Tác vụ AI `continue`, `rewrite selection`, `expand`, `shorten`, `outline`, `suggest`; preview → Accept/Discard. | 6 tác vụ chạy với fake provider; Discard không thay đổi bản thảo; Accept chỉ thay đoạn đã chọn. |
| **N-06** | P1 | Context tiểu thuyết riêng: Story Bible, summaries chương, timeline, vị trí cảnh, ngôi kể/văn phong. | Viết chương mới nhận đúng canon và diễn biến của dự án; không mang ngữ cảnh novel khác. |
| **N-07** | P1 | Xuất TXT và Markdown (UTF-8), theo thứ tự chương; chọn nơi lưu bằng SAF. | Tệp mở được trên máy tính; dấu tiếng Việt, tiêu đề và thứ tự chương giữ nguyên; không cần cấp quyền toàn bộ bộ nhớ. |
| **N-08** | P1 | Stress test 20 chương, >200.000 ký tự, sửa lại nhiều lần; tối ưu truy vấn, lazy rendering. | Không ANR khi mở/chuyển/sửa chương trên thiết bị thật; có số đo baseline để theo dõi hồi quy. |

**Gate G3:** Tạo dàn ý, 20 chương, sửa offline, dùng AI viết lại một cảnh, khôi phục bản cũ và xuất Markdown; đóng/mở app không mất bản thảo đã lưu.

### EPIC D — Bảo vệ dữ liệu và cập nhật APK (Gate G4)

| ID | Ưu tiên | Công việc / phụ thuộc | Tiêu chí nghiệm thu có thể kiểm tra |
|---|---|---|---|
| **D-01** | P0 | Room `exportSchema=true`, lưu schema trong repo, có migration test ngay từ V1; không `fallbackToDestructiveMigration`. Làm ngay sau A-03. | Fixture tạo DB V1 có dữ liệu → nâng schema V2 bằng `MigrationTestHelper` → so sánh đủ bản ghi/nội dung, PASS. |
| **D-02** | P0 | Định dạng backup có `manifest.json` (format/schema version, ngày, checksum), các entities, revisions, summaries; snapshot nhất quán. | Backup → restore round-trip trùng số lượng/nội dung; file sai checksum bị từ chối; không copy DB đang ghi tùy tiện. |
| **D-03** | P0 | Sao lưu thủ công qua SAF, mã hóa archive bằng mật khẩu sử dụng authenticated encryption + KDF thích hợp; loại trừ API key/Keystore key. | Đúng mật khẩu giải mã thành công; sai mật khẩu/chỉnh một byte thì thất bại; dữ liệu gốc không bị sửa. |
| **D-04** | P0 | Phục hồi theo luồng: chọn tệp → giải mã/xác minh → staging → xác nhận **Replace** → áp dụng transaction/rollback. | File hỏng/không tương thích không làm thay đổi DB hiện tại; restore hợp lệ khôi phục được nhân vật, chat, truyện, trí nhớ. |
| **D-05** | P0 | Cấu hình Android Auto Backup/dataExtractionRules tương ứng phiên bản; chặn tự sao lưu bí mật hoặc dữ liệu truyện trái ý người dùng. | Kiểm tra manifest và rules cho Android 12/13; API key không nằm trong backup; có hướng dẫn backup thủ công. |
| **D-06** | P0 | Chốt `applicationId`, tạo khóa ký release, backup keystore ngoài repo, tăng `versionCode` mỗi bản. Làm trước release đầu. | APK release V1/V2 cùng ID và chứng chỉ, code tăng; `adb install -r` cập nhật được mà không uninstall. |
| **D-07** | P0 | Kiểm thử nâng cấp trên fixture: 3 nhân vật, 3 chat, 200 messages, 2 novels × 10 chương, 20 memory facts, 5 revisions. | Cài release V1 → cập nhật release V2: không mất một bản ghi/nội dung; migration pass; không cần xóa dữ liệu app. |

**Gate G4:** Không xuất V1.0 nếu bất kỳ D-01..D-07 không đạt. Việc uninstall/clear storage/hỏng điện thoại vẫn có thể làm mất dữ liệu nếu không có bản sao lưu hợp lệ; kiểm thử cập nhật không phải bảo đảm tuyệt đối trước mọi sự cố.

### EPIC Q — Kiểm thử cuối cùng và đóng gói (Gate G5)

| ID | Ưu tiên | Công việc / phụ thuộc | Tiêu chí nghiệm thu có thể kiểm tra |
|---|---|---|---|
| **Q-01** | P0 | Chạy lại unit/integration: SSE, retrieval scope, budget, repositories, transactions, migrations, backup crypto/checksum. | Tất cả test tự động PASS với fake service và dữ liệu kiểm thử cố định; lưu báo cáo kết quả. |
| **Q-02** | P0 | Android instrumentation/Compose UI: chat, editor, pause/resume, rotate, process death, airplane mode, keyboard. | Chạy trên Android 12 và 13; toàn bộ luồng trọng yếu PASS; không còn lỗi P0/P1 khi phát hành. |
| **Q-03** | P0 | Security/performance audit: redaction, leak, StrictMode, main-thread I/O, chat/editor tải nặng, release R8 nếu sử dụng. | Không API key/dữ liệu truyện riêng tư trong log/artifact; không ANR trong stress test; theo dõi baseline hiệu năng. |
| **Q-04** | P0 | Tạo signed release APK, release notes, checksum SHA-256, hướng dẫn cài mới/nâng cấp/restore và lưu khóa ký an toàn. | Cài và cập nhật trên 2 điện thoại; D-07 pass; có checklist và bản backup xác minh được. |

## 2. Thứ tự thực hiện, cổng chất lượng

- **G0:** A-01 → A-02/A-03/A-04/A-05/A-06 → D-01 và D-06. Nền tảng, dữ liệu, bảo mật và signing.
- **G1:** C-01 → C-02; C-03 → C-04 → C-05 → C-06/C-07 → C-08. Có bản chat MVP chạy được.
- **G2:** M-01 → M-02/M-03 → M-04/M-05 → M-06 → M-07. Trí nhớ kiểm thử được.
- **G3:** N-01 → N-02/N-03/N-04 → N-05/N-06 → N-07/N-08. Có hai chế độ hoàn chỉnh.
- **G4:** D-02 → D-03 → D-04 → D-05/D-07. D-01, D-06 đã làm từ G0.
- **G5:** Q-01 → Q-02 → Q-03 → Q-04. Release V1.0.

**Quy tắc:** Giao Codex **một ID mỗi lần**. Chỉ chuyển task sau khi build/test và ghi bằng chứng. Không để AI tự triển khai hàng loạt mà chưa nghiệm thu.

## 3. Ma trận kiểm thử V1.0

| ID | Kịch bản | Kết quả bắt buộc |
|---|---|---|
| T-01 | SSE 1.000 fragments, chia giữa byte UTF-8/JSON | Không mất/trùng ký tự; text cuối đúng. |
| T-02 | Ngắt mạng sau một phần stream | Partial được đánh dấu, Retry hoạt động, app không crash. |
| T-03 | Đổi model; HTTP 401/429/500 và timeout | Thông báo rõ, không lộ key, không gửi tin trùng. |
| T-04 | Mở 3 cuộc hội thoại xen kẽ | Không trộn tin nhắn, branch, trí nhớ. |
| T-05 | Tạo 100+ messages rồi force-stop | Giữ lịch sử đã lưu; partial không thành completed. |
| T-06 | 20 truy vấn fact thuộc 2 dự án | >=18/20 retrieval đúng; 0/20 rò rỉ ký ức chéo. |
| T-07 | AI đề xuất fact trái với canon | Không ghi đè canon, yêu cầu duyệt. |
| T-08 | Novel 20 chương, hơn 200.000 ký tự | Không ANR; đọc/sửa/lưu/chuyển chương được. |
| T-09 | Gõ, đợi báo Saved, force-stop | Bản nháp đã báo Saved còn nguyên. |
| T-10 | Rewrite → Discard; rewrite → Restore | Discard không đổi bản gốc; Restore đúng snapshot. |
| T-11 | Backup hợp lệ / sai mật khẩu / file hỏng | Đúng thì restore; sai thì từ chối, DB cũ nguyên vẹn. |
| T-12 | Cài release V1 → V2 có migration | Không uninstall; không mất bản ghi/nội dung. |
| T-13 | Không có Internet | Đọc/sửa truyện và xem lịch sử vẫn được; AI báo offline. |
| T-14 | Kiểm tra log, Git, archive | Không có key trong log/repo/archive; backup được mã hóa. |

Chất lượng văn xuôi, độ hợp lý cảm xúc và khả năng duy trì tính cách **cần đánh giá thủ công** bên cạnh bài test deterministic; không thể đảm bảo model không bao giờ suy đoán sai.

## 4. Quy trình cập nhật APK an toàn

1. Chốt `applicationId` ngay từ bản đầu; dùng keystore release riêng, lưu bản sao ở nơi an toàn ngoài repository.
2. Với mỗi bản release, **giữ cùng certificate** và tăng `versionCode`. APK debug thường được ký bằng khóa debug khác nên không thể dùng nó để cập nhật trực tiếp bản release.
3. Trước khi nâng cấp bản quan trọng, **xuất một bản sao lưu và thử phục hồi trên dữ liệu thử nghiệm**; không dùng `adb uninstall`, không chọn Clear Storage.
4. Dùng `adb install -r` cho APK release đã ký và kiểm tra dữ liệu/migration sau nâng cấp.
5. Khi đổi điện thoại hoặc cài lại app, phục hồi từ backup đã mã hóa. API key không được sao lưu và phải nhập lại.

## 5. Lệnh tham chiếu trên Windows PowerShell

```powershell
# Kiem tra thiet bi
adb devices

# Build debug
.\gradlew.bat assembleDebug

# Unit test
.\gradlew.bat testDebugUnitTest

# Test tren thiet bi/emulator
.\gradlew.bat connectedDebugAndroidTest

# Cai APK debug
adb install -r .\app\build\outputs\apk\debug\app-debug.apk

# Cai APK release da ky (kiem tra duong dan thuc te)
adb install -r .\app\build\outputs\apk\release\app-release.apk
```

## 6. Prompt giao Codex một nhiệm vụ

> Bạn là Senior Android Engineer và QA Engineer trong dự án AI Story Studio. Hãy đọc `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/TEST_PLAN.md` và file backlog này. Thực hiện **duy nhất nhiệm vụ [TASK-ID]** theo các tiêu chí nghiệm thu đã ghi. Trước khi sửa, kiểm tra code, SDK/Gradle và các phụ thuộc thực tế. Tạo test, sửa nhỏ nhất có thể, build và chạy test thực tế khi môi trường cho phép. Không thay đổi những module không liên quan; không hardcode/log API key, không dùng destructive DB migration. Comment trong code bằng tiếng Việt không dấu. Cuối nhiệm vụ báo cáo: file đã sửa, lý do sửa, lệnh test đã chạy, kết quả PASS/FAIL có bằng chứng, rủi ro tồn tại, cập nhật `docs/TASK_LOG.md`. Nếu không chạy được test, ghi rõ BLOCKED, không nhận PASS. **Không tự chuyển sang nhiệm vụ tiếp theo.**

**Nhiệm vụ đầu tiên:** `A-01`. Sau `A-03` phải thực hiện `D-01`; trước bản phát hành cá nhân cần làm `D-06`.

## 7. Tài liệu chính thức để đối chiếu khi triển khai

- Android offline-first: https://developer.android.com/topic/architecture/data-layer/offline-first
- Room migration testing: https://developer.android.com/reference/androidx/room/testing/MigrationTestHelper
- Android backup: https://developer.android.com/identity/data/autobackup
- Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files
- Android app signing: https://developer.android.com/studio/publish/app-signing
- Codex và AGENTS.md: https://developers.openai.com/cookbook/examples/gpt-5/codex_prompting_guide
