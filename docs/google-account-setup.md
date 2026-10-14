# Google 로그인 켜기: 처음 설정하는 분을 위한 안내

현재 APK에는 로그인·계정 선택·Drive 백업 코드가 있지만 Google 앱 등록값이 없어 실제 Google 연결은 준비 중입니다. 먼저 계정 없이 사용하거나 파일 백업을 사용할 수 있습니다. 로그인 성공을 흉내 내거나 임의의 클라이언트 ID를 넣지 않았습니다.

## Google Cloud에서 한 번 설정할 내용

1. 본인 Google 계정으로 https://console.cloud.google.com/ 에 접속해 새 프로젝트를 만듭니다. 프로젝트 이름은 ‘위인의 하루’처럼 알아보기 쉽게 정합니다.
2. ‘Google Auth Platform’에서 앱 이름, 지원 이메일과 개발자 연락 이메일을 설정합니다. 테스트 단계라면 외부 사용자 대상의 테스트 상태로 두고 본인 계정을 테스트 사용자에 넣습니다. 메뉴 이름은 콘솔 개편에 따라 달라질 수 있습니다.
3. ‘클라이언트’에서 **Android** 유형을 만듭니다. 패키지는 `com.example.test`, 현재 테스트 APK의 SHA-1은 아래 값입니다.
4. 같은 프로젝트에서 **웹 애플리케이션** 유형의 클라이언트를 만듭니다. 발급된 ‘클라이언트 ID’(`…apps.googleusercontent.com`)를 복사합니다. 앱에 필요한 값은 이 공개 ID입니다. **비밀번호, 클라이언트 비밀 키나 서비스 계정 개인 키를 앱·저장소에 넣지 않습니다.**
5. ‘API 및 서비스 → 라이브러리’에서 **Google Drive API**를 사용 설정합니다. 백업할 때 앱 전용 폴더 권한만 요청합니다.
6. 발급된 웹 클라이언트 ID를 개발자에게 전달해 같은 서명으로 APK를 다시 빌드합니다. 이 ID는 OAuth 비밀 키가 아닙니다. 사용자 본인 Google 계정으로 첫 로그인과 Drive 백업·복원을 실제 테스트해야 합니다.

현재 테스트 서명 SHA-1:

```text
F3:AC:C5:63:58:43:2F:89:4C:08:E2:A4:04:72:49:B5:A0:52:DE:2A
```

현재 v3 계열 서명 SHA-256은 `bf3489b7cb1b71f6f452b1e3caea6859e29365b69a37d5d8680c536dc5f81c4c`입니다. 키를 새로 만들지 않습니다. 기존 v2의 다른 서명 키는 이 환경에 없어 v2 호환 업데이트 제한이 남습니다.

개발자의 빌드 명령:

```bash
source /workspace/.android-cloud/env.sh
bash gradlew --no-daemon --console=plain \
  -PgoogleWebClientId=발급된ID.apps.googleusercontent.com \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

플레이 스토어용 서명이나 패키지를 확정하면 해당 인증서/패키지의 Android 클라이언트 등록도 필요합니다. 현재 테스트 인증서만으로 스토어 서명 등록까지 했다고 주장하지 않습니다.

## 앱 안의 흐름

처음 실행 → Google로 로그인 또는 계정 없이 시작 → 짧은 사용 안내 → 인물 선택 → 하루 시작. 계정 연결은 선택이며 알림 상세 연결은 튜토리얼로 가로막지 않습니다. 설정의 ‘사용 안내 다시 보기’에서 다시 열 수 있습니다.

설정 → 계정과 백업에서 계정을 연결하고 ‘Google에 백업’을 누르면 Drive 권한 확인 후 백업합니다. 로그인만으로 자동 업로드하지 않습니다. 복원은 백업 정보 확인과 사용자의 ‘이 백업으로 복원’ 클릭 후 진행하며 현재 기기의 복원 전 기록도 보관합니다. 로그아웃해도 로컬 기록은 삭제하지 않습니다.

ID 토큰을 서버로 검증하는 자체 로그인 서버나 실시간 다중 기기 동기화는 구현하지 않았습니다. Google SDK가 선택한 계정 정보를 표시하고 실제 Drive 작업마다 Google 권한을 다시 확인합니다. 이름·이메일만 기기에 캐시하며 ID/접근/갱신 토큰을 파일에 저장하거나 백업하지 않습니다.

## 출시 전 남은 확인

실제 로그인·동의·권한 취소·계정 변경·Drive 백업/복원 테스트, OAuth 공개 상태와 스토어 서명 등록, 공개 개인정보처리방침과 데이터 보안 신고가 필요합니다. 현재 환경에는 사용자의 Google Cloud 프로젝트/관리 권한이 없으므로 이를 완료했다고 보고하지 않습니다.

SDK 공식 POM은 실제 HTTP 200으로 확인했습니다. AndroidX Credentials는 Apache 2.0, Google Identity/Play services Auth는 Android SDK License로 선언됩니다. Google 약관 본문은 CONNECT 403으로 접근하지 못했습니다. [접근 기록](google-library-access.json)을 참고하세요. 이 안내는 콘솔 조작을 이 환경에서 실제 수행했다는 기록이 아닙니다.
