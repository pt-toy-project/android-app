# 위인의 하루 v3.13 테스트 APK

**[v3.13 테스트 APK 다운로드 — 계획 탭을 설정으로 교체](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.13-debug.apk)**

하단 메뉴가 오늘 · 인물 · 시간표 · 상점 · 설정으로 바뀌었습니다. 기존 개인 계획은 오늘 화면의 ‘내 계획 보기’에서 계속 관리할 수 있고, 날짜별 실천 기록은 오늘의 월간 달력에 남아 있습니다. 저장 데이터와 DB 구조는 변경하지 않았습니다.

**기존 v3.3 배포 APK와 서명이 다릅니다. v3.3 위에 업데이트 설치할 수 없으므로 기존 앱과 데이터를 삭제하지 마세요.** v3.12 테스트 APK와는 서명·버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.13 / versionCode 16 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 9,435,001 B |
| APK SHA-256 | `4448cddc702e254499bfef59c69e9ee2bc72062a6f98990a25bdf1e9c1ac47ff` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 171개·SQLite 22개 통과, lint 오류 0개·경고 196개, 앱·기기 테스트 APK 빌드 및 앱 APK 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·휴대폰 화면 확인 |

[변경·검증 보고서](../docs/upgrade-v3.13.md) · [빌드 정보](v3.13-build-manifest.json) · [체크섬](android-app-v3.13-debug.apk.sha256) · [오늘](../docs/screenshots/v3.13-today-night.png) · [설정](../docs/screenshots/v3.13-settings-night.png)

```bash
cd downloads
sha256sum --check android-app-v3.13-debug.apk.sha256
```

## v3.12 이전 테스트 APK

**[v3.12 테스트 APK 다운로드 — 주간 시간표를 첫 화면에](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.12-debug.apk)**

시간표 탭을 열면 요일·시간 격자가 먼저 나옵니다. 기본 화면 폭에서 월~토 수업을 함께 보고, 일요일은 옆으로 밀어 볼 수 있습니다. 수업은 매주 반복하므로 주 이동과 날짜 숫자를 없애고 목록에도 요일만 표시합니다. 추가·에타 가져오기·위젯 등의 도구는 표 아래에 있습니다.

**기존 v3.3 배포 APK와 서명이 다릅니다. v3.3 위에 업데이트 설치할 수 없으므로 기존 앱과 데이터를 삭제하지 마세요.** v3.11 테스트 APK와는 서명·버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.12 / versionCode 15 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 9,259,299 B |
| APK SHA-256 | `d32521961449487979f9f5abdeb4b8a9d82d82bd5ca0763cfd82def286efc001` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 169개·SQLite 22개 통과, lint 오류 0개·경고 194개, 앱·기기 테스트 APK 빌드 및 앱 APK 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·휴대폰 화면 확인 |

[구현·검증 보고서](../docs/upgrade-v3.12.md) · [빌드 정보](v3.12-build-manifest.json) · [체크섬](android-app-v3.12-debug.apk.sha256) · [주간 격자](../docs/screenshots/v3.12-timetable-first-night.png) · [요일별 목록](../docs/screenshots/v3.12-timetable-list-night.png)

```bash
cd downloads
sha256sum --check android-app-v3.12-debug.apk.sha256
```

## v3.11 이전 테스트 APK

**[v3.11 테스트 APK 다운로드 — 시간표 위젯·마이페이지·광고 영역](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.11-debug.apk)**

홈 화면 위젯에 앱 카드와 같은 오늘 수업 시간·이름·강의실을 표시합니다. 크기가 작거나 글씨가 크면 한 수업과 남은 개수를 보여 줍니다. 마이페이지에 계정 없는 사용 상태, 파일 백업·복원, 기록·알림을 정리했습니다. 인물 추천 화면에는 디버그 빌드에서 확인할 수 있는 광고 영역을 만들었습니다. 실제 광고는 연결하지 않았습니다.

**기존 v3.3 배포 APK와 서명이 다릅니다. v3.3 위에 업데이트 설치할 수 없으므로 기존 앱과 데이터를 삭제하지 마세요.** v3.10 테스트 APK와는 서명·버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.11 / versionCode 14 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 11,072,258 B |
| APK SHA-256 | `ea19f1cee0098ed81055e015be2a2d060f31ea598b379d8c15ab2e25f89b8966` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 169개·SQLite 22개 통과, lint 오류 0개·경고 193개, 앱·기기 테스트 APK 빌드 및 앱 APK 서명 검증 성공 |
| 미실행 | 실제 기기 계측·런처 위젯 표시·업데이트 설치·실제 광고 송출 |

[구현·검증 보고서](../docs/upgrade-v3.11.md) · [빌드 정보](v3.11-build-manifest.json) · [체크섬](android-app-v3.11-debug.apk.sha256) · [마이페이지](../docs/screenshots/v3.11-settings-light.png) · [위젯](../docs/screenshots/v3.11-widget-two-classes-night.png)

```bash
cd downloads
sha256sum --check android-app-v3.11-debug.apk.sha256
```

## v3.10 이전 테스트 APK

**[v3.10 테스트 APK 다운로드 — ‘오늘’ 시간표 카드 정리](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.10-debug.apk)**

수업마다 시간·이름·강의실을 따로 표시하고, 나머지 수업 개수와 전체 시간표 링크를 제공합니다. 달력에서 선택한 날짜의 요일 수업을 보여 주며, 큰 글씨에서도 날짜와 달력 버튼이 겹치지 않습니다.

**기존 v3.3 배포 APK와 서명이 다릅니다. v3.3 위에 업데이트 설치할 수 없으므로 기존 앱과 데이터를 삭제하지 마세요.** v3.9 테스트 APK와는 서명·버전 조건이 맞지만 실제 덮어쓰기 설치는 검증하지 않았습니다.

| 항목 | 값 |
| --- | --- |
| 버전 | 3.10 / versionCode 13 |
| 패키지 / 최소 OS | com.example.test / Android 7.0(API 24)+ |
| APK 크기 | 11,060,338 B |
| APK SHA-256 | `3f49ccb5a373e60f14985be0b6d88b3db6f203995ff985d8915596f3b5cbe56c` |
| 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 167개·SQLite 22개 통과, lint 오류 0개·경고 189개, 앱·기기 테스트 APK 빌드 및 앱 APK 서명 검증 성공 |
| 미실행 | 실제 기기 계측·업데이트 설치·휴대폰에서의 에타 가져오기 |

[구현·검증 보고서](../docs/upgrade-v3.10.md) · [빌드 정보](v3.10-build-manifest.json) · [체크섬](android-app-v3.10-debug.apk.sha256) · [기본 화면](../docs/screenshots/v3.10-today-timetable-compact.png) · [큰 글씨](../docs/screenshots/v3.10-today-timetable-large-font.png)

```bash
cd downloads
sha256sum --check android-app-v3.10-debug.apk.sha256
```

## v3.9 이전 테스트 APK

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
