# Nhật ký quyết định kiến trúc

Các quyết định dưới đây ghi lại hướng thiết kế dựa trên phạm vi người dùng và docs/BACKLOG_V1.md. Chúng chưa khẳng định tính năng tương ứng đã được triển khai. Nếu cần đổi hướng, cập nhật quyết định và bằng chứng trước khi sửa các task phụ thuộc.

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

- **Trạng thái:** chọn; chưa triển khai schema.
- **Quyết định:** lưu hồ sơ, chat, messages, variants, novel, chapters, scenes, revisions, memories, summaries và timeline trong Room. DataStore dùng cho tùy chọn và metadata cấu hình.
- **Lý do:** dữ liệu quan hệ, truy vấn theo scope và transaction cần migration có kiểm soát.
- **Ràng buộc:** export schema, migration test; không xóa schema khi migration lỗi. D-01 thực hiện ngay sau A-03.

## ADR-004 — Provider AI qua abstraction và streaming

- **Trạng thái:** chọn cho thiết kế; chưa có provider.
- **Quyết định:** giao diện AiProvider tách use case khỏi HTTP; FakeAiProvider cho test offline; provider mạng dùng OkHttp và SSE.
- **Lý do:** ứng dụng không cần backend riêng, nhưng cần thử lỗi, cancellation và stream mà không gọi dịch vụ thật.
- **Ràng buộc:** endpoint và model chỉnh sửa được. deepseek-v4.1-flash-uncensored là model ban đầu người dùng đề xuất, không nhúng cứng vào code và không giả định mọi endpoint đều hỗ trợ.

## ADR-005 — API token được mã hóa bằng Android Keystore

- **Trạng thái:** chọn cho thiết kế; chưa triển khai.
- **Quyết định:** sinh khóa AES-GCM không xuất được trong Android Keystore; lưu ciphertext và metadata tối thiểu; chỉ giải mã trong bộ nhớ khi gửi request.
- **Lý do:** không lưu token dạng rõ trong Room, source, log hay APK.
- **Hệ quả:** xử lý riêng việc người dùng xóa dữ liệu/đổi thiết bị/Keystore invalidation; token cần nhập lại khi không thể giải mã. API token bị loại khỏi backup.

## ADR-006 — Tách dữ liệu và ngữ cảnh Roleplay/Novel

- **Trạng thái:** chọn cho thiết kế; schema cần cụ thể hóa ở A-03.
- **Quyết định:** chat, branch, memory và summary Roleplay có scope hội thoại/nhân vật; dự án, chương, cảnh, memory và summary Novel có scope project. Hồ sơ nhân vật có thể được chia sẻ bằng liên kết tường minh.
- **Lý do:** ngăn lịch sử và ký ức của hai chế độ hoặc hai dự án bị trộn.
- **Kiểm chứng:** A-03 và M-01..M-06 cần test scope âm tính, không chỉ xác nhận truy vấn có dữ liệu.

## ADR-007 — Không dùng destructive Room migration

- **Trạng thái:** bắt buộc theo backlog.
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
