# 테스트 APK

[**위인의 하루 v3.15 다운로드**](https://github.com/pt-toy-project/android-app/raw/refs/heads/main/downloads/android-app-v3.15-debug.apk) · [변경 내용](../docs/upgrade-v3.15.md) · [이전 버전](../docs/releases.md)

이 APK는 Android 7.0 이상용 디버그 빌드입니다. **기존 배포 v3.3과 서명이 달라 덮어쓰기 설치할 수 없습니다. 기존 앱과 데이터를 삭제하지 마세요.** v3.14 테스트 APK와는 서명·버전 조건이 맞지만 실제 업데이트 설치는 검증하지 않았습니다.

| 항목 | v3.15 |
| --- | --- |
| 패키지 / 버전 | `com.example.test` / versionCode 18 |
| APK 크기 | 9,268,117 B |
| SHA-256 | `e3064d1d938b8bc41e6648ed96f442c4cd1aa21d10f10c59a48b8f975239ac8f` |
| 서명 인증서 SHA-256 | `bc33faab57ff446fdffd9faf5d73dd397da37fa9eaa5a0baeb9811e952e013a0` |
| 검증 | JVM 테스트 175개, SQLite 검증 22개 통과; lint 오류 0개, 경고 197개; 앱·기기 테스트 APK 빌드 |
| 미실행 | 실제 기기 테스트, 실제 업데이트 설치 |

[빌드 정보](v3.15-build-manifest.json) · [체크섬 파일](android-app-v3.15-debug.apk.sha256)

```bash
cd downloads
sha256sum --check android-app-v3.15-debug.apk.sha256
```

이전 APK와 빌드 정보는 이 디렉터리에 보존되어 있습니다.
