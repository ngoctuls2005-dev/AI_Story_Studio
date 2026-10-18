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

## Kiến trúc và dữ liệu

- Dùng docs/ARCHITECTURE.md và docs/DECISIONS.md làm thiết kế mục tiêu; chúng không có nghĩa là các phần đó đã được triển khai.
- Giữ dự án ở một Gradle app module cho đến khi có lý do đo được để tách module; tổ chức mã theo feature và phụ thuộc theo hướng UI → ViewModel → Use Case → Repository.
- Room là nguồn dữ liệu chính cho nội dung người dùng; mọi thay đổi schema cần schema export, migration có tên và migration test. Không dùng destructive migration fallback.
- Tách ngữ cảnh Roleplay và Novel theo định danh phạm vi. Chỉ dùng hồ sơ nhân vật chung khi hành động đó được yêu cầu rõ ràng.
- Giữ cấu hình API do người dùng chỉnh sửa được. Khóa API phải được mã hóa bằng khóa Android Keystore; không lưu khóa dạng rõ.

## Hoàn tất nhiệm vụ

- Đối chiếu kết quả với tiêu chí nghiệm thu trong backlog. Nếu còn điều kiện chưa kiểm tra, ghi trạng thái một phần hoặc BLOCKED và nêu chính xác việc còn thiếu.
- Không bắt đầu mã nhiệm vụ kế tiếp trong cùng lượt nếu người dùng chỉ giao một mã.
