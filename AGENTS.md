# Quy tắc làm việc cho AI Story Studio

## Trước khi thay đổi

- Đọc toàn bộ docs/BACKLOG_V1.md và xem docs/TASK_LOG.md để hiểu phạm vi, phụ thuộc và trạng thái thực tế.
- Mỗi lần chỉ thực hiện một mã nhiệm vụ. Nếu người dùng chỉ định phạm vi tài liệu rộng hơn một mã, hoàn thành phần tài liệu được yêu cầu mà không triển khai tính năng thuộc mã khác.
- Kiểm tra cấu hình, mã nguồn và dữ liệu hiện có trước khi sửa. Ưu tiên thay đổi nhỏ, giải quyết nguyên nhân gốc và giữ lại dữ liệu người dùng.
- Không xóa, ghi đè hoặc đặt lại dữ liệu để làm build hay test chạy được.

## Bảo mật và mã nguồn

- Không đưa API key, thông tin đăng nhập hoặc khóa ký vào mã nguồn, log, repository hay báo cáo lỗi. Không commit local.properties, tệp .jks/.keystore hoặc signing secrets.
- Không ghi nội dung truyện, hội thoại riêng tư hay thông tin nhạy cảm vào log và báo cáo kiểm thử.
- Mọi comment mới trong mã nguồn phải viết bằng tiếng Việt không dấu. Ưu tiên tên hàm và tên biến rõ nghĩa thay cho comment không cần thiết.
- Không thêm Room, navigation, provider AI, lưu trữ bí mật hoặc backup ngoài mã nhiệm vụ đang làm.

## Build, kiểm thử và bằng chứng

- Dùng Gradle Wrapper của dự án. Trước khi chạy lệnh, kiểm tra JDK, Android SDK và trạng thái thiết bị/emulator có liên quan.
- Chỉ ghi PASS khi lệnh đã chạy xong và có kết quả quan sát được. Nếu môi trường không cho phép chạy, ghi BLOCKED cùng nguyên nhân và bằng chứng; lỗi thực thi ghi FAIL.
- Phân biệt test mẫu hiện có với kiểm thử tính năng. Không suy diễn kết quả của test chưa chạy.
- Cập nhật docs/TASK_LOG.md với mã nhiệm vụ, file tạo/sửa, lệnh, kết quả, đường dẫn báo cáo/artifact và điều kiện nghiệm thu còn thiếu.
- Tạo checkpoint Git cục bộ sau khi hoàn tất phần việc. Không push, merge, phát hành hoặc gửi dữ liệu ra ngoài nếu chưa được yêu cầu.

### Chạy test trên Windows

- Ưu tiên `scripts/test-unit.ps1` cho unit test và `scripts/test-all.ps1 -DeviceSerial <emulator-serial>` cho hồi quy đầy đủ. Các script xác minh JDK/SDK/ADB và từ chối kết quả không có test, test lỗi hoặc test bị bỏ qua.
- Instrumentation mặc định phải chạy qua `scripts/test-instrumentation.ps1` trên emulator được chọn tường minh; script cài APK bằng `adb -s ... install -r` và giữ app data. Không dùng `connectedDebugAndroidTest` như runner mặc định vì AGP có thể gỡ app và test APK sau khi chạy.
- Chỉ chọn `-UseGradleRunner` trên AVD có thể cài lại và không cần giữ app data; kiểm tra log/report cùng số lượng test, không dựa riêng vào `BUILD SUCCESSFUL`. Dùng `scripts/test-harness.ps1` để xác nhận guard số test trước khi sửa runner.
- Build release qua `scripts/build-release.ps1` hoặc các biến môi trường trong process hiện tại; không đặt mật khẩu trong tham số lệnh, source, `gradle.properties` hay log. Release task phải chạy với `--no-configuration-cache`.
- Kiểm thử cài đè release chỉ dùng emulator thử nghiệm sạch qua `scripts/test-release-upgrade.ps1`; nếu thiếu biến ký, script hỏi mật khẩu qua prompt ẩn. Script từ chối nếu ứng dụng đã cài và không gọi uninstall/clear data.

## Quy trình Git sau nhiệm vụ

Khi người dùng yêu cầu đồng bộ GitHub, thực hiện tuần tự và ghi bằng chứng theo `docs/GIT_WORKFLOW.md`:

1. Hoàn thành đúng phạm vi nhiệm vụ được giao.
2. Chạy build và các kiểm thử phù hợp; ghi rõ lệnh và kết quả thực tế.
3. Cập nhật `docs/TASK_LOG.md`, gồm file thay đổi, kết quả và điều kiện còn thiếu.
4. Tạo commit Git cục bộ sau khi kiểm tra diff.
5. Kiểm tra file được theo dõi và lịch sử chưa đồng bộ để phát hiện khóa, dữ liệu riêng tư, artifact build hoặc cache.
6. Chỉ push khi người dùng đã yêu cầu đồng bộ; không force push.
7. Sau push, fetch/đọc ref remote và xác minh commit, nhánh, lịch sử và artifact cần thiết đã có trên GitHub.

Không xem việc tạo commit cục bộ là bằng chứng đã đồng bộ remote. Nếu xác thực bị yêu cầu hoặc remote có commit mới, dừng để người dùng xác thực hoặc kiểm tra lại lịch sử mà không viết lại lịch sử.

## Kiến trúc và dữ liệu

- Dùng docs/ARCHITECTURE.md và docs/DECISIONS.md làm thiết kế mục tiêu; chúng không có nghĩa là các phần đó đã được triển khai.
- Giữ dự án ở một Gradle app module cho đến khi có lý do đo được để tách module; tổ chức mã theo feature và phụ thuộc theo hướng UI → ViewModel → Use Case → Repository.
- Room là nguồn dữ liệu chính cho nội dung người dùng; mọi thay đổi schema cần schema export, migration có tên và migration test. Không dùng destructive migration fallback.
- Tách ngữ cảnh Roleplay và Novel theo định danh phạm vi. Chỉ dùng hồ sơ nhân vật chung khi hành động đó được yêu cầu rõ ràng.
- Giữ cấu hình API do người dùng chỉnh sửa được. Khóa API phải được mã hóa bằng khóa Android Keystore; không lưu khóa dạng rõ.

## Hoàn tất nhiệm vụ

- Đối chiếu kết quả với tiêu chí nghiệm thu trong backlog. Nếu còn điều kiện chưa kiểm tra, ghi trạng thái một phần hoặc BLOCKED và nêu chính xác việc còn thiếu.
- Không bắt đầu mã nhiệm vụ kế tiếp trong cùng lượt nếu người dùng chỉ giao một mã.
