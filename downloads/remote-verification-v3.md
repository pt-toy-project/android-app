# v3 원격 다운로드 검증

이 문서는 초기 v3 게시본의 검증 기록입니다. 후속 코드 개선 APK의 최신 체크섬과 소스는 [v3-build-manifest.json](v3-build-manifest.json)을 참고하세요.

2026-10-13, `origin`이 `https://github.com/pt-toy-project/android-app.git`임을 확인하고 강제 푸시 없이 main에 반영했습니다.

- 구현 소스 커밋: `1c0343a` (전체 ID: v3-build-manifest.json)
- APK 게시 커밋: `62a3189da637198de94901156360dd1c4dd2ff83`
- git ls-remote로 위 게시 커밋이 원격 main임을 실제 확인했습니다.
- [v3 APK](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3-debug.apk)를 curl로 실제 내려받았습니다. SHA-256 `5b1ec993f2f26408cb65631af7bc181ca4cf5dbcd69907e187405dcc64bcdb3e`, 로컬 downloads 파일과 cmp 일치(종료 코드 0).
- [기존 v2 APK](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-debug.apk)도 실제 내려받았습니다. SHA-256 `d5d7b482564624aa26f0f6e158957ebdafae16e0ad75401d475e255865d0f706`, 로컬 보존 파일과 cmp 일치(종료 코드 0).
- 두 로컬 체크섬 파일 모두 sha256sum --check 성공. apksigner에서 기존 v2와 신규 v3 서명 자체는 유효하지만 인증서는 불일치하므로 호환 업데이트가 아닙니다.

역사 자료와 공공누리 사이트의 CONNECT 403 실패, 라이선스 원문의 HTTP 200 접근, GitHub fetch/push/download 성공은 각각 실제 결과로 구분합니다. 네트워크 설정 초안을 저장하거나 실행 환경 정책을 수정하지 않았습니다. 기존 자료 접근 실패를 다운로드 성공으로 바꾸어 기록하지 않습니다.
