# AdGuard Waker

Android용 AdGuard 자동화 앱. 루트 및 상주 서비스 없이 AdGuard 공식 자동화 Broadcast Intent를 이용합니다. AGPL-3.0 (기존 LICENSE 유지).

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
- 실행 설정 간 시각적 간격 및 상태 카드 유지, 하단 도움말 문단 삭제. 배터리 최적화 버튼은 중간 앱 정보 화면 없이 휴대폰 시스템의 배터리 사용량 화면(`ACTION_POWER_USAGE_SUMMARY`)으로 직접 이동합니다. 지원되지 않는 기기는 배터리 절약 설정, 배터리 최적화 관리 설정 순으로 이동을 시도합니다. 제조사마다 실제 설정 화면은 다를 수 있으며, AdGuard Waker 항목을 자동 선택하거나 강조하지는 않습니다.
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
| Optimized | `app/build/outputs/apk/optimized/app-optimized.apk` | 켬 (코드·리소스 축소) | Debug 서명 |
| Release | `app/build/outputs/apk/release/app-release-unsigned.apk` (별도 빌드 필요) | 켬 | 서명 설정 없음 |

`optimized`는 기존 `release`를 상속하여 R8 최적화 설정을 유지하지만, 설치 테스트를 위해 **임시 Debug 키로 서명**합니다. 공식 배포용 서명이 아닙니다. GitHub Actions의 Debug 서명키는 실행마다 달라질 수 있으므로, 기존 앱 업데이트 설치 시 서명 불일치가 발생하면 설정을 백업하고 기존 앱을 제거한 뒤 다시 설치해야 합니다. 앱 제거 시 저장된 설정이 삭제됩니다.

GitHub Actions에서는 APK 두 종류를 별도의 아티팩트로 업로드하며, Actions 실행의 **Step summary**에서 압축 전 실제 APK 파일 크기와 최적화 전후 절감률을 비교할 수 있습니다. GitHub 아티팩트 ZIP 크기와 혼동하지 마세요.

정식 배포에는 개인 소유의 **고정된 Release 서명키**를 별도로 설정하여 R8 최적화된 Release APK에 서명해야 합니다. 키 파일이나 비밀번호를 공개 저장소에 커밋하지 마세요.

## 수동 검증 체크리스트

- 올바른/틀린 비밀번호, AdGuard 미설치, 외부 자동화 해제.
- 키패드와 다이얼로 각각 1분/720분 입력, 721분 이상 거부, 다이얼 취소 및 확인, 자동 저장 후 재실행.
- 1분/5분/60분 주기, 개별 동작 켜기/끄기, 주기 변경 및 앱 종료 후 재예약.
- 키 입력 디바운스, 토글 즉시 저장, 다이얼 확인 후 즉시 저장, 뒤로가기/앱 전환 중 저장, 비밀번호 입력 중 불필요한 재시작 방지, 기존 720분 초과 주기 마이그레이션 확인.
- 배터리 버튼으로 별도 앱 정보 화면 없이 폰 설정의 배터리 페이지가 바로 열리는지 확인. 해당 Intent 미지원 기기에서 배터리 절약 설정 또는 배터리 최적화 관리 화면으로 이동하는지 확인.
- 정확한 알람 허용/거부/취소 후 앱 열기 또는 복구 작업 확인.
- 화면 꺼짐, Doze, 배터리 제한, 재부팅 및 최초 잠금 해제, 앱 업데이트.
- Android 12/13/14/15/16/17에서 실제 AdGuard와 검증.
- Waker의 자체 알림 없음, AdGuard VPN 알림은 별개.

참고: [AdGuard 공식 자동화 문서](https://adguard.com/kb/ko/adguard-for-android/solving-problems/tasker/) · [Android alarm](https://developer.android.com/develop/background-work/services/alarms) · [Doze](https://developer.android.com/training/monitoring-device-state/doze-standby).
