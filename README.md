# AdGuard Waker

Android용 AdGuard 전용 자동화 앱. Tasker, 루트, 상주 서비스 없이 AdGuard 공식 자동화 Broadcast Intent를 이용합니다. GPL-3.0 (기존 LICENSE 유지).

## 기능

- `start` (보호 시작)과 `update` (필터 및 앱 업데이트 확인)의 개별 활성화 및 1~10,080분 주기.
- 기본값: 시작 5분, 업데이트 확인 60분. 최초 자동화는 꺼져 있습니다.
- 활성화 시 보호 시작을 즉시 요청하고, 업데이트는 첫 주기 이후 요청합니다. 재부팅 후 최초 잠금 해제 시 동일하게 복구합니다.
- 정확한 알람 권한이 있으면 exact alarm, 없으면 inexact alarm으로 폴백. 하나의 알람으로 두 작업을 처리하고 15분 주기의 시스템 JobScheduler로 복구를 보조합니다.
- 자체 알림, foreground service, 네트워크 권한, 외부 런타임 라이브러리 없음.
- AdGuard 4.x receiver 우선, 이전 receiver 호환 경로. quiet=true로 toast 억제 요청.
- 비밀번호는 앱 전용 SharedPreferences에 저장(별도 암호화 없음, 백업 제외). 명령 전송 시에만 AdGuard에 전달합니다.
- 최근 명령 전송 시도 및 다음 목표 시각 표시. 한국어 UI, 시스템 다크 모드 지원.

## 설치 및 설정

1. AdGuard for Android에서 외부 앱 자동화 기능을 켜고 비밀번호를 확인합니다.
2. Waker에 비밀번호를 입력하고 '지금 보호 시작'/'지금 업데이트 확인'으로 실제 AdGuard 반응을 확인합니다.
3. 원하는 분 주기와 자동화를 활성화하고 저장합니다.
4. 정확한 알람이 필요하면 '알람 및 리마인더 권한'에서 허용합니다. 기기별 자동 시작 및 배터리 최적화 설정도 확인합니다. AdGuard 자체의 배터리 제한도 해제해야 할 수 있습니다.

## Android 제약

minSdk 31 (Android 12), compileSdk/targetSdk 37 (Android 17). Android 12~17 실기기 검증은 별도로 필요합니다.
정확한 알람도 Doze 및 제조사 정책에 따라 지연되며, Doze 중에는 앱당 9분보다 자주 허용되지 않을 수 있습니다.
비정확 알람은 더 지연될 수 있습니다. 앱 강제 종료, 시스템의 앱 제한, VPN 권한 미승인 또는 AdGuard 강제 종료 상태에서는 실행이 보장되지 않습니다.
정확한 알람 권한 취소 시 OS가 기존 알람을 삭제할 수 있으며 복구 작업이나 앱 재실행으로 재예약합니다.
누락된 반복 명령은 몰아서 전송하지 않습니다. 다음 주기는 실제 시도 시점부터 계산합니다.
`update`는 업데이트 검사 요청이며 강제 앱 설치가 아닙니다. Broadcast 결과 응답이 문서화되지 않아 UI는 실제 보호 여부/업데이트 성공이 아니라 전송 시도만 기록합니다.
Waker는 자체 상태바 알림을 만들지 않지만 AdGuard 및 Android의 VPN 표시/알림을 숨길 수 없습니다.

## 빌드

JDK 17, Gradle 9.3.1, Android SDK Platform 37 / Build Tools 36.0.0, AGP 9.1.1.
Android Studio에서 프로젝트를 열거나 설치된 Gradle로 실행합니다 (바이너리 wrapper는 포함하지 않음).

```sh
gradle --no-daemon :app:assembleDebug :app:lintDebug
javac -d /tmp/waker-test app/src/main/java/io/github/elsaphaba/adguardwaker/ScheduleMath.java tests/ScheduleMathTest.java
java -cp /tmp/waker-test io.github.elsaphaba.adguardwaker.ScheduleMathTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Debug APK는 테스트용이며 정식 배포에는 고정된 개인 서명키로 release APK를 서명해야 합니다.

## 수동 검증 체크리스트

- 올바른/틀린 비밀번호, AdGuard 미설치, 외부 자동화 해제.
- 1분/5분/60분 주기, 개별 동작 켜기/끄기, 주기 변경 및 앱 종료 후 재예약.
- 정확한 알람 허용/거부/취소 후 앱 열기 또는 복구 작업 확인.
- 화면 꺼짐, Doze, 배터리 제한, 재부팅 및 최초 잠금 해제, 앱 업데이트.
- Android 12/13/14/15/16/17에서 실제 AdGuard와 검증.
- Waker의 자체 알림 없음, AdGuard VPN 알림은 별개.

참고: [AdGuard 공식 자동화 문서](https://adguard.com/kb/ko/adguard-for-android/solving-problems/tasker/) · [Android alarm](https://developer.android.com/develop/background-work/services/alarms) · [Doze](https://developer.android.com/training/monitoring-device-state/doze-standby).
