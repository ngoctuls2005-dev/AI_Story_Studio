# Lộ trình AI Story Studio V1.0

**Nguồn thứ tự và tiêu chí:** docs/BACKLOG_V1.md. Trạng thái dưới đây chỉ được cập nhật khi có bằng chứng chạy được; việc còn thiếu được ghi riêng.

## Trạng thái hiện tại

| Mục | Trạng thái | Bằng chứng hoặc việc còn thiếu |
|---|---|---|
| A-01 — nền tảng dự án | Đạt một phần | Build và test mẫu chạy trên emulator. Điện thoại Xiaomi API 33 được ADB nhận diện nhưng từ chối cài APK (`INSTALL_FAILED_USER_RESTRICTED`); smoke thật còn thiếu. |
| Tài liệu kiến trúc | Đã tạo/cập nhật | ARCHITECTURE.md, ROADMAP.md, TEST_PLAN.md, DECISIONS.md, AGENTS.md; phân biệt rõ hiện trạng với mục tiêu. |
| A-02 — giao diện và điều hướng | Hoàn thành trên emulator | Sáu màn hình điều hướng, Back, khôi phục Activity/nền-quay lại và theme sáng/tối đã được instrumentation test; smoke thủ công và viewport rộng chạy trên emulator API 37. Chưa kiểm thử trên điện thoại thật. |
| A-03 — Room và migration an toàn | Hoàn thành trên emulator API 37 | Schema V1, 14 Entity, bốn DAO, bốn repository, FK/scope/transaction; build, unit, lint và 14 instrumentation tests đã chạy. Chưa có migration V1→V2 vì chưa có schema V2. |
| D-01 — MigrationTestHelper từ V1 sang V2 | Chờ schema V2 thật | A-03 đã export/validate V1 và ghi quy trình; không tạo version/migration giả. Cần fixture có dữ liệu khi một thay đổi schema thật tạo ra V2. |
| A-04 — AiProvider/FakeAiProvider | Hoàn thành provider-level | AiProvider và FakeAiProvider có stream/lỗi offline; unit test A-04 chạy trong suite. Chat UI và Novel Editor chưa có consumer, không tuyên bố hai luồng sản phẩm offline đã hoạt động. |
| A-05 — cấu hình provider và API key | Hoàn thành trên AVD API 37 | Settings/DataStore/Android Keystore AES-256-GCM; unit test, UI/Keystore/backup instrumentation và process restart test đạt. Tài liệu/bằng chứng chi tiết trong TASK_LOG.md. Không có kết nối API thật. |
| A-06 — test harness | Hoàn thành trên AVD API 37 | MockWebServer loopback, fixture chung, script build/unit/lint/instrumentation có kiểm tra số test; unit offline 27/27, regression instrumentation hiện tại 19/19 (gồm fixture D-06 mới), lint 0 lỗi. Gradle runner được chẩn đoán; runner trực tiếp là mặc định để giữ app data. Chi tiết và giới hạn trong TASK_LOG.md. |
| D-06 — release signing và cập nhật APK | Hạ tầng đã xác minh; khóa production còn thiếu | Release V1→V2 đạt trên AVD API 37 bằng keystore dùng một lần: cùng applicationId/chứng chỉ, versionCode 1→2, `adb install -r` giữ fixture. Khóa production và bản sao lưu ở vị trí thứ hai phải do người dùng tự tạo trước release đầu. R8 release tạm tắt sau khi APK tối ưu crash trên AVD; cần kiểm thử riêng trước khi bật lại. |

## Trình tự đề xuất

1. **Đóng A-01:** xử lý quyền cài qua USB trên điện thoại Xiaomi API 33, cài debug APK, mở MainActivity và ghi smoke test. ADB đã nhận thiết bị nhưng Android từ chối cài APK.
2. **D-01 khi có thay đổi schema thật:** kiểm thử fixture dữ liệu V1 nâng lên V2 bằng MigrationTestHelper; không tạo migration giả chỉ để đóng task.
3. **D-06 trước release đầu:** người dùng tự tạo khóa production ngoài repository, sao lưu ở vị trí thứ hai và xác nhận có thể truy cập khóa/alias trong môi trường build.
4. Sau nền tảng G0 mới triển khai C-01/C-02 rồi C-03/C-04/C-05 theo backlog; A-05 chưa tạo kết nối API thực tế.

## Các cổng sau nền tảng

- **G0:** A-01 đến A-06 và D-01/D-06 đạt trước khi coi nền tảng sẵn sàng.
- **G1:** CRUD nhân vật/chat → provider và SSE → chat streaming, lưu partial, lỗi mạng, variants.
- **G2:** memory có scope → duyệt và truy xuất → Context Builder và benchmark tính nhất quán.
- **G3:** dự án/editor/autosave/revisions → thao tác AI → export Markdown/TXT và stress test.
- **G4:** backup có kiểm tra/mã hóa/phục hồi, Android Auto Backup rules và cập nhật APK không mất dữ liệu.
- **G5:** test hồi quy, thiết bị Android 12/13, audit bảo mật/hiệu năng và đóng gói release.

Tiêu chí chi tiết, phụ thuộc và các test T-01..T-14 nằm trong docs/BACKLOG_V1.md; không thay đổi trạng thái backlog khi chưa có bằng chứng thực tế.
