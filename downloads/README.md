# 위인의 하루 v3.9 테스트 APK

**[v3.9 테스트 APK 다운로드 — 읽기 쉬운 주간 시간표·가져오기 미리보기](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.9-debug.apk)**

요일 머리글을 유지하며 시간대를 위아래로 보고, 기본 크기에서 주중 다섯 요일을 한눈에 볼 수 있게 했습니다. 수업별 색·간결한 카드·목록 보기·상단 추가 버튼을 제공하며, 공유 링크/CSV를 저장하기 전에 주간 표를 미리 볼 수 있습니다.

**기존 v3.3 배포 APK와 서명이 다릅니다. v3.3 위에 업데이트 설치할 수 없으므로 기존 앱과 데이터를 삭제하지 마세요.** v3.8 테스트 APK와는 서명·버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.9 / versionCode 12 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 9,248,832 B |
| APK SHA-256 | `5420d2c08a529e08012868dafa57ea7aa9512594968316e1b3d4dfe8facf13dd` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 165개·SQLite 22개 통과, lint 오류 0개·경고 189개, 앱·기기 테스트 APK 빌드 및 앱 APK 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·휴대폰에서의 에타 가져오기 |

[구현·검증 보고서](../docs/upgrade-v3.9.md) · [빌드 정보](v3.9-build-manifest.json) · [체크섬](android-app-v3.9-debug.apk.sha256) · [주간 표](../docs/screenshots/v3.9-timetable-grid-night.png) · [가져오기 미리보기](../docs/screenshots/v3.9-timetable-import-preview-night.png)

```bash
cd downloads
sha256sum --check android-app-v3.9-debug.apk.sha256
```

## v3.8 이전 테스트 APK

**[v3.8 테스트 APK 다운로드 — 에타 시간표 가져오기 오류 수정](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.8-debug.apk)**

v3.7에서 공유 링크의 수업 XML을 휴대폰에서 읽지 못하던 문제를 수정했습니다. 실제 공개 응답을 확인하고 Android XML 파서로 교체했으며, 익명화한 응답 구조로 테스트했습니다. **수정 APK를 실제 휴대폰에 설치해 가져오기 성공을 확인한 것은 아닙니다.**

**기존 v3.3 배포 APK와 서명이 다릅니다. v3.3 위에 업데이트 설치할 수 없으므로 기존 앱과 데이터를 삭제하지 마세요.** v3.7 테스트 APK와는 서명·버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.8 / versionCode 11 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 9,243,049 B |
| APK SHA-256 | `491bf3caf1e0583358ca0168be45234e8aa190fc47eed1dfa807881d27cff588` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 163개·SQLite 22개 통과, lint 오류 0개·경고 184개, 앱·기기 테스트 APK 빌드 및 앱 APK 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·휴대폰에서의 에타 가져오기 |

[구현·검증 보고서](../docs/upgrade-v3.8.md) · [빌드 정보](v3.8-build-manifest.json) · [체크섬](android-app-v3.8-debug.apk.sha256)

```bash
cd downloads
sha256sum --check android-app-v3.8-debug.apk.sha256
```

## v3.7 이전 테스트 APK

**[v3.7 테스트 APK 다운로드 — 달력 폭 수정·에타 공개 시간표 가져오기](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.7-debug.apk)**

월간 달력이 좁은 열로 압축되는 문제를 수정했습니다. 시간표 공유 URL을 붙여넣으면 공개된 수업을 가져와 확인 후 로컬 시간표에 추가할 수 있습니다. 로그인·비밀번호·쿠키는 받지 않습니다. 직접 입력과 CSV도 사용할 수 있습니다. 클라우드에서 에브리타임 API 접근이 제한되어 실제 공유 링크 가져오기 성공은 아직 검증하지 못했습니다.

**기존 v3.3 배포 APK와 서명이 다릅니다. 이 테스트 APK는 v3.3 위에 업데이트 설치할 수 없습니다. 기존 앱이나 데이터를 삭제하지 마세요.** v3.6 테스트 APK와는 서명·버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.7 / versionCode 10 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 11,046,848 B |
| APK SHA-256 | `652aed3f308a09109d6e0ed312535f3b5bb274320fd5b28a69bd5c9001575259` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 162개·SQLite 22개 통과, lint 오류 0개·경고 185개, 앱·기기 테스트 APK 빌드 및 앱 APK 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·에타 API 실수업 응답 |

[구현·검증 보고서](../docs/upgrade-v3.7.md) · [빌드 정보](v3.7-build-manifest.json) · [체크섬](android-app-v3.7-debug.apk.sha256) · [달력 화면](../docs/screenshots/v3.7-calendar-wide-font-night.png)

```bash
cd downloads
sha256sum --check android-app-v3.7-debug.apk.sha256
```

## v3.6 이전 테스트 APK

**[v3.6 테스트 APK 다운로드 — 시간표·홈 위젯·알림창 카드](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.6-debug.apk)**

수업을 직접 추가하거나 앱의 UTF-8 CSV 형식으로 가져올 수 있습니다. 에브리타임 공유 링크는 저장·열기만 지원하고 자동 수업 가져오기는 아직 지원하지 않습니다. 홈 화면 위젯은 런처에 추가할 수 있으며, 삼성 알림창과 같은 알림 영역에는 사용자가 켠 경우에만 시간표 카드가 표시됩니다.

**기존 배포 v3.3과 서명이 다릅니다. 이 APK로 v3.3 위에 업데이트 설치할 수 없습니다. 기존 앱이나 데이터를 삭제하지 마세요.** v3.5 테스트 APK와는 서명과 버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.6 / versionCode 9 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 11,027,540 B |
| APK SHA-256 | `f94fbfd196f35572e68db3517ec0279833d2a11ec1c80b4cb75638161f0db36b` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 159개·SQLite 22개 통과, lint 오류 0개·경고 185개, 앱·기기 테스트 APK 빌드 및 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·런처 위젯·알림창 표시·TalkBack·Doze |

[구현·검증 보고서](../docs/upgrade-v3.6.md) · [빌드 정보](v3.6-build-manifest.json) · [체크섬](android-app-v3.6-debug.apk.sha256) · [시간표 화면](../docs/screenshots/v3.6-timetable-dark.png)

```bash
cd downloads
sha256sum --check android-app-v3.6-debug.apk.sha256
```

## v3.5 이전 테스트 APK

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
