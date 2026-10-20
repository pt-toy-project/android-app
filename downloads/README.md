# 위인의 하루 v3.5 테스트 APK

**[v3.5 테스트 APK 다운로드 — 달력·통계·배지](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.5-debug.apk)**

월간 달력과 날짜별 실천 링, 목표별 인물 추천, 통계·달성 배지를 추가했습니다. 밝은 배경과 파란색 강조를 사용하며 다크 모드를 지원합니다. 계정 없는 시작·파일 백업·타이머 상태 복원·알림 안내 개선도 포함합니다.

**기존 배포 v3.3과 서명이 다릅니다. 이 APK는 기존 배포본 위에 업데이트 설치할 수 없습니다. 기존 앱이나 데이터를 삭제하지 마세요.** 데이터 보존 업데이트에는 기존 배포 키로 다시 빌드한 APK와 실제 업데이트 검증이 필요합니다. 현재 파일은 별도 테스트 환경용입니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.5 / versionCode 8 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 10,892,603 B |
| APK SHA-256 | `fc2c7d002d84dedf5c4f9f9df4ad6844fcd5cdf75793eb37b9d80c766aba7fcc` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 대응 소스 | [`ada146c`](https://github.com/pt-toy-project/android-app/commit/ada146c5fd44bd18b17685cdf72061a7d1970c50) |
| 검증 | JVM 154개·SQLite 21개 통과, lint 오류 0·경고 174개, 앱·기기 테스트 APK 빌드 및 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·실제 소리/진동·TalkBack·Doze |

로그인 없이 핵심 기능을 사용할 수 있습니다. Google 연결 코드는 유지했으며 Google 앱 등록은 이번 기능의 필수 조건이 아닙니다. 타이머 종료·달력 조회·배지 달성으로 별도 코인을 지급하지 않습니다.

[구현·검증 보고서](../docs/upgrade-v3.5.md) · [다음 작업 프롬프트](../docs/next-upgrade-prompt.md) · [빌드 정보](v3.5-build-manifest.json) · [체크섬](android-app-v3.5-debug.apk.sha256)

```bash
cd downloads
sha256sum --check android-app-v3.5-debug.apk.sha256
```

화면 미리보기는 Robolectric에서 시험 기록으로 렌더링한 실제 Android View입니다: [월간 달력](../docs/screenshots/v3.5-calendar.png) · [통계와 배지](../docs/screenshots/v3.5-progress.png).

이전 배포 파일과 검증 기록은 보존했습니다: [v3.3](v3.3-archive.md) · [v3.2 APK](android-app-v3.2-debug.apk) · [v3.1 APK](android-app-v3.1-debug.apk) · [v2 안내](v2-archive.md).
