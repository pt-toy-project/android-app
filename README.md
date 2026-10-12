# (평택 토이 프로젝트)

> "위인의 하루 루틴을 따라하고, 코인을 모아 아이템을 구매하는 동기부여 Android 앱"

## APK 다운로드 및 설치

Android Studio 없이 Android 휴대폰에서 바로 확인할 수 있습니다.

**[테스트용 APK 다운로드](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-debug.apk)**

1. Android 7.0 이상 휴대폰에서 위 링크를 열어 APK를 다운로드합니다.
2. 다운로드한 `android-app-debug.apk`를 열고 설치합니다. 요청되면 사용 중인 브라우저 또는 파일 앱의 앱 설치 권한을 허용합니다.
3. 설치된 `test` 앱을 실행합니다.

현재 APK는 로컬 데모 확인용 디버그 빌드이며, 위인 선택·날짜별 완료 기록·설정 저장을 확인할 수 있습니다. 코인 보상·상점·실제 알림은 미구현입니다. APK의 소스 버전과 SHA-256 검증 방법은 [다운로드 안내](downloads/README.md)를 참고하세요.

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
* **위인 루틴 제공**: 다양한 위인들의 실제 하루 일과와 습관을 바탕으로 한 루틴 목록 제공
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

현재 앱은 로컬 데모입니다. 6명의 위인 목록과 분야 필터, 날짜별 예시 루틴, 위인·날짜별 완료 기록, 홈 진행률, 설정값 저장을 제공합니다. 루틴은 위인의 실제 역사적 일과를 검증한 자료가 아니라 공통 예시입니다.

완료 기록과 마지막 위인 선택은 `routine_state_v1`, 사운드·알람 선호 값은 `app_settings` SharedPreferences에 저장합니다. 로그인, 서버 API, 관계형 DB, 코인 보상, 아이템 상점, 실제 사운드 재생·알람 예약은 구현되지 않았습니다. 선택 날짜는 화면 재생성 시 복원하고 새 실행에서는 오늘을 표시합니다.

점검 내용, 결함 수정, 데이터 저장 한계, 검증 결과는 [프로젝트 점검 보고서](docs/project-audit.md)를 참고하세요.

## 개발 및 검증

JDK 25, Gradle Wrapper 9.5.0, Android SDK 37 및 Build Tools 36.0.0을 사용합니다. 저장소 밖의 SDK를 `ANDROID_HOME`으로 지정하고 `bash gradlew`로 실행합니다.

클라우드 환경에서는 다음과 같이 실행합니다.

```bash
bash /workspace/.android-cloud/install.sh --activate-only
source /workspace/.android-cloud/env.sh
cd /workspace/android-app
bash gradlew --no-daemon --console=plain \
  :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

단위 테스트 결과: `app/build/test-results/testDebugUnitTest/`

Lint 보고서: `app/build/reports/lint-results-debug.html`

`assembleDebugAndroidTest`는 기기 테스트 APK를 빌드하며 테스트를 실행하지 않습니다. 연결된 Android 기기가 있을 때 `bash gradlew :app:connectedDebugAndroidTest`로 실행하세요. 기기 테스트는 별도 설치 환경에서 실행하고 실제 사용자 데이터가 있는 기기에서 실행하지 마세요.
