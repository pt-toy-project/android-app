# v3.0 저작권·라이선스 확인

신규 설명은 자체 작성한 짧은 요약/활동 제안이며 원문·현대 번역·긴 인용·시·노래 가사를 새로 복사하지 않았습니다. 역사 텍스트 URL 47개와 공공누리 이용조건 URL은 실제 요청했으나 CONNECT 403으로 차단되었습니다. 해당 자료의 정확한 위치, 국내외 보호기간·번역·편집 권리, 공공누리 종류 및 재사용 조건은 미확인입니다. 필요 시 법률 확인이 필요합니다. 퍼블릭 도메인이라고 일괄 단정하지 않습니다. 기존 이미지 파일과 저자·라이선스 크레딧은 보존합니다.

2026-10-13에 아래 라이선스 파일을 HTTP 200으로 실제 내려받아 Apache License 2.0 전문을 확인했습니다. 앱 assets/licenses에 원문 라이선스를 포함했습니다. 기존 의존성 버전은 바꾸지 않았으며 추가 외부 폰트가 없습니다. 앱은 Android 시스템 sans-serif를 사용합니다. 시스템 폰트를 별도로 번들하지 않습니다. JUnit/Espresso는 테스트 의존성입니다.

- AndroidX: https://raw.githubusercontent.com/androidx/androidx/androidx-main/LICENSE.txt
- Material Components: https://raw.githubusercontent.com/material-components/material-components-android/master/LICENSE
- Kotlin: https://raw.githubusercontent.com/JetBrains/kotlin/master/license/LICENSE.txt
- 공공누리 제1유형: https://www.kogl.or.kr/info/licenseType1.do — 미확인: curl(56) CONNECT 403

위 upstream 라이선스 확인은 모든 개별 전이 의존성의 NOTICE나 특정 번들 artifact에 대한 법률 판단을 대체하지 않습니다. 각 배포 artifact의 NOTICE 추가 검토가 필요할 수 있습니다. 신규 벡터 아바타는 앱 자체 제작(외부 이미지 없음)입니다.

## UI 검증 의존성

Robolectric 4.16.1은 테스트에만 사용하며 APK에 포함하지 않습니다. Maven POM(https://repo.maven.apache.org/maven2/org/robolectric/robolectric/4.16.1/robolectric-4.16.1.pom)과 공식 LICENSE(https://raw.githubusercontent.com/robolectric/robolectric/master/LICENSE)를 실제 HTTP 200으로 확인했습니다. MIT 및 함께 제공된 Apache 고지를 test-licenses/robolectric-MIT.txt에 보관합니다. 앱의 신규 폰트·런타임 이미지/라이브러리 추가는 없습니다.

## v3.1 서브 퀘스트

기존 30명만 유지하고 새 유명인·사진·로고·번역문·폰트·런타임 라이브러리를 추가하지 않았다. 세부 실천 문구는 직접 작성했으며 현대 적용으로 표시한다. 사망한 인물이라고 모든 권리가 자유롭다고 판단하지 않는다. 기존 자료 이용조건·개별 권리 확인의 미확인 범위는 그대로 남는다. 공개 배포 전 미확인 자료·권리 조건과 앱의 인물 이름 활용에 대한 검토가 필요하며 공식 제휴·추천을 주장하지 않는다. 기존 이미지 파일·이미지 크레딧을 보존한다.

## v3.2 Google 연동 SDK

공식 POM 4개를 실제 HTTP 200으로 확인했다. AndroidX Credentials / credentials-play-services-auth 1.5.0은 Apache 2.0, googleid 1.1.1과 Play services Auth 21.4.0은 Android SDK License로 선언된다. 약관 URL https://developer.android.com/studio/terms.html 은 실제 요청했지만 CONNECT 403으로 실패했다. 선언 확인과 약관 본문 확인을 구분하며 새 SDK 및 전이 의존성의 배포 조건/NOTICE 검토는 완결됐다고 주장하지 않는다. [접근 기록](google-library-access.json)을 참고한다. 앱 크레딧에 별도 계정 연동 라이브러리 경로를 추가하고 기존 이미지 크레딧 파일을 보존했다.

Google SDK 표준 로그인 버튼의 번들 리소스를 사용했다. 새 사진·AI 이미지·프로필 사진 다운로드는 없다. 기존 인물 이미지 원본은 0 B 증가이며 SDK 내부 비트맵 증가(압축 전)는 28,040 B다. 앱 등록/동의 화면/개인정보처리방침·스토어 데이터 보안 신고와 실제 권한 취소/계정 변경 테스트는 출시 전 필요한 미완료 항목이다.
