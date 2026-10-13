# (평택 토이 프로젝트)

> "위인의 하루 루틴을 따라하고, 코인을 모아 아이템을 구매하는 동기부여 Android 앱"

## APK 다운로드 및 설치

**[v3.0 APK 다운로드 — 30명·간결 화면](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3-debug.apk)**

파일 이름은 `android-app-v3-debug.apk`입니다. 실행 후 오늘 화면 상단의 **3.0 · 간결 화면 1013**, 인물 화면의 **인물 · 30명**으로 새 빌드를 구분합니다. 목록과 하루 블록은 제목·보조 한 줄을 기본으로 하고 긴 설명은 자세히에서 엽니다.

**기존 v2와 서명이 달라 이 APK를 v2 위에 업데이트 설치할 수 없습니다.** 기존 데이터가 있는 앱을 삭제하지 마세요. 기존 서명 키를 확보해 같은 키로 다시 빌드해야 데이터 보존 업데이트가 가능합니다. 이미 현재 v3 서명으로 설치된 앱에는 같은 인증서를 유지한 APK를 제공합니다. 실제 휴대폰 업데이트 검증은 미실행입니다.

Android 7.0 이상, 기존 앱이 없는 테스트 기기에서 APK를 열어 설치할 수 있습니다. 최신 SHA-256·소스 커밋·서명 제한은 [다운로드 안내](downloads/README.md)를 참고하세요. 이전 README의 첫 링크가 v2 파일을 가리켰던 오류를 수정했습니다. 기존 v2 파일은 보존했습니다.

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

30명의 인물 목록·검색·분야 필터·하루 미리보기를 제공하며 8블록을 한 번에 시작합니다. 오늘과 나만의 계획은 분리합니다. 기록된 사실과 현대적 실천 제안, 원문에 있는 시간과 앱에서 제안하는 시간을 구분합니다. 인물 전체의 하루를 지어내지 않았으며, 셰익스피어는 개인 일과를 확인하지 못해 작품 기반 창작 실천임을 명시했습니다.

SQLite에 개인 루틴, 완료 기록, 코인, 구매·적용한 칭호를 저장합니다. 완료 한 번에 10코인을 지급하며, 같은 인물·습관·날짜는 삭제 후 다시 추가해도 중복 보상하지 않습니다. 설정값은 기존 `app_settings`를 유지하고, 기존 `routine_state_v1` 기록은 원본을 변경하지 않고 별도 이전 기록으로 보존합니다. 이전 샘플 기록에는 코인을 소급 지급하지 않습니다.

로그인·서버 동기화·현금 결제는 없습니다. 코인은 기기 안에서 사용하는 포인트입니다. [3.0 구현 보고서](docs/implementation-v3.md), [역사 자료](docs/historical-sources.md), [이미지 출처·라이선스](docs/image-credits.md), [로컬 DB](docs/local-database.md), [알림 구현과 제한](docs/alarm-implementation.md)을 참고하세요.

## 개발 및 검증

JDK 25, Gradle Wrapper 9.5.0, Android SDK 37 및 Build Tools 36.0.0을 사용합니다. 저장소 밖의 SDK를 `ANDROID_HOME`으로 지정하고 `bash gradlew`로 실행합니다.

클라우드 환경에서는 다음과 같이 실행합니다.

```bash
bash /workspace/.android-cloud/install.sh --activate-only
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
