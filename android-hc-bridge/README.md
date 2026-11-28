# 하루+ Health Connect 브리지 (android-hc-bridge)

Health Connect에 쌓인 **오늘 걸음수 · 최근 수면 · 최근 혈압**을 읽어서, 루틴 앱 **하루+**
(https://lukekim777.github.io/luke-routine-app/) 의 건강 기록 칸에 그대로 채워 넣는 미니 안드로이드 앱입니다.

앱에서 **「하루+ 웹 앱으로 전송」** 버튼을 누르면 브라우저로 아래 주소가 열리고,
하루+ 가 값을 받아 오늘 기록에 저장합니다. (값이 없는 항목은 주소에서 빠집니다.)

```
https://lukekim777.github.io/luke-routine-app/?hc_steps=8432&hc_sleep=7.5&hc_sbp=128&hc_dbp=82&hc_date=2026-11-28
```

| 파라미터 | 뜻 | 범위 |
|---|---|---|
| `hc_steps` | 오늘 0시부터 지금까지 걸음수 합계 | 1 ~ 200,000 |
| `hc_sleep` | 최근 24시간 안에 끝난 수면 시간(시간, 소수 1자리) | 0 ~ 24 |
| `hc_sbp` / `hc_dbp` | 가장 최근 혈압 수축기 / 이완기 (mmHg) | 50~260 / 30~180 |
| `hc_date` | 기록 날짜 (`YYYY-MM-DD`, 없으면 오늘) | — |

---

## 1. APK 만들기 (GitHub Actions, 폰 없이 3분)

GitHub 연동 토큰으로는 `.github/workflows/` 아래 파일을 푸시할 수 없어서, 워크플로 파일만 웹에서 한 번 붙여 넣으면 됩니다.

1. 이 폴더의 **`WORKFLOW_TEMPLATE.yml`** 파일 내용을 전부 복사합니다.
2. GitHub 저장소 `lukekim777/luke-routine-app` → **Add file ▸ Create new file** 클릭
3. 파일 이름에 `.github/workflows/build-hc-apk.yml` 을 입력하고 (슬래시를 치면 폴더가 자동으로 만들어집니다) 복사한 내용을 붙여 넣습니다.
4. **Commit changes** 로 저장하면 빌드가 자동으로 시작됩니다.
   (자동으로 시작하지 않으면 **Actions ▸ APK 빌드 (Health Connect 브리지) ▸ Run workflow**)
5. 초록 체크가 뜨면 그 실행 화면 아래 **Artifacts ▸ app-debug** 를 내려받고 압축을 풀면 **`app-debug.apk`** 가 나옵니다.

빌드가 하는 일:

- JDK 17 (temurin) 준비 + Android SDK 라이선스 동의 (platform 36 · build-tools 는 빌드 중 자동 다운로드)
- Gradle 8.11.1 로 `:app:testDebugUnitTest`(URL 생성 단위 테스트 5개) 와 `:app:assembleDebug` 실행
- `android-hc-bridge/app/build/outputs/apk/debug/app-debug.apk` 를 아티팩트로 업로드 (30일 보관)

## 2. 폰에 설치

1. 내려받은 `app-debug.apk` 를 폰으로 옮깁니다(카카오톡 나에게 보내기, USB, 드라이브 등).
2. 파일을 눌러 설치합니다. “출처를 알 수 없는 앱” 경고가 뜨면 **설정 ▸ 허용**을 눌러 진행합니다.
3. 홈 화면에 **하루+ HC 브리지** 아이콘이 생깁니다.

## 3. Health Connect 권한 켜기

1. 앱을 엽니다. **「Health Connect 권한 허용」** 버튼을 누릅니다.
2. Health Connect 권한 화면에서 **걸음수 · 수면 · 혈압** 세 항목을 **모두 허용**한 뒤 **완료/허용**을 누릅니다.
3. 앱으로 돌아오면 오늘 값이 표시됩니다.

Health Connect 위치:

- **Android 14 이상**: 설정 ▸ 보안 및 개인정보 ▸ Health Connect (기기마다 “Health Connect” 또는 “건강 데이터”)
- **Android 13 이하**: Play 스토어의 **Health Connect** 앱 설치 필요 (앱이 안내 창으로 연결해 줍니다)
- **삼성 갤럭시**: Samsung Health ▸ 설정 ▸ Health Connect 에서 연결하면 걸음수·수면이 Health Connect로 넘어옵니다.

## 4. 하루+ 로 넘기기

1. 값이 표시된 상태에서 **「하루+ 웹 앱으로 전송」** 을 누릅니다.
2. 브라우저에 하루+ 가 열리고 “Health Connect에서 건강 기록을 불러왔어요!” 토스트가 뜨면 저장 완료입니다.
3. 하루+ 를 홈 화면에 추가해 두면 다음부터는 앱 → 전송 → 확인 두 번의 동작으로 끝납니다. 같은 방식(홈 화면 아이콘 또는 브라우저)으로 열어야 기록이 이어집니다. 브라우저마다 저장 공간이 따로이기 때문입니다.

### 값이 “기록 없음”으로 나올 때

- **걸음수**: 오늘 0시 이후 기록이 있어야 합니다. Samsung Health → Health Connect 동기화를 확인하세요.
- **수면**: 최근 24시간 안에 **끝난** 수면 세션이 있어야 합니다. 워치를 차고 잔 날 다음에 값이 나옵니다.
- **혈압**: 직접 재서 기록한 값이 있어야 합니다(최근 1년). 삼성 헬스 혈압은 혈압계·갤럭시 워치 측정 기록이 있어야 합니다.
- 화면을 아래로 당기거나 **새로고침** 을 누르면 다시 읽습니다.

## 5. 개인정보

- 읽은 값은 **앱 안에 저장하지 않습니다.** 메모리에만 두고 화면에 보여 줍니다.
- 서버가 없습니다. 값은 **전송 버튼을 눌렀을 때 하루+ 주소의 파라미터로만** 넘어갑니다.
- 하루+ 는 받은 값을 그 브라우저의 `localStorage` 에만 저장합니다(하루+ 자체 설명과 동일).
- 권한은 언제든 Health Connect 설정에서 끌 수 있습니다.

## 6. 검증된 버전 (2026-11-28 기준 실제 저장소 확인)

| 항목 | 버전 | 확인 근거 |
|---|---|---|
| Android Gradle Plugin | 8.9.3 | `dl.google.com/.../com/android/tools/build/gradle/maven-metadata.xml` 에 존재 |
| Kotlin (`org.jetbrains.kotlin.android`) | 2.2.21 | Maven Central 플러그인 마커 메타데이터에 존재 |
| Gradle | 8.11.1 | wrapper + CI 공통 (`distributionSha256Sum` 고정) |
| `androidx.health.connect:connect-client` | 1.1.0 | Google Maven 최신 정식 버전. AAR 메타데이터가 **minCompileSdk=36, minAGP=8.9.1** 을 요구 |
| `androidx.activity:activity-ktx` | 1.9.3 | Google Maven 존재 |
| `androidx.core:core-ktx` | 1.13.1 | Google Maven 존재 |
| `junit:junit` | 4.13.2 | Maven Central |
| compileSdk / targetSdk / minSdk | 36 / 35 / 26 | connect-client 1.1.0 요구(36) + 하루+ 사용 기기 범위 |
| JDK | 17 | AGP 8.9 요구 사항 |

> 참고: 처음에는 `compileSdk 35` + AGP 8.7.3 + Gradle 8.9 로 잡았지만, `connect-client 1.1.0` 의 AAR 메타데이터가
> `minCompileSdk=36`, `minAndroidGradlePluginVersion=8.9.1` 을 선언하고 있어 그대로는 빌드가 거부됩니다.
> 그래서 compileSdk 36 · AGP 8.9.3 · Gradle 8.11.1 로 올렸습니다. `targetSdk` 는 35 그대로입니다.
> (1.0.0 은 Google Maven 에서 파일이 더 이상 내려받히지 않아 선택할 수 없었습니다.)

## 7. 로컬에서 빌드하기

```bash
cd android-hc-bridge
echo "sdk.dir=/path/to/Android/sdk" > local.properties   # 또는 ANDROID_HOME 환경변수
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

결과물: `app/build/outputs/apk/debug/app-debug.apk`

## 8. 파일 구성

```
android-hc-bridge/
├─ settings.gradle.kts / build.gradle.kts / gradle.properties
├─ gradlew / gradlew.bat / gradle/wrapper/…          # Gradle 8.11.1 래퍼
├─ WORKFLOW_TEMPLATE.yml                              # .github/workflows/build-hc-apk.yml 로 붙여 넣기
└─ app/
   ├─ build.gradle.kts
   └─ src/
      ├─ main/AndroidManifest.xml                     # health.READ_* 권한, queries, 권한 이유 화면
      ├─ main/java/com/lukekim/hcbridge/
      │  ├─ MainActivity.kt                           # SDK 상태 확인 · 권한 요청 · 값 읽기 · 전송
      │  ├─ PermissionsRationaleActivity.kt           # Android 14+ 권한 사용 이유 화면
      │  └─ HealthPayload.kt                          # 하루+ URL 생성 (순수 함수)
      ├─ main/res/…                                    # 레이아웃·문자열(한국어)·테마·아이콘
      └─ test/java/com/lukekim/hcbridge/HealthPayloadTest.kt
```
