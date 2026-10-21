# 위인의 하루

역사적 인물의 루틴을 골라 실천하고, 완료 기록과 코인을 쌓는 Android 앱입니다. 계정 없이 시작할 수 있습니다.

[**최신 테스트 APK v3.14 다운로드**](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.14-debug.apk) · [화면 보기](docs/screenshots/v3.14-timetable-personal-night.png) · [변경 내용](docs/upgrade-v3.14.md)

> 기존에 배포된 v3.3 앱과 테스트 APK는 서명이 달라 덮어쓰기 설치할 수 없습니다. 기존 앱과 데이터를 삭제하지 마세요. 설치·검증 정보는 [다운로드 안내](downloads/README.md)에 있습니다.

## 주요 기능

- **오늘:** 선택한 루틴을 실천하고, 타이머와 날짜별 완료 기록을 확인합니다.
- **인물:** 인물을 검색하고 루틴을 살펴본 뒤 시작합니다.
- **시간표:** 주간 수업을 보고, 아래에서 개인 계획을 추가·관리합니다. 공개 에브리타임 링크·CSV 가져오기를 지원합니다.
- **상점·설정:** 획득한 코인으로 아이템을 구매하고, 알림과 파일 백업·복원을 관리합니다.

루틴·완료 기록·코인·시간표는 기기의 로컬 DB에 저장됩니다. 핵심 기능에 로그인이나 서버가 필요하지 않습니다.

## 빌드와 검증

클라우드 Android 환경에서 다음 명령을 사용합니다.

```bash
bash /workspace/.android-cloud/install.sh --activate-only
source /workspace/.android-cloud/env.sh
cd /workspace/android-app
bash gradlew --no-daemon --console=plain --max-workers=4 \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
python3 scripts/verify_database.py
```

빌드 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. `assembleDebugAndroidTest`는 기기 테스트 APK만 빌드하며 실제 기기 테스트를 실행하지 않습니다.

[이전 버전](docs/releases.md) · [프로젝트와 팀](docs/project-background.md) · [역사 자료](docs/historical-sources.md) · [로컬 DB](docs/local-database.md)
