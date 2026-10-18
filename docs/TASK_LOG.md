# Nhat ky cong viec

## A-01 — 18/10/2026

### Pham vi va phat hien

- Kiem tra toan bo `docs/BACKLOG_V1.md`; chi thuc hien A-01.
- Project ban dau chua duoc khoi tao Git, chua co README va AGENTS. `local.properties` da co va tro den Android SDK tren may.
- Android SDK co Platform 37, Build Tools 36.0.0, Platform Tools/ADB 37.0.1 va system image/emulator API 37.
- Gradle Wrapper la 9.6.0; AGP la 9.4.1. Theo bang tuong thich AGP 9.4 can Gradle 9.6, ho tro toi API 37, Build Tools 36.0.0 va JDK toi thieu/mac dinh 17. Gradle 9.6 chay daemon voi JDK 25 theo `gradle/gradle-daemon-jvm.properties`; Android Studio JBR tren may la JDK 25. Java nguon dat muc 11. Java 27 dang la JDK mac dinh cua PATH tren may, nhung Gradle 9.6 chua cong bo ho tro chay tren Java 27; README huong dan dung JBR 25 cho Android Studio va command line.
- Compose plugin 2.2.10, Compose BOM 2026.02.01 va Material 3 da duoc cau hinh; `buildFeatures.compose` dang bat. Build xac nhan cau hinh plugin va Compose compile duoc.
- `minSdk` 26, `targetSdk` 37, `applicationId` `com.hoangtu.aistorystudio`. Room chua co; day la A-03 va khong duoc them trong A-01.
- JDK NIO Selector va Gradle daemon ban dau gap `Unable to establish loopback connection` trong moi truong shell Codex tren Windows. Kiem tra Selector truc tiep tai JDK 17 va 25 cung loi; dat `TEMP`/`TMP` cua tien trinh ve `C:\jtmp` thi Selector mo duoc va Gradle build chay duoc. Day la workaround moi truong, khong can doi source project.

### File tao/sua

- Tao `AGENTS.md`: quy tac pham vi backlog, bao mat, comment code, build/test va Git checkpoint.
- Tao `README.md`: stack/version hien tai, cach mo project, build/test, cai APK va workaround NIO tren shell Codex Windows.
- Tao `docs/TASK_LOG.md`: ket qua va bang chung A-01.
- Cap nhat `.gitignore`: bo qua Android Studio local state, local.properties, build output, signing keys, secrets va APK/AAB.
- Khoi tao Git va tao checkpoint cuc bo sau khi hoan tat thay doi A-01.

### Lenh va ket qua

Tat ca lenh Gradle duoi day chay trong PowerShell voi `TEMP=C:\jtmp`, `TMP=C:\jtmp` va `JAVA_HOME` tro den JBR Android Studio JDK 25.

| Lenh | Ket qua |
| --- | --- |
| `gradlew.bat assembleDebug` | **PASS** — `BUILD SUCCESSFUL`, 36 actionable tasks; APK tao tai `app/build/outputs/apk/debug/app-debug.apk` (12,052,400 bytes). |
| `gradlew.bat testDebugUnitTest` | **PASS** — 1 test, 0 failure, 0 error; XML: `app/build/test-results/testDebugUnitTest/TEST-com.hoangtu.aistorystudio.ExampleUnitTest.xml`. Day la unit test mau `2 + 2`. |
| `gradlew.bat connectedDebugAndroidTest` | **PASS** — 1 instrumented test, 0 failure, 0 error tren emulator API 37 (`emulator-5554`); XML: `app/build/outputs/androidTest-results/connected/debug/TEST-com.hoangtu.aistorystudio.ExampleInstrumentedTest.xml`. Test mau xac nhan package name. |
| `gradlew.bat lintDebug` | **PASS** — `BUILD SUCCESSFUL`, 0 errors, 10 warnings; report: `app/build/reports/lint-results-debug.html`. Warning gom label Manifest trung lap, goi y co phien ban dependency moi va cac mau mau khong dung. |
| `adb install -r app/build/outputs/apk/debug/app-debug.apk` tren `emulator-5554` | **PASS** — ADB tra `Success`. `adb shell getprop ro.build.version.sdk` tra `37`. |
| `adb shell am start -W -n com.hoangtu.aistorystudio/.MainActivity` | **PASS** — `Status: ok`; Activity `MainActivity` o trang thai `RESUMED` va co window foreground trong `dumpsys activity`. Logcat khong co `FATAL EXCEPTION` cho package. |
| Kiem tra thiet bi that bang `adb devices -l` | **BLOCKED** — thiet bi `JBZTBABQLBI7BYVG` chuyen sang `offline`; khong doc duoc API level, khong cai/mo APK tren dien thoai that. |

### Rui ro va tieu chi nghiem thu

- Build, unit test, instrumented test tren emulator va mo APK tren emulator deu da xac minh thuc te.
- Dieu kien A-01 **chua dat day du**: backlog yeu cau mo APK tren dien thoai that, nhung ADB hien khong co dien thoai online. Can ket noi lai dien thoai va cai/mo APK de dong A-01.
- Chi co bo test mau mac dinh; chua co test tinh nang. Khong chay tren API 31/33 trong lan nay; system image san co la API 37.
- Build co canh bao thu vien `libandroidx.graphics.path.so` khong strip duoc nen duoc dong goi nguyen; khong lam build that bai. Instrumentation co canh bao deprecated API tu protobuf; test van PASS.
- Khong tao Room, navigation hay man hinh san pham; cac phan nay nam ngoai A-01. Khong co API key hoac release keystore duoc them vao repository.
