# Ký và cập nhật APK release

Tài liệu này áp dụng cho APK cài trực tiếp, không dùng Google Play App Signing. Người sở hữu ứng dụng tự tạo và giữ khóa release; không gửi keystore hoặc mật khẩu vào Git, log hay cuộc trò chuyện.

## Tạo keystore production một lần

Chọn một thư mục riêng tư nằm ngoài repository và kiểm tra tên tệp chưa tồn tại trước khi tạo. Ví dụ dưới đây lưu PKCS12; thay đường dẫn bằng nơi lưu do bạn kiểm soát:

```powershell
$keyDirectory = 'D:\PrivateKeys\AIStoryStudio'
$keyStore = Join-Path $keyDirectory 'aistory-release.p12'
New-Item -ItemType Directory -Path $keyDirectory -Force | Out-Null
if (Test-Path -LiteralPath $keyStore) { throw 'Keystore đã tồn tại; không ghi đè.' }
& "$env:JAVA_HOME\bin\keytool.exe" -genkeypair -v -storetype PKCS12 `
  -keystore $keyStore -alias aistory-release -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` sẽ hỏi mật khẩu và thông tin chứng chỉ tương tác. Nhập mật khẩu tại prompt; không thêm `-storepass` hoặc `-keypass` vào command line. Dùng mật khẩu mạnh, lưu tách khỏi tệp keystore. PKCS12 thường yêu cầu mật khẩu private key giống mật khẩu kho.

Android cần cùng package và chứng chỉ để chấp nhận APK như một bản cập nhật; không tạo lại khóa sau khi phát hành bản đầu. Nếu private key bị mất, APK tự quản lý sẽ không thể ký bản cập nhật tương thích với ứng dụng đã cài. Xem [hướng dẫn ký ứng dụng Android](https://developer.android.com/studio/publish/app-signing).

## Sao lưu khóa

Ngay sau khi tạo, sao chép nguyên tệp keystore sang ít nhất một vị trí thứ hai do bạn kiểm soát, ví dụ thiết bị lưu trữ ngoại tuyến được mã hóa hoặc kho lưu trữ mã hóa có quyền truy cập riêng. Không dùng repository, thư mục đồng bộ chia sẻ rộng, email hoặc chat làm bản sao.

So sánh SHA-256 của hai bản mà không mở hoặc in nội dung keystore:

```powershell
(Get-FileHash -Algorithm SHA256 -LiteralPath $keyStore).Hash
(Get-FileHash -Algorithm SHA256 -LiteralPath '<duong-dan-ban-sao>').Hash
```

Hai giá trị phải giống nhau. Giữ alias và mật khẩu trong trình quản lý mật khẩu riêng, bật xác thực đa yếu tố cho nơi lưu backup nếu có. Định kỳ xác minh có thể đọc backup bằng `keytool -list` mà không xuất private key; chỉ làm trên bản sao. Không xóa hoặc thay khóa đang dùng để sửa lỗi build.

## Cấu hình build process

Gradle nhận bốn biến môi trường process, không đọc secret từ mã nguồn hoặc `gradle.properties`:

| Biến | Nội dung |
|---|---|
| `ASTORY_RELEASE_STORE_FILE` | Đường dẫn tuyệt đối tới keystore ngoài repository. |
| `ASTORY_RELEASE_STORE_PASSWORD` | Mật khẩu keystore. |
| `ASTORY_RELEASE_KEY_ALIAS` | Alias của private key. |
| `ASTORY_RELEASE_KEY_PASSWORD` | Mật khẩu private key. |

Chạy `.\scripts\build-release.ps1`; script hỏi đường dẫn/alias và nhận hai mật khẩu dưới dạng input ẩn, đặt chúng tạm thời trong process environment, gọi Gradle Wrapper và `apksigner`, rồi khôi phục/xóa biến môi trường. Không dùng `setx` cho password. Có thể gọi wrapper trực tiếp khi một secret manager hoặc shell cục bộ đã cấp bốn biến:

```powershell
.\gradlew.bat :app:assembleRelease --no-configuration-cache --console=plain
```

`--no-configuration-cache` tránh đưa credential trong signing task vào cache build. Gradle mã hóa Configuration Cache trên đĩa theo máy, nhưng credential còn có thể nằm trong trạng thái task được lưu; vì vậy quy trình release tắt cache. Xem [Gradle Configuration Cache: handling credentials](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html).

Nếu thiếu bất kỳ biến nào, keystore không tồn tại, đường dẫn tương đối hoặc file nằm trong repository, release task dừng với lỗi cấu hình. `assembleDebug` vẫn dùng debug signing riêng do Android SDK quản lý. Không tái sử dụng debug keystore làm khóa release.

## Quy tắc version

- `applicationId` phải giữ nguyên `com.hoangtu.aistorystudio` để Android nhận cùng một ứng dụng.
- `app.versionCode` trong `gradle.properties` bắt đầu tại `1`. Trước mỗi bản phát hành mới, tăng lên số nguyên lớn hơn versionCode cao nhất từng phát hành/cài đặt. Không giảm hoặc dùng lại số cũ.
- `versionName` hiện là `1.0`; điều chỉnh theo nhãn phiên bản sản phẩm khi phát hành. `versionName` không thay thế quy tắc tăng `versionCode`.
- Override `-Papp.versionCode=<so-moi>` chỉ dùng để kiểm thử; sau kiểm thử, không commit version test thay cho version release dự kiến.

Android kiểm tra package, chữ ký và version khi cập nhật. Dùng `apksigner verify --print-certs` để xác nhận chữ ký và `aapt dump badging` để đọc package/version. `scripts/test-release-upgrade.ps1` tự thực hiện các kiểm tra đó.

## Kiểm thử V1 → V2

Tạo một AVD mới, sạch, dùng riêng cho thử nghiệm. Kiểm tra ADB serial là emulator và chưa cài `com.hoangtu.aistorystudio`, rồi chạy:

```powershell
.\scripts\test-release-upgrade.ps1 -DeviceSerial <emulator-serial>
```

Script build hai APK release bằng cùng keystore, V1 dùng giá trị `app.versionCode` hiện tại và V2 tăng tạm một đơn vị. Nó kiểm tra applicationId, versionCode, chứng chỉ V1/V2 và sự khác nhau giữa chứng chỉ debug/release, cài V1, ghi fixture riêng trong app-private cache, cập nhật bằng `adb install -r`, rồi xác minh fixture còn nguyên. Script dừng nếu ứng dụng đã có trên emulator; nó không uninstall hoặc clear data. Fixture là tệp giả, không phải dữ liệu Room và không tạo migration Room.

Nếu process chưa có đủ biến `ASTORY_RELEASE_*`, script hỏi đường dẫn/alias và mật khẩu bằng prompt ẩn tương tự `build-release.ps1`; các giá trị cũ được khôi phục/xóa khi chạy xong. Test APK instrumentation được tạo từ build type debug và ký lại bằng khóa của lần kiểm thử để có thể chạy trong process của app release; APK test này không phải artifact phát hành.

Không chạy trên điện thoại hoặc AVD có dữ liệu cá nhân. AVD hiện có thể chứa bản debug ký bằng chứng chỉ khác; xung đột chữ ký sẽ làm `install -r` bị từ chối. Không uninstall để vượt qua lỗi đó; dùng AVD thử nghiệm mới.

## Bằng chứng và giới hạn

`apksigner verify` xác nhận APK được ký; fingerprint chứng chỉ công khai có thể được ghi vào TASK_LOG. Không ghi mật khẩu, nội dung keystore hoặc private key. Kiểm thử file fixture chứng minh Android giữ app-private data qua package update; kiểm thử đầy đủ số bản ghi Room, revisions và migration thuộc D-07/D-01.

Khóa production và bản sao lưu thật phải do người dùng tạo ngoài repository trước khi phát hành bản đầu. Khóa tạm dùng trong kiểm thử không được dùng để phân phối APK.
