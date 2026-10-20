# (평택 토이 프로젝트)

> "위인의 하루 루틴을 따라하고, 코인을 모아 아이템을 구매하는 동기부여 Android 앱"

## v3.5 테스트 버전

계정 없는 시작·백업·타이머 개선에 월간 실천 달력, 목표별 인물 추천, 통계와 달성 배지를 더했습니다. 밝은 회색 바탕과 파란색 강조, 넓은 여백으로 화면을 정리했으며 다크 모드도 지원합니다.

**[v3.5 테스트 APK 다운로드](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.5-debug.apk)**

**기존 배포 v3.3과 서명이 달라 덮어쓰기 설치할 수 없습니다. 기존 앱과 데이터를 삭제하지 마세요.** 기존 배포 키로 재빌드한 뒤 업데이트 설치를 검증해야 합니다. 현재 APK는 별도 테스트 환경용입니다.

버전은 3.5 / versionCode 8이며 Android 7.0 이상을 지원합니다. JVM 154개·SQLite 21개 통과, lint 오류 0·경고 174개입니다. 기기 테스트 APK는 빌드했으며 실제 기기 실행과 업데이트 설치 검증은 미실행입니다. Google 로그인 없이 핵심 기능과 파일 백업·복원을 사용할 수 있습니다.

[다운로드·체크섬·서명 안내](downloads/README.md) · [v3.5 구현·검증 결과](docs/upgrade-v3.5.md) · [앞선 v3.4 개선](docs/upgrade-v3.4.md) · [다음 작업 프롬프트](docs/next-upgrade-prompt.md)

화면 미리보기: [월간 달력](docs/screenshots/v3.5-calendar.png) · [통계와 배지](docs/screenshots/v3.5-progress.png). Robolectric에서 시험 기록으로 렌더링한 화면입니다.

## 프로젝트 소개
본 프로젝트는 캡스톤 디자인을 준비하며 팀원 간의 협업 경험을 쌓기 위해 진행된 토이 프로젝트입니다. 사용자가 역사적 위인들의 훌륭한 습관과 하루 루틴을 직접 체험하고 실천할 수 있도록 돕습니다. 루틴 달성 시 코인을 지급하여 사용자의 지속적인 동기를 유발합니다.

* **목적**: 캡스톤 디자인 전 사전 협업 경험 축적 및 Android 앱 개발 사이클 경험
* **개발 플랫폼**: Android

## 팀 구성 (Team)
| 역할 | 이름 | 담당 업무 |
| :---: | :---: | :--- |
| **PM** | 백승효 | 기획 및 Figma 화면 설계, 프로젝트 일정 관리 |
| **Frontend** | 박성호 | 프론트엔드 팀장 |
| **Frontend** | 박진영 | 프론트엔드 팀원 및 Git 관리 |
| **Frontend** | 변주혁 | 프론트엔드 팀원 |
| **Backend** | 백수현 | 백엔드 팀장 |
| **Backend** | 배재한 | 백엔드 팀원 |
| **Backend** | 윤동규 | 백엔드 팀원 |

## 주요 기능 (Key Features)
* **위인 루틴 제공**: 30명의 자료·가치관과 연결한 하루 제안, 근거 등급·자료 확인 상태 표시
* **루틴 실천 및 트래킹**: 사용자가 선택한 루틴의 달성 여부를 체크하고 기록 
* **보상 시스템**: 루틴 완료 시 코인 지급
* **아이템 상점**: 모은 코인을 활용해 앱 내에서 사용할 수 있는 다양한 아이템 구매

## 🛠️ 기술 스택 (Tech Stack)
### Frontend
* **Language**: `Kotlin`
* **UI**: `XML`

### Backend


### Design & Collaboration
* **Design**: `Figma`
* **Version Control**: `Git`, `GitHub`
* **Communication**: `Discord`, `Notion`

## 🌲 브랜치 전략 (Branch Strategy)
* `main`: 배포 가능한 안정적인 버전
* `develop`: 다음 버전을 위해 기능들이 병합되는 개발 브랜치
* `feat/*`: 프론트엔드/백엔드 각 담당자의 세부 기능 개발 브랜치 (예: `feature/room-database`)

## 현재 구현 상태

240개 블록에 510개 서브 퀘스트가 있습니다. 체크는 루틴·날짜별로 저장하며 PERSONAL/HERO_DAY가 서로 섞이지 않습니다. 서브 퀘스트 자체에는 보상을 지급하지 않고 기존 부모 블록 완료 보상을 유지합니다. 펠레·호날두는 예시로만 언급됐으며 추가하지 않았습니다.

30명의 인물 목록·검색·분야 필터·하루 미리보기를 제공하며 8블록을 한 번에 시작합니다. 오늘과 나만의 계획은 분리합니다. 새 세부 동작·시간·횟수는 모두 현대 적용 제안으로 표시합니다. 기존 자료 확인 상태를 보존하며, 셰익스피어는 작품 기반 창작 실천을 유지합니다. 뉴턴·다 빈치·나이팅게일의 저술 기반 제안을 실제 고정 일과로 표시하지 않습니다.

SQLite에 개인 루틴, 완료 기록, 코인, 구매·적용한 칭호를 저장합니다. 완료 한 번에 10코인을 지급하며, 같은 인물·습관·날짜는 삭제 후 다시 추가해도 중복 보상하지 않습니다. 설정값은 기존 `app_settings`를 유지하고, 기존 `routine_state_v1` 기록은 원본을 변경하지 않고 별도 이전 기록으로 보존합니다. 이전 샘플 기록에는 코인을 소급 지급하지 않습니다.

Google 로그인·Drive 백업은 앱 등록 후 활성화하며 현재 APK에서는 준비 중입니다. 서버 동기화·현금 결제는 없습니다. 코인은 기기 안에서 사용하는 포인트입니다. [3.0 구현 보고서](docs/implementation-v3.md), [역사 자료](docs/historical-sources.md), [이미지 출처·라이선스](docs/image-credits.md), [로컬 DB](docs/local-database.md), [알림 구현과 제한](docs/alarm-implementation.md)을 참고하세요.

## 개발 및 검증

JDK 25, Gradle Wrapper 9.5.0, Android SDK 37 및 Build Tools 36.0.0을 사용합니다. 저장소 밖의 SDK를 `ANDROID_HOME`으로 지정하고 `bash gradlew`로 실행합니다.

클라우드 환경에서는 다음과 같이 실행합니다.

```bash
if [ -f /workspace/.android-cloud/install.sh ]; then
  bash /workspace/.android-cloud/install.sh --activate-only
fi
source /workspace/.android-cloud/env.sh
cd /workspace/android-app
bash gradlew --no-daemon --console=plain \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
python3 scripts/verify_database.py
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

단위 테스트 결과: `app/build/test-results/testDebugUnitTest/`

Lint 보고서: `app/build/reports/lint-results-debug.html`

`assembleDebugAndroidTest`는 기기 테스트 APK를 빌드하며 테스트를 실행하지 않습니다. 연결된 Android 기기가 있을 때 `bash gradlew :app:connectedDebugAndroidTest`로 실행하세요. 기기 테스트는 별도 설치 환경에서 실행하고 실제 사용자 데이터가 있는 기기에서 실행하지 마세요.

## 3.3 회귀 테스트 파일

- [FocusTimerStoreTest](app/src/test/java/com/example/test/focus/FocusTimerStoreTest.kt): 시간 보존·일시정지·재부팅·이전 종료 이벤트 보호.
- [ExperienceReminderTest](app/src/test/java/com/example/test/alarm/ExperienceReminderTest.kt): 실제 PendingIntent·권한/채널·시험 알림의 보상 분리(Robolectric).
- [AlarmRuntimeTest](app/src/androidTest/java/com/example/test/alarm/AlarmRuntimeTest.kt): Android OS의 예약 전달, 두 출처의 실제 상세 화면 연결, 중복 보상 방지.
- [ScreenRenderTest](app/src/test/java/com/example/test/ScreenRenderTest.kt): 실제 View 렌더링·집중 화면·큰 글꼴·회전 후 상태 보존.
- [RoutineActivityTest](app/src/androidTest/java/com/example/test/RoutineActivityTest.kt): 날짜·인물 선택 보존과 실천 시작 빠른 연속 탭의 상세 중복 방지.

이하 내용은 **v3.3의 이전 검증 기록**입니다. 최신 v3.5 결과는 위의 구현·검증 문서를 참고하세요. v3.3은 JVM 127개·SQLite 21개와 네 가지 Gradle 작업을 통과했습니다. lint 오류 0·경고 166입니다. Android 35 가상 기기에서 v3.2 → v3.3 업데이트 설치가 성공했고 모든 DB 행·기존 루틴 ID·완료 기록·구매/적용·80포인트를 비교해 보존을 확인했습니다. 실제 휴대폰 업데이트와 스피커 청취·TalkBack·절전/Doze는 미실행입니다. OS 알림 테스트의 최종 결과와 APK 체크섬은 [다운로드 안내](downloads/README.md)에 기록합니다.

Android 35에서 알림 시험·정규 예약/출처 연결/중복 보상·미래 완료 보호·빠른 연속 탭 등 계측 4개를 통과했습니다. 실행한 5개 중 달력/회전 1개는 가상 기기의 반복 System UI ANR로 창 포커스를 확보하지 못해 실패했고 나머지 47개는 미실행입니다. 이 제한을 통과로 표시하지 않습니다.
