# Android Studio 없이 앱 확인하기

**[android-app-debug.apk 다운로드](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-debug.apk)**

| 항목 | 값 |
| --- | --- |
| 종류 | 서명된 테스트용 디버그 APK |
| 지원 기기 | Android 7.0(API 24) 이상 |
| 설치 후 표시 이름 | test |
| 패키지 | com.example.test |
| 앱 버전 | 1.0 (versionCode 1) |
| 소스 커밋 | 891784ac03e8d3347c58802f622740438c281f71 |
| 검증 | 앱 빌드·APK 서명 검증 성공, JVM 테스트 24개 통과 |

휴대폰에서 다운로드한 APK를 열고 설치합니다. 설치 권한을 요청하면 해당 브라우저나 파일 앱의 앱 설치 권한을 허용한 뒤 다시 진행합니다. 저장소가 비공개인 경우 접근 권한이 있는 GitHub 계정으로 로그인해야 합니다.

위인 목록·분야 필터, 공통 예시 루틴, 날짜별 완료 기록, 홈 진행률, 설정값 저장을 확인할 수 있습니다. 코인·상점·실제 사운드/알림·서버 동기화는 구현되지 않았습니다. 기기 테스트는 실행하지 않았으므로 빌드/단위 테스트 성공과 실제 기기 동작 검증을 구분합니다.

SHA-256 파일은 [android-app-debug.apk.sha256](android-app-debug.apk.sha256)입니다. PC에서 선택적으로 검증하려면 두 파일을 같은 폴더에 내려받고 실행합니다.

```bash
sha256sum --check android-app-debug.apk.sha256
```

이 파일은 위 소스 커밋을 빌드한 스냅샷입니다. 이후 코드만 수정해도 자동으로 갱신되지 않으며 새 APK를 빌드해 함께 교체해야 합니다. 빌드 결과는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다.
