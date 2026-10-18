# Quy trình Git an toàn

Tài liệu này áp dụng khi người dùng yêu cầu đưa thay đổi của AI Story Studio lên GitHub. Không tự push trong các nhiệm vụ chỉ yêu cầu thay đổi cục bộ.

## Trình tự

1. Xác nhận thư mục dự án, nhánh, working tree, commit gần nhất và remote bằng `git status`, `git branch -vv`, `git log` và `git remote -v`.
2. Xác minh URL remote trước khi thêm hoặc thay đổi remote; fetch trước khi hợp nhất. Kiểm tra quan hệ ancestor và nội dung remote. Nếu lịch sử độc lập, hợp nhất bảo toàn cả hai bằng `--allow-unrelated-histories`; không reset, rebase viết lại lịch sử hoặc force push nếu chưa được chấp thuận.
3. Đọc diff, build và chạy test phù hợp với nhiệm vụ. Cập nhật `docs/TASK_LOG.md` với lệnh, kết quả quan sát được, report/artifact và giới hạn kiểm thử.
4. Tạo commit cục bộ sau khi xem diff và `git diff --check`.
5. Trước push, kiểm tra toàn bộ đường dẫn được theo dõi và commit chưa có trên remote. Không đồng bộ API key, token, mật khẩu, khóa ký, `.env`, `local.properties`, database người dùng, nội dung hội thoại riêng tư, APK, build output hoặc cache. Không in giá trị nghi là bí mật vào terminal hay báo cáo. Giữ `app/schemas/` và tài liệu kỹ thuật cần quản lý phiên bản.
6. Chỉ push khi người dùng yêu cầu rõ. Push đúng nhánh đã thỏa thuận; không force. Nếu remote từ chối vì đã thay đổi, fetch và đánh giá quan hệ mới trước khi làm tiếp. Nếu cần xác thực tương tác, dừng để người dùng hoàn tất.
7. Sau push, kiểm tra `git ls-remote` hoặc fetch lại ref, xác nhận remote SHA bằng commit mong muốn, và kiểm tra lịch sử/artifact quan trọng bằng GitHub hoặc ref đã fetch. Chỉ báo đồng bộ thành công sau khi quan sát được xác nhận remote.

## Bằng chứng cần ghi

- URL remote đã xác minh và nhánh đích.
- SHA commit trước/sau, kết quả merge nếu có và trạng thái working tree.
- Kết quả build, unit test, lint và instrumentation test đã chạy; không suy diễn test chưa chạy.
- Phạm vi kiểm tra bảo mật, phát hiện và cách xử lý mà không tiết lộ giá trị nhạy cảm.
- Kết quả xác minh ref remote, Room schema và tài liệu sau push.
