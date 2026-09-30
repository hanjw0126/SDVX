# VOLTEX MATE

SOUND VOLTEX ∇ 플레이어를 위한 **비공식 팬메이드** 안드로이드 앱입니다.
KONAMI 와 무관하며, 개인 용도로 사용하는 것을 권장합니다.

## 기능
- **공식 로그인**: 앱 안의 WebView 로 `p.eagate.573.jp` 에 직접 로그인합니다. 비밀번호는 앱이 보거나 저장하지 않고, 세션 쿠키만 기기에 남습니다.
- **프로필**: 닉네임 · SV-ID · 어필 칭호 · VOLFORCE(공식 클래스 배지와 단계) · 플레이 현황 · 클리어/그레이드 표. 프로필 사진은 공식 어필카드 또는 갤러리 사진.
- **VF 대상곡**: 공식 *スコアデータ CSV* 를 가져와 VF 에 반영되는 상위 50 차트를 계산하고 1080px PNG 로 저장·공유합니다.
- **최근 플레이**: 공식 매칭 이력(최근 20곡)을 불러올 때마다 누적하고, CSV 를 다시 가져오면 점수·클리어가 오른 차트를 기록합니다.
- **랭킹**: 공식 RANKING 메뉴의 스코어 랭킹 · 위클리 스코어 어택 · 배틀 랭킹. 표를 읽지 못하면 공식 페이지를 앱 안에서 열 수 있습니다.
- **데모 모드**: 로그인 없이 가상 데이터로 전체 UI 를 볼 수 있습니다.

> 상세 플레이 데이터·CSV 는 보통 e-amusement 베이직 코스가 필요합니다.

## 설치
[Releases](../../releases) 에서 `VoltexMate-x.y.z.apk` 를 받아 설치합니다.

## 빌드
- JDK 17, Android SDK 34
- Android Studio 로 열거나 `gradle :app:assembleDebug` (Gradle 8.7)
- `gradlew` 스크립트를 쓰려면 먼저 `gradle wrapper --gradle-version 8.7` 을 한 번 실행하세요.

## 릴리즈 (GitHub Actions)
| 워크플로 | 트리거 | 결과 |
|---|---|---|
| `Build` | main 푸시 / PR | 디버그 APK 아티팩트 |
| `Release APK` | GitHub Release 게시 | 서명된 APK 를 해당 릴리즈에 첨부 |

필요한 저장소 시크릿: `KEYSTORE_BASE64`(PKCS12 키스토어 base64), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
키스토어 파일은 절대 커밋하지 마세요. 키를 잃어버리면 기존 설치본에 업데이트(덮어쓰기)할 수 없습니다.

## 파서가 맞지 않을 때
공식 사이트 구조가 바뀌면 데이터가 비어 보일 수 있습니다. 프로필 메뉴의 **HTML 공유** 또는 랭킹 화면의 **HTML 공유** 로 원본을 받아 `data/Parsers.kt` 의 셀렉터를 수정하세요.

## 라이선스
MIT. SOUND VOLTEX 관련 명칭·이미지·데이터의 권리는 KONAMI 에 있으며, 공식 이미지는 실행 중 공식 서버에서 불러올 뿐 저장소에 포함하지 않습니다.
