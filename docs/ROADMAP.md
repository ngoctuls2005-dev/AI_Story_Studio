# Lộ trình AI Story Studio V1.0

**Nguồn thứ tự và tiêu chí:** docs/BACKLOG_V1.md. Mọi mã ngoài A-01 đang ở TODO; tài liệu này không đánh dấu những phần chưa làm là hoàn tất.

## Trạng thái hiện tại

| Mục | Trạng thái | Bằng chứng hoặc việc còn thiếu |
|---|---|---|
| A-01 — nền tảng dự án | Đạt một phần | Wrapper build được; unit test và instrumentation mẫu đã chạy trên emulator API 37. Cần mở APK trên điện thoại thật để đạt đầy đủ tiêu chí backlog. |
| Tài liệu kiến trúc | Tạo trong phạm vi yêu cầu A-01 | ARCHITECTURE.md, ROADMAP.md, TEST_PLAN.md, DECISIONS.md, AGENTS.md; tài liệu mô tả mục tiêu, không khẳng định đã triển khai tính năng. |
| A-02 trở đi | TODO | Chưa có Navigation hay màn hình ứng dụng; chưa bắt đầu implementation. |

## Trình tự đề xuất

1. **Đóng A-01:** kết nối điện thoại Android thật, cài debug APK bằng adb install -r, mở MainActivity, ghi model/API level và kết quả trong docs/TASK_LOG.md.
2. **A-02 — khung giao diện và điều hướng:** tạo Material 3, hỗ trợ Dark Mode, Navigation và sáu màn Home, Roleplay, Novel, Characters, Library, Settings. Kiểm tra chuyển màn và khôi phục trạng thái.
3. **A-03 — nền dữ liệu Room:** chốt schema V1, khóa ngoại/index, repositories và transaction; thêm CRUD, export schema và kiểm thử migration theo D-01 ngay sau đó.
4. **A-04 — nhà cung cấp AI giả lập:** tạo AiProvider và stream giả lập offline để kiểm tra luồng giao diện, lỗi, hủy.
5. **A-05 — cấu hình API an toàn:** endpoint/model có thể sửa; token mã hóa bằng Android Keystore; kiểm tra, dùng lại sau khởi động và xóa token.
6. **A-06 — hoàn thiện quy trình test:** cấu hình test runner, Compose tests và MockWebServer còn thiếu; các tài liệu kiến trúc đã được tạo sớm theo yêu cầu riêng của A-01.
7. **D-06 trước release đầu:** chốt khóa ký release bên ngoài repository và quy trình tăng versionCode.

## Các cổng sau nền tảng

- **G0:** A-01 đến A-06 và D-01/D-06 đạt trước khi coi nền tảng sẵn sàng.
- **G1:** CRUD nhân vật/chat → provider và SSE → chat streaming, lưu partial, lỗi mạng, variants.
- **G2:** memory có scope → duyệt và truy xuất → Context Builder và benchmark tính nhất quán.
- **G3:** dự án/editor/autosave/revisions → thao tác AI → export Markdown/TXT và stress test.
- **G4:** backup có kiểm tra/mã hóa/phục hồi, Android Auto Backup rules và cập nhật APK không mất dữ liệu.
- **G5:** test hồi quy, thiết bị Android 12/13, audit bảo mật/hiệu năng và đóng gói release.

Tiêu chí chi tiết, phụ thuộc và các test T-01..T-14 nằm trong docs/BACKLOG_V1.md; không thay đổi trạng thái backlog khi chưa có bằng chứng thực tế.
