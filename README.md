# AI Story Studio

Ung dung Android ca nhan, phat trien bang Kotlin va Jetpack Compose. Pham vi hien tai la nen tang A-01; cac man hinh va Room chua duoc trien khai trong nhiem vu nay.

## Moi truong build

| Thanh phan | Cau hinh hien tai |
| --- | --- |
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.6.0 |
| Gradle daemon JDK | 25, duoc pin trong `gradle/gradle-daemon-jvm.properties` |
| Android SDK Platform | 37 |
| Android SDK Build Tools | 36.0.0 |
| Kotlin Compose plugin | 2.2.10 |
| Compose BOM | 2026.02.01 |
| `minSdk` / `targetSdk` | 26 / 37 |
| `applicationId` | `com.hoangtu.aistorystudio` |

Compose duoc bat trong module `app` bang `buildFeatures.compose = true`; Material 3 va Compose UI dung Compose BOM. Ma nguon Java dat `sourceCompatibility`/`targetCompatibility` 11. Gradle wrapper la nguon chuan de chay build. Android Studio can dung Gradle JDK 25 cho project nay.

Room chua duoc them vao module; phan do thuoc A-03.

## Mo project va build tren Windows

1. Cai Android Studio, Android SDK Platform 37 va Build Tools 36.0.0.
2. Mo thu muc project nay trong Android Studio. De Android Studio tao `local.properties` theo SDK tren may.
3. Chon Gradle JDK 25 trong **Settings > Build, Execution, Deployment > Build Tools > Gradle**. Co the dung JBR di kem Android Studio.
4. Trong PowerShell, tro `JAVA_HOME` den JBR 25 truoc khi chay wrapper (sua duong dan neu Android Studio duoc cai noi khac):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

APK debug duoc tao tai `app/build/outputs/apk/debug/app-debug.apk`.

## Cai va mo APK

Bat USB debugging, ket noi thiet bi, sau do chay:

```powershell
adb devices -l
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.hoangtu.aistorystudio -c android.intent.category.LAUNCHER 1
```

## Loi NIO loopback trong shell Codex tren Windows

Neu Gradle bao `Unable to establish loopback connection`, shell co the dang truyen duong dan TEMP dai/ao vao JVM. Tao thu muc tam ngan va chay Gradle trong cung cua so PowerShell:

```powershell
New-Item -ItemType Directory -Path 'C:\jtmp' -Force | Out-Null
$env:TEMP = 'C:\jtmp'
$env:TMP = 'C:\jtmp'
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
```

Neu Android Studio duoc cai o duong dan khac, thay `JAVA_HOME` bang JBR tuong ung. Cai dat nay chi ap dung cho cua so PowerShell hien tai.

## Bao mat va Git

Khong commit `local.properties`, API key, `key.properties`, `secrets.properties`, `.env`, file `.jks` hoac `.keystore`. Khong dua khoa API vao ma nguon hay log. Ban debug dung debug keystore do Android tao tren may; cau hinh ky release thuoc D-06.

Quy tac lam viec theo tung backlog ID nam trong `AGENTS.md`; bang chung build va test duoc ghi trong `docs/TASK_LOG.md`.
