# Nhật ký quyết định kiến trúc

Các quyết định dưới đây ghi lại hướng thiết kế và trạng thái triển khai dựa trên docs/BACKLOG_V1.md. Nếu cần đổi hướng, cập nhật quyết định và bằng chứng trước khi sửa các task phụ thuộc.

## ADR-001 — Bắt đầu bằng một Gradle app module

- **Trạng thái:** chọn cho V1.0 hiện tại.
- **Quyết định:** giữ một module :app; tách trách nhiệm trong package theo core và feature.
- **Lý do:** ứng dụng cá nhân, quy mô V1.0 còn nhỏ, cần APK đơn giản và ít cấu hình build. Package boundaries cho phép kiểm thử và phân chia nghiệp vụ mà không thêm phức tạp module sớm.
- **Xem xét lại khi:** build/test cần tách độc lập, có nhiều app target, hoặc ownership và dependency graph cho thấy module hóa có lợi cụ thể.

## ADR-002 — MVVM, Use Cases và Repository

- **Trạng thái:** chọn cho thiết kế; cần áp dụng khi thêm feature.
- **Quyết định:** Compose UI chỉ xử lý state/events; ViewModel công bố StateFlow; Use Case điều phối nghiệp vụ; Repository ẩn chi tiết Room, DataStore và mạng.
- **Lý do:** tách UI khỏi I/O để test offline, thay provider và kiểm soát concurrency.

## ADR-003 — Room là nguồn chính cho nội dung

- **Trạng thái:** đã triển khai nền tảng schema V1 trong A-03.
- **Quyết định:** lưu hồ sơ, chat, messages, variants, novel, chapters, scenes, revisions, memories, summaries và timeline trong Room. DataStore dùng cho tùy chọn và metadata cấu hình.
- **Lý do:** dữ liệu quan hệ, truy vấn theo scope và transaction cần migration có kiểm soát.
- **Ràng buộc:** schema export tại `app/schemas/com.hoangtu.aistorystudio.core.database.StudioDatabase/1.json`; không fallback destructive, không xóa/reset DB khi lỗi. MigrationTestHelper kiểm tra schema V1. Migration V1→V2 thật chỉ được thêm khi có thay đổi schema thật và phải có test bảo toàn dữ liệu.

## ADR-004 — Provider AI qua abstraction và streaming

- **Trạng thái:** interface và FakeAiProvider đã triển khai ở A-04; provider mạng chưa triển khai.
- **Quyết định:** `AiProvider` nằm trong `core.ai`, nhận `AiRequest` gồm lịch sử role/content và phát `Flow<AiStreamEvent>`. Stream phát text delta, một terminal completion hoặc lỗi phân loại; cancellation của coroutine không bị đổi thành lỗi. `FakeAiProvider` hỗ trợ scenario cố định và scenario chọn theo request, không gửi network request.
- **Lý do:** ứng dụng không cần backend riêng, nhưng cần thử lỗi, cancellation và stream mà không gọi dịch vụ thật.
- **Ràng buộc:** domain không phụ thuộc OkHttp, JSON, HTTP response, provider hay model cụ thể. Endpoint và model sẽ do cấu hình ở task khác quản lý. `deepseek-v4.1-flash-uncensored` là model ban đầu người dùng đề xuất, không nhúng cứng vào code và không giả định mọi endpoint đều hỗ trợ.
- **Giới hạn A-04:** test xác nhận FakeAiProvider hoạt động offline; Chat UI và Novel Editor chưa có consumer thực tế nên chưa thể nghiệm thu hai luồng sản phẩm chạy offline.

## ADR-005 — API token được mã hóa bằng Android Keystore

- **Trạng thái:** đã triển khai nền tảng ở A-05; chưa có consumer gọi AI qua mạng.
- **Quyết định:** `AndroidKeyStoreSecureKeyStore` tạo khóa AES-256 không xuất được cho mỗi lần lưu key. Dùng `AES/GCM/NoPadding`, IV 96-bit do Cipher tạo ngẫu nhiên, tag 128-bit và provider ID làm AAD. Preferences DataStore lưu ciphertext/IV/alias và metadata provider; không lưu plaintext trong Room, source, BuildConfig, resource hoặc log.
- **Thay khóa/xóa:** lưu key mới dưới alias mới, thay ciphertext/alias trong một lần ghi DataStore, rồi xóa alias cũ. Xóa key gỡ ciphertext trước khi xóa alias. Nếu DataStore/Keystore gặp lỗi, trả mã lỗi domain không kèm exception message hoặc secret; không xóa DB truyện. Alias mồ côi có thể còn nếu transaction lưu bị hủy/lỗi hoặc xóa alias thất bại, nhưng không còn payload để dùng.
- **Keystore/ciphertext không đọc được:** giữ metadata provider và ciphertext, đánh dấu cần nhập lại; không crash và không tự xóa dữ liệu truyện. API key được loại khỏi cloud backup, device transfer và legacy Auto Backup bằng rule loại trừ file DataStore riêng.
- **Kiểm chứng A-05:** unit test DataStore/repository với FakeSecureKeyStore; instrumentation dùng Android Keystore thật cho AES-GCM, tag/AAD, xóa và dữ liệu backup. Một test ADB riêng đã seed ciphertext, force-stop rồi mở lại app và đọc đúng fixture sau khi process mới khởi động.
- **Giới hạn:** dữ liệu plaintext xuất hiện tạm trong bộ nhớ khi người dùng nhập hoặc khi consumer tương lai sử dụng; Keystore ngăn trích xuất khóa nhưng không ngăn process đã được cấp quyền sử dụng khóa. Backup archive thủ công chưa tồn tại và phải loại trừ cấu hình này khi được triển khai.
- **Tham khảo:** [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [DataStore và backup](https://developer.android.com/topic/libraries/architecture/datastore#device-backup-and-restore).

## ADR-006 — Tách dữ liệu và ngữ cảnh Roleplay/Novel

- **Trạng thái:** schema scope đã triển khai ở A-03; Context Builder vẫn chưa triển khai.
- **Quyết định:** chat, branch, memory và summary Roleplay có scope hội thoại/nhân vật; dự án, chương, cảnh, memory và summary Novel có scope project. Hồ sơ nhân vật có thể được chia sẻ bằng liên kết tường minh.
- **Lý do:** ngăn lịch sử và ký ức của hai chế độ hoặc hai dự án bị trộn.
- **Kiểm chứng:** A-03 có test hai hội thoại, hai project, summary và memory không lẫn scope. Repository xác thực memory có đúng một owner và chặn đổi owner; Context Builder và M-01..M-06 vẫn cần test riêng.

## ADR-007 — Không dùng destructive Room migration

- **Trạng thái:** đã áp dụng trong `StudioDatabase`; không có destructive fallback hoặc lệnh xóa toàn bảng.
- **Quyết định:** giữ schema export và migration tường minh có test; nếu đường nâng cấp không hỗ trợ thì dừng, báo lỗi có thể xử lý và giữ DB nguồn.
- **Lý do:** ưu tiên không mất dữ liệu khi cập nhật APK.

## ADR-008 — SAF cho import/export bản thảo và backup

- **Trạng thái:** chọn cho thiết kế; thuộc task D/N tương ứng.
- **Quyết định:** dùng Storage Access Framework để người dùng chọn tệp/vị trí. Export truyện hỗ trợ TXT/Markdown UTF-8; backup có manifest, checksum và mã hóa.
- **Lý do:** thao tác do người dùng kiểm soát, tránh xin quyền lưu trữ rộng.

## ADR-009 — Giữ applicationId và quản lý signing riêng

- **Trạng thái:** applicationId hiện tại được giữ; khóa release thuộc D-06.
- **Quyết định:** dùng com.hoangtu.aistorystudio ổn định; keystore release lưu ngoài repository và giữ cùng certificate giữa các bản.
- **Lý do:** cập nhật APK cần cùng ID/chứng chỉ và versionCode tăng để cài đè mà giữ app data.

## ADR-010 — Một NavHost với trạng thái riêng cho sáu đích chính

- **Trạng thái:** đã triển khai trong A-02.
- **Quyết định:** dùng Navigation Compose với một `NavHost`; điều hướng đích chính lưu và khôi phục back stack bằng `saveState`, `restoreState` và `launchSingleTop`. Back hệ thống tiếp tục dùng hành vi back stack của Navigation.
- **Lý do:** giữ trạng thái màn khi chuyển giữa Roleplay, Novel và các khu vực khác, đồng thời tránh tạo thêm bản sao đích chính khi người dùng nhấn lặp.
- **Hiển thị thích ứng:** dùng NavigationBar ở màn hình hẹp và NavigationRail từ 600 dp; nội dung có chiều rộng tối đa 840 dp. Theme theo cài đặt sáng/tối hệ thống và dùng màu động trên Android 12 trở lên.
- **Kiểm chứng:** instrumentation test sáu đích, Back, tạo lại Activity, nền/quay lại và hai bảng màu; smoke điện thoại thật vẫn thuộc phần còn thiếu của A-01.
- **Giới hạn:** sáu màn hình A-02 hiện là khung giao diện; ViewModel, dữ liệu và hành vi sản phẩm sẽ được triển khai trong task tương ứng.

## ADR-011 — Room/KSP và phiên bản schema đầu tiên

- **Trạng thái:** đã áp dụng trong A-03.
- **Quyết định:** Room runtime/compiler/testing 2.8.5; KSP 2.3.12; schema bắt đầu ở version 1 và xuất vào thư mục được Git quản lý. `room-ktx` không thêm riêng vì API Coroutines/Flow đã có trong Room runtime hiện tại.
- **Lý do:** các phiên bản tương thích với AGP 9.4.1, Gradle 9.6.0 và Kotlin Compose plugin 2.2.10; KSP task sinh implementation DAO thành công.
- **Tương thích MigrationTestHelper:** Room testing 2.8.5 cần kotlinx-serialization-core 1.8.1. Navigation/lifecycle hiện kéo 1.7.3; dependency runtime được căn về 1.8.1 để tránh `AbstractMethodError` khi đọc schema export.
- **Giới hạn:** hiện chưa có migration nâng version. Không tăng DB version chỉ để tạo migration giả; thay đổi schema tiếp theo phải thêm migration có tên và test V1→V2 với dữ liệu fixture.
