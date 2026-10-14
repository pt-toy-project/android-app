# 위인의 하루 v3.2 테스트 APK

**[최신 APK 다운로드 — 시작 안내·백업·화면 정리](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.2-debug.apk)**

첫 실행에서 Google 로그인 또는 계정 없이 시작과 3단계 사용 안내를 제공합니다. **현재는 Google 앱 등록값이 없어 Google 연결이 준비 중입니다.** [처음 Google 로그인 설정하는 방법](../docs/google-account-setup.md)을 따라 공개 웹 클라이언트 ID를 발급하고 재빌드해야 실제 연결이 활성화됩니다. 로그인 성공을 흉내 내지 않습니다. 계정 없이 시작, 파일 백업·복원, 기존 30명·240블록·510퀘스트는 사용할 수 있습니다.

오늘은 실천 시작을 주 동작으로 정리하고 같은 블록을 중복 표시하지 않습니다. 설정은 내 기록·알림·도움말로 묶고 진단 설명을 상세로 옮겼습니다. 파일 복원은 백업 정보와 확인 버튼을 거쳐 실행하며 복원 전 사본을 보관합니다. 체크섬·타입·버전·외래키·SQLite 제약과 기존 기록/보상 상태를 보호합니다.

| 항목 | 값 |
|---|---|
| 버전 | 3.2 / versionCode 5 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| 대응 소스 | `c6487d40c5b9bffab28dc5046a8723b9071ec4e6` |
| APK 크기 | 10,772,891 B |
| APK SHA-256 | `d9275d1a1df9833dd88178534e4281627085f58776259ad95ba347850f9825e0` |
| 인증서 SHA-256 | `bf3489b7cb1b71f6f452b1e3caea6859e29365b69a37d5d8680c536dc5f81c4c` |
| 검증 | JVM 119개·SQLite 21개 통과, 앱·계측 APK 빌드, lint 오류 0·경고 154, apksigner 성공 |
| 미실행 | 실제 Google 로그인·Drive API·기기 설치/업데이트·TalkBack·OS 알림·계측 50개 실행 |

이 저장소의 v3.0/v3.1과 같은 패키지·인증서이며 versionCode를 올렸습니다. 실제 휴대폰 업데이트는 미실행입니다. **기존 v2는 다른 서명 키여서 이 APK로 업데이트할 수 없습니다. 기존 데이터를 보존하려면 v2를 삭제하지 마세요.** 원래 키가 필요합니다. 이전 APK와 체크섬을 보존했습니다.

Robolectric/Android 35에서 첫 실행/건너뛰기/완료/다시 보기, 실제 SQLite 백업·복원과 실패 시 보존, 문서 선택창 실행, 화면 렌더링을 확인했습니다. 실제 파일 앱과 Google 계정 테스트는 별도입니다. [구현·검증 범위](../docs/experience-v3.2.md)를 참고하세요.

기존 15장 인물 사진·벡터 아바타·원래 크레딧은 그대로이고 인물 이미지 증가 0 B입니다. 새 Google SDK 표준 로그인 버튼 등의 의존성 비트맵은 APK 압축 전 기준 28,040 B 증가했습니다. AI 이미지·신규 인물 사진·프로필 사진 다운로드는 없습니다. AndroidX 라이선스와 Google SDK 라이선스 선언은 공식 POM을 실제 확인했고, Google 약관 본문은 CONNECT 403으로 미확인입니다. 신규 SDK 배포 조건과 앱 등록·개인정보처리방침·스토어 데이터 보안 검토가 남습니다.

체크섬: [android-app-v3.2-debug.apk.sha256](android-app-v3.2-debug.apk.sha256).

```bash
sha256sum --check android-app-v3.2-debug.apk.sha256
```

빌드와 검증:

```bash
source /workspace/.android-cloud/env.sh
bash gradlew --no-daemon --console=plain \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
python3 scripts/verify_database.py
```

Google 활성화 빌드는 위 명령에 `-PgoogleWebClientId=실제공개클라이언트ID.apps.googleusercontent.com`을 추가합니다. 비밀 키를 넣지 않습니다. `assembleDebugAndroidTest`는 기기 테스트 APK 빌드이며 실제 실행이 아닙니다.
