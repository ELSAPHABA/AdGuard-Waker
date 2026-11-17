# AdGuard Waker

Android용 AdGuard 자동화 앱. 루트 및 상주 서비스 없이 AdGuard 공식 자동화 Broadcast Intent를 이용합니다. AGPL-3.0 (기존 LICENSE 유지).

## 제작 이유

AdGuard를 비활성화 하신 후 다시 활성화를 까먹으시는 부모님을 위해 제작함.
Tasker류 어플리케이션은 무겁고 유료이기 때문에 초경량 무료 앱을 제작하게 됨.

## 기능

- `start` (보호 시작)과 `update` (필터 및 앱 업데이트 확인)의 개별 활성화 및 1~720분 주기.
- 각 주기는 숫자 키패드로 직접 입력하거나 다이얼을 돌려 설정합니다. 체크박스와 다이얼 확정은 즉시, 글자 입력은 650ms 디바운스 후 유효한 값만 자동 저장합니다. 앱을 떠날 때 대기 중인 입력도 저장하며, 저장 버튼은 수동 확인용으로 남겨둡니다.
- 기본값: 시작 5분, 업데이트 확인 60분. 최초 자동화는 꺼져 있습니다.
- 활성화 시 보호 시작을 즉시 요청하고, 업데이트는 첫 주기 이후 요청합니다. 재부팅 후 최초 잠금 해제 시 동일하게 복구합니다.
- 정확한 알람 권한이 있으면 exact alarm, 없으면 inexact alarm으로 폴백. 하나의 알람으로 두 작업을 처리하고 15분 주기의 시스템 JobScheduler로 복구를 보조합니다.
- 자체 알림, foreground service, 네트워크 권한, 외부 런타임 라이브러리 없음.
- AdGuard 4.x receiver 우선, 이전 receiver 호환 경로. quiet=true로 toast 억제 요청.
- 비밀번호는 앱 전용 SharedPreferences에 저장(별도 암호화 없음, 백업 제외). 명령 전송 시에만 AdGuard에 전달합니다.
- 뒤로가기·제스처, 앱 전환 시에도 유효한 설정을 저장합니다. 미완성/범위 초과 숫자 입력은 기존 유효한 주기를 유지합니다. 기존 720분 초과 주기는 업데이트 시 720분으로 보정하고 예정 시각을 재설정합니다.
- 최상단 AdGuard Waker 제목을 굵게 표시합니다. 배터리 최적화 버튼은 삼성 갤럭시에서 AdGuard Waker 앱의 배터리 설정 상세 화면(`android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL`, `package:io.github.elsaphaba.adguardwaker`)을 바로 열도록 먼저 시도합니다. 해당 비공개 액션을 지원하지 않는 기기에서는 Waker 앱 정보 화면으로 이동하며 배터리 항목은 수동 선택해야 합니다. 기종과 One UI 버전에 따라 다를 수 있습니다.
- Java 소스 전체를 Kotlin으로 변경하였으며, Kotlin 2.4.20과 JUnit4 테스트 사용.

## 설치 및 설정

1. AdGuard for Android에서 외부 앱 자동화 기능을 켜고 비밀번호를 확인합니다.
2. Waker에 비밀번호를 입력하고 '지금 보호 시작'/'지금 업데이트 확인'으로 실제 AdGuard 반응을 확인합니다.
3. 원하는 주기(1~720분)를 입력하거나 다이얼로 선택합니다. 설정은 변경 후 자동 저장됩니다.
4. 정확한 알람이 필요하면 '알람 및 리마인더 권한'에서 허용합니다. 기기별 자동 시작 및 배터리 최적화 설정도 확인합니다. AdGuard 자체의 배터리 제한도 해제해야 할 수 있습니다.

## Android 제약

minSdk 31 (Android 12), compileSdk/targetSdk 36 (Android 16). 최신 버전에서의 실행을 막는 maxSdkVersion은 설정하지 않습니다. Android 12 이상 및 Android 17 실기기 검증은 별도로 필요합니다.
정확한 알람도 Doze 및 제조사 정책에 따라 지연되며, Doze 중에는 앱당 9분보다 자주 허용되지 않을 수 있습니다.
비정확 알람은 더 지연될 수 있습니다. 앱 강제 종료, 시스템의 앱 제한, VPN 권한 미승인 또는 AdGuard 강제 종료 상태에서는 실행이 보장되지 않습니다.
정확한 알람 권한 취소 시 OS가 기존 알람을 삭제할 수 있으며 복구 작업이나 앱 재실행으로 재예약합니다.
누락된 반복 명령은 몰아서 전송하지 않습니다. 다음 주기는 실제 시도 시점부터 계산합니다.
`update`는 업데이트 검사 요청이며 강제 앱 설치가 아닙니다. Broadcast 결과 응답이 문서화되지 않아 UI는 실제 보호 여부/업데이트 성공이 아니라 전송 시도만 기록합니다.
Waker는 자체 상태바 알림을 만들지 않지만 AdGuard 및 Android의 VPN 표시/알림을 숨길 수 없습니다.

## 빌드

JDK 17, Gradle 8.13, Android SDK Platform 36 / Build Tools 35.0.0, AGP 8.13.2, Kotlin 2.4.20.
Android Studio에서 프로젝트를 열거나 설치된 Gradle로 실행합니다 (바이너리 wrapper는 포함하지 않음).

```sh
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug :app:assembleOptimized :app:lintDebug :app:lintOptimized
```

빌드 결과는 다음과 같습니다.

| 유형 | 위치 | R8 | 서명 |
|---|---|---|---|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` | 끔 | Debug 서명 |
| Optimized test | `app/build/outputs/apk/optimized/app-optimized.apk` | 켬 | 매번 달라질 수 있는 Debug 서명 |
| Release | `app/build/outputs/apk/release/app-release.apk` (아래 4개 Secrets 설정 후 main CI 빌드) | 켬 | 고정 개인 서명키 |

**업데이트 설치용 APK는 `AdGuard-Waker-release-stable`만 사용하세요.** 이전 CI의 Debug/Optimized APK는 실행마다 서명 인증서가 달라질 수 있어 업데이트 설치가 실패합니다. 서명 일치와 새로운 `versionCode`가 모두 필요합니다. 기존 임시 서명 APK에서 고정 서명 APK로 전환하는 첫 설치는 일반 업데이트가 불가능합니다. 기존 설정이 필요한 경우 앱 삭제 전에 보존 방법을 별도로 마련하세요(앱은 백업 기능을 제공하지 않음). 이후에는 같은 키로 서명한 Release APK 사이에서 설정을 유지하는 업데이트 설치가 가능합니다.

### 영구 서명키 최초 1회 설정

1. JDK가 설치된 신뢰할 수 있는 컴퓨터에서 아래 명령어를 실행해 비공개 서명키를 생성합니다.

```bash
keytool -genkeypair -v -keystore adguard-waker-release.jks -storetype PKCS12 -alias adguard-waker -keyalg RSA -keysize 3072 -validity 10000
```

2. 키 파일을 **외부 안전한 장소에 백업**합니다. 비밀번호도 별도 보관합니다. 키가 유실되면 같은 인증서로 업데이트를 배포할 수 없습니다.
3. 키 파일을 Base64로 인코딩합니다: Linux에서는 `base64 -w0 adguard-waker-release.jks`, Windows PowerShell에서는 `[Convert]::ToBase64String([IO.File]::ReadAllBytes("adguard-waker-release.jks"))`.
4. GitHub 저장소의 **Settings → Secrets and variables → Actions → New repository secret**에서 아래 네 가지 Secret을 생성합니다.

| Secret 이름 | 값 |
|---|---|
| `WAKER_SIGNING_KEYSTORE_BASE64` | Base64 인코딩한 키 파일 전체 |
| `WAKER_SIGNING_STORE_PASSWORD` | 키 저장소 비밀번호 |
| `WAKER_SIGNING_KEY_ALIAS` | `adguard-waker` |
| `WAKER_SIGNING_KEY_PASSWORD` | 키 비밀번호 (별도 질문이 없었다면 저장소 비밀번호와 같을 수 있음) |

5. 위 설정 후 **main 브랜치**에서 GitHub Actions의 Android build 워크플로를 실행합니다. PR 빌드에서는 안전을 위해 비공개 서명키를 사용하지 않습니다. Secret이 없으면 기존 Debug 및 임시 Optimized 테스트 APK만 생성되고 Release APK는 생략됩니다.

6. 생성된 **`AdGuard-Waker-release-stable`** 아티팩트에서 APK를 내려받습니다. main CI는 매 실행 `10000 + GITHUB_RUN_NUMBER`를 사용하여 `versionCode`가 계속 증가합니다. 로컬 빌드는 기본 `versionCode=3`, `versionName=1.2.0`입니다. CI 배포 앱 위에 로컬 기본 빌드를 덮어쓰면 버전 다운그레이드 오류가 날 수 있습니다.

GitHub Actions Step summary에서 APK 크기와 서명 인증서 SHA-256 지문을 확인할 수 있습니다. **개인 서명키 또는 비밀번호를 공개 저장소에 커밋하지 마세요.**

참고: [AdGuard 공식 자동화 문서](https://adguard.com/kb/ko/adguard-for-android/solving-problems/tasker/) · [Android alarm](https://developer.android.com/develop/background-work/services/alarms) · [Doze](https://developer.android.com/training/monitoring-device-state/doze-standby).
