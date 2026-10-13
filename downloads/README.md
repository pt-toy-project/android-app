# 위인의 하루 v3.1 테스트 APK

**[최신 v3.1 APK 다운로드 — 서브 퀘스트](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.1-debug.apk)**

파일 이름은 `android-app-v3.1-debug.apk`입니다. 실행 후 오늘/설정의 **3.1 · 서브 퀘스트**, 인물의 **인물 · 30명** 표시를 확인하세요. 30명·240블록에 510개 구체적인 행동·제안 분량과 날짜별 체크를 추가했습니다. 목표별 인물 탐색과 아침·점심·저녁 항목을 제공하며 기존 기록·설정·보상 보호를 유지합니다. 펠레·호날두는 예시이며 추가하지 않았습니다.

이 저장소에서 배포한 v3.0 APK와 패키지·인증서가 같고 versionCode를 3→4로 올렸습니다. 해당 v3.0이 설치된 기기에서 파일을 열어 업데이트 설치를 시도할 수 있습니다. **실제 휴대폰 업데이트 설치는 미실행입니다. 기존 v2와는 서명이 달라 업데이트가 불가능하므로 기록 보존을 위해 v2 앱을 삭제하지 마세요.** v2 호환 업데이트에는 원래 서명 키가 필요합니다.

| 항목 | 값 |
|---|---|
| 버전 | 3.1 / versionCode 4 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| 대응 소스 커밋 | `0084273bc796fbdb6dc13a3a66d41ba2b4896ebe` |
| 파일 크기 | 9,352,396 B |
| APK SHA-256 | `9f5ab9ba5bcf793e781633fe9630421a9762055ee512caa35195dda932eca5df` |
| 인증서 SHA-256 | `bf3489b7cb1b71f6f452b1e3caea6859e29365b69a37d5d8680c536dc5f81c4c` |
| 검증 | JVM 104개·SQLite 21개 통과, 앱·계측 APK 빌드 성공, lint 오류 0·경고 130, apksigner 성공 |
| 미실행 | 계측 50개 실행·휴대폰 설치/업데이트·TalkBack·OS 알림·실기기 글꼴/성능 측정 |

Robolectric/Android 35에서 실제 목표 선택창 항목과 체크박스를 클릭하고 체크 저장·진행 상태·코인 미지급을 검증했습니다. 기존 v1/v2 SQLite 파일 fixture의 업그레이드와 데이터 보존, 부모 블록 완료 및 두 출처 중복 코인 방지도 실행했습니다. 시뮬레이션과 실제 기기 실행은 구분합니다. 자세한 범위는 [구현·검증 보고서](../docs/quest-implementation.md)를 참고하세요.

기존 사진 15장·벡터 아바타·이미지 크레딧은 보존했으며 이미지 증가 0 B입니다. 새 인물·외부 이미지·AI 이미지·폰트·라이브러리는 추가하지 않았습니다. 새 동작·시간·횟수는 모두 직접 작성한 앱 제안이며 기존 자료 확인 상태를 유지합니다. 미확인 이용조건과 출시 전 권리 검토는 남습니다. 코인은 현금 가치가 없는 로컬 포인트이며 로그인·서버 동기화·현금 결제는 없습니다.

처음 설치하는 테스트 기기는 Android 7.0 이상에서 파일을 열고 요청된 브라우저/파일 앱의 설치 권한을 허용합니다. 비공개 저장소라면 권한이 있는 GitHub 계정이 필요합니다. 이전 v2/v3.0 APK와 체크섬은 보존했습니다. [v2 보존 안내](v2-archive.md)는 현재 주 다운로드가 아닙니다.

체크섬 파일: [android-app-v3.1-debug.apk.sha256](android-app-v3.1-debug.apk.sha256).

```bash
sha256sum --check android-app-v3.1-debug.apk.sha256
```

빌드·테스트 실행 명령:

```bash
source /workspace/.android-cloud/env.sh
bash gradlew --no-daemon --console=plain \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
python3 scripts/verify_database.py
```

`assembleDebugAndroidTest`는 APK 빌드이며 기기 테스트 실행이 아닙니다.
