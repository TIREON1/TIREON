# TIREON

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Pleos%20Connect-000000?style=for-the-badge" />
  <img src="https://img.shields.io/badge/OS-Android%20Automotive-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/JDK-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Build-Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
</p>

<p align="center">
  <a href="https://github.com/TIREON1/TIREON/actions/workflows/ci.yml">
    <img src="https://github.com/TIREON1/TIREON/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI" />
  </a>
</p>

<p align="center">
  <b>운전을 배우는 순간부터, 더 나은 운전 습관까지</b>
  <br />
  초보 운전자를 위한 차량 데이터 기반 운전 코칭 서비스 TIREON
</p>

---

## 1. 프로젝트 소개

<strong>TIREON(티레온)</strong>은 초보 운전자가 자신의 운전 습관을 이해하고 개선할 수 있도록 돕는 차량 데이터 기반 운전 코칭 서비스입니다.

운전자는 주행 중 다양한 도로 상황과 차량 정보를 마주하지만, 자신의 운전 습관이 안전한지 객관적으로 판단하기 어렵습니다. 특히 운전 경험이 부족한 초보 운전자는 주행 중 발생하는 상황에 적절하게 대응하는 데 어려움을 겪을 수 있습니다.

TIREON은 Pleos Connect 플랫폼에서 차량 신호를 수집해 운전 습관을 분석하고, 그 결과를 음성과 화면으로 안내합니다. 공터에서 하는 연습 미션은 실시간 음성으로 코칭하고, 실제 도로 주행은 화면 없이 기록한 뒤 정차 후 리포트로 보여 줍니다.

단순히 차량 정보를 확인하는 것을 넘어, 운전자가 자신의 주행 패턴을 파악하고 안전한 운전 습관을 형성할 수 있도록 지원합니다.

### 프로젝트 목표

- **운전 습관 분석:** 차량 데이터를 기반으로 운전자의 주행 패턴을 파악합니다.
- **운전 코칭:** 측정한 운전 행동을 근거로 코칭 문장을 만들고, 음성(TTS)과 화면으로 개선 방향을 안내합니다.
- **운전 경험 개선:** 초보 운전자가 자신의 운전 상태를 이해하고 점진적으로 운전 능력을 향상할 수 있도록 돕습니다.
- **차량 데이터 활용:** Pleos Connect SDK를 활용하여 차량에서 제공하는 데이터를 서비스에 연동합니다.

## 2. 기획 배경 및 문제 정의

### 2.1 기획 배경

운전을 처음 시작하는 사람들은 도로 환경에 익숙하지 않을 뿐만 아니라, 자신의 운전 습관을 객관적으로 평가하기 어렵습니다.

운전 중 급가속, 급제동, 불필요한 조작 등은 운전 습관을 판단할 수 있는 요소이지만, 운전자가 이러한 행동을 반복적으로 인식하고 개선하기는 쉽지 않습니다.

기존의 차량 정보 제공 서비스는 차량 상태나 주행 정보를 확인하는 데 집중하는 경우가 많습니다. 하지만 운전자가 자신의 행동을 이해하고 실제 운전 습관을 개선하도록 돕는 개인화된 코칭에는 별도의 접근이 필요합니다.

이에 TIREON은 차량 데이터를 단순히 보여주는 것이 아니라, 데이터를 해석하고 운전자에게 이해하기 쉬운 피드백으로 전달하는 서비스를 기획했습니다.

### 2.2 문제 정의

| 문제                    | 설명                                                                                  |
| ----------------------- | ------------------------------------------------------------------------------------- |
| 운전 습관 파악의 어려움 | 자신의 운전 방식이 안전한지 객관적으로 판단하기 어렵습니다.                           |
| 주행 데이터 활용 부족   | 차량에서 수집할 수 있는 정보가 실제 운전 습관 개선으로 이어지기 어렵습니다.           |
| 개인화된 피드백 부족    | 운전자의 주행 특성과 수준에 맞는 구체적인 개선 방향을 얻기 어렵습니다.                |
| 초보 운전자의 불안감    | 운전 경험이 부족하여 다양한 주행 상황에 대한 판단과 대응에 어려움을 겪을 수 있습니다. |

### 2.3 해결 방향

TIREON은 다음과 같은 방향으로 문제를 해결하고자 합니다.

1. 차량 데이터를 수집하여 운전자의 주행 정보를 파악합니다.
2. 수집된 데이터를 기반으로 운전 습관과 주행 패턴을 분석합니다.
3. 분석 결과를 운전자가 이해하기 쉬운 피드백으로 전달합니다.
4. 운전자가 피드백을 바탕으로 자신의 운전 습관을 점진적으로 개선하도록 돕습니다.

## 3. 주요 기능

TIREON은 차량 신호 수집, 운전 습관 분석, 코칭 안내, 기록과 리포트를 하나의 흐름으로 제공합니다.

### 3.1 차량 데이터 연동

차량 신호를 100ms(10Hz) 간격으로 모아 하나의 샘플로 묶습니다.

| 신호 | 출처 |
| --- | --- |
| 속도, 기어 | AAOS Car API (`PERF_VEHICLE_SPEED`, `GEAR_SELECTION`) |
| 조향각, 브레이크, 방향지시등 | Pleos Vehicle SDK |
| 주행거리(Odometer), 차량 정보 | Pleos Vehicle SDK |

- 값을 받지 못한 신호는 0으로 채우지 않고 "측정 불가"로 남깁니다.
- 차량이 지원하지 않는 신호가 있어도 나머지 신호로 수집을 이어 갑니다.

### 3.2 운전 습관 분석

수집한 샘플에서 운전 행동 이벤트를 찾고 점수를 계산합니다.

- **급가속·급제동:** 가속도 ±0.3g(2.94m/s²) 이상이 0.5초 이상 이어진 경우
- **급조향:** 시속 20km 이상에서 조향각 변화 속도가 200°/s를 넘은 경우
- **방향지시등 누락:** 방향지시등 없이 핸들을 30° 이상 돌린 회전
- **점수:** 이벤트 종류별 가중치와 주행 거리로 100점 만점 점수를 계산합니다. 거리·시간·샘플이 부족하면 점수 대신 부족한 이유를 안내합니다.

### 3.3 운전 코칭

측정한 이벤트를 근거로 코칭 문장을 만들고, Pleos TextToSpeech SDK(ON_DEVICE)로 읽어 줍니다. 코칭 문장은 정해진 규칙으로 만들며, 화면에는 문장과 측정값을 나눠서 보여 줍니다.

- **연습 미션 6종:** 부드러운 출발, 부드러운 정지, 천천히 후진, 깜빡이 습관, 정속 유지, 우회전 일시정지. 연습 중에는 급가속·급제동 같은 이벤트를 바로 음성으로 안내하고, 시작할 때는 미션 안내를, 끝나면 미션 결과와 점수를 읽어 줍니다.
- **주행 미션 4종:** 부드러운 주행, 방향지시등 습관, 안정적인 조향, 전체 점검. 미션 없이 주행할 수도 있습니다. 실제 도로에서는 주행 중 음성 코칭을 하지 않고, 정차 후 결과만 안내합니다.
- 같은 안내가 짧은 간격으로 반복되지 않도록 재생 간격을 조절합니다.

코칭 결과는 참고용 정보이며, 차량의 안전 시스템이나 운전자의 직접적인 판단을 대체하지 않습니다.

### 3.4 기록과 리포트

- 세션별 점수, 이벤트 횟수, 미션 결과를 기기에 저장합니다.
- 리포트 목록과 상세 화면, 미션별 성적 통계를 제공합니다.
- 최근 기록을 바탕으로 다음 연습 미션을 추천합니다.
- 주간·야간 모드를 지원합니다.

### 3.5 주행 안전 설계

- **주행 중 화면 차단:** 홈·리포트·주행 기록 화면은 Distraction Optimization을 선언하지 않아, 주행 중에는 플랫폼이 화면을 가립니다.
- **연습 중 최소 UI:** 연습 화면만 Distraction Optimization을 선언하고, `CarUxRestrictions`가 제한을 걸면 숫자 하나와 음성만 남깁니다. 제한 상태를 알 수 없을 때도 최소 UI로 처리합니다.
- **주행 기록 중 내비게이션:** 주행 기록을 시작하면 지도 앱으로 넘어가고, 포그라운드 서비스로 화면 없이 기록을 이어 갑니다.
- **정차 후 결과:** 차가 움직이는 동안에는 결과 화면을 띄우지 않습니다.

### 3.6 실행 화면

Pleos Connect v2.1.0 에뮬레이터(2560×1440)에서 촬영한 화면입니다.

| 홈 | 연습 미션 |
| --- | --- |
| <img src="docs/screenshots/01_home.jpg" alt="홈" width="420"> | <img src="docs/screenshots/02_practice_missions.jpg" alt="연습 미션" width="420"> |
| **연습 준비** | **주행 중(시스템이 화면 차단)** |
| <img src="docs/screenshots/03_practice_ready.jpg" alt="연습 준비" width="420"> | <img src="docs/screenshots/04_driving_blocked.jpg" alt="주행 중" width="420"> |
| **정차 후 연습 종료 버튼** | **연습 결과와 코치 피드백** |
| <img src="docs/screenshots/05_parked_running.jpg" alt="정차 후" width="420"> | <img src="docs/screenshots/06_practice_result.jpg" alt="연습 결과" width="420"> |
| **리포트** | **야간 모드 홈** |
| <img src="docs/screenshots/07_report.jpg" alt="리포트" width="420"> | <img src="docs/screenshots/08_home_night.jpg" alt="야간 모드 홈" width="420"> |
| **야간 모드 리포트** | |
| <img src="docs/screenshots/09_report_night.jpg" alt="야간 모드 리포트" width="420"> | |

## 4. 기술 스택

TIREON은 Pleos Connect 기반 차량용 애플리케이션 개발 환경을 중심으로 구성합니다.

| 구분            | 기술                         | 용도                          |
| --------------- | ---------------------------- | ----------------------------- |
| Platform        | Pleos Connect                | 차량용 앱 플랫폼              |
| OS              | Android Automotive OS (AAOS) | 차량용 애플리케이션 실행 환경 |
| Language        | Kotlin                       | 애플리케이션 개발             |
| JDK             | Java 21                      | 개발 및 빌드 실행 환경        |
| SDK             | Pleos Connect SDK            | 차량 기능 및 데이터 연동      |
| IDE             | Android Studio               | 개발 및 디버깅                |
| Build           | Gradle                       | 프로젝트 빌드 및 의존성 관리  |
| Version Control | Git / GitHub                 | 소스 코드 및 버전 관리        |
| CI              | GitHub Actions               | 자동 빌드 및 검증             |
| Test            | Pleos Connect Emulator       | 애플리케이션 테스트           |

## 5. 시스템 아키텍처

TIREON은 차량 신호 수집부터 운전 습관 분석, 코칭 안내까지 이어지는 구조입니다. SDK 의존은 `:pleos`, 분석 로직은 Android에 의존하지 않는 `:core`, 화면은 `:app` 모듈에 둡니다.

### 5.1 전체 구조

```mermaid
flowchart TD
    A["차량 환경<br/>Pleos Connect"] --> B["Pleos Vehicle SDK<br/>AAOS Car API"]
    B --> C["차량 신호 수집 (10Hz)<br/>:pleos"]
    C --> D["샘플 검증"]
    D --> E["이벤트 판정·점수 계산<br/>:core"]
    E --> F["코칭 문장 생성<br/>:core"]
    F --> G["음성 안내 (TTS)<br/>:pleos"]
    F --> H["화면·리포트<br/>:app"]
```

### 5.2 주요 구성 요소

| 구성 요소 | 모듈 | 역할 |
| --- | --- | --- |
| 신호 수집 | `:pleos` | Pleos Vehicle SDK와 AAOS Car API에서 차량 신호를 받아 샘플로 묶음 |
| 주행 제한 감지 | `:pleos` | `CarUxRestrictions`로 주행 중 제한 상태 확인 |
| 이벤트 판정·점수 | `:core` | 급가속·급제동·급조향·방향지시등 누락 판정, 점수와 미션 판정 |
| 코칭 | `:core` | 측정 이벤트를 코칭 문장으로 변환, 음성 재생 간격 조절 |
| 음성 안내 | `:pleos` | Pleos TextToSpeech SDK(ON_DEVICE)로 코칭 문장 재생 |
| 화면·저장 | `:app` | Compose UI, 세션 기록 저장, 리포트와 통계 |

### 5.3 데이터 처리 흐름

1. **Data Collection:** 세션이 진행 중일 때만 차량 신호 리스너를 등록하고, 100ms마다 최신 값을 샘플로 묶습니다.
2. **Data Validation:** 시간 순서가 어긋난 샘플과 간격이 비정상인 구간은 판정에서 제외합니다.
3. **Driving Analysis:** 샘플에서 운전 행동 이벤트를 찾고 점수와 미션 결과를 계산합니다.
4. **Coaching:** 측정 이벤트를 코칭 문장으로 바꿔 연습 중에는 바로, 실제 주행은 정차 후에 안내합니다.
5. **Result Display:** 결과를 화면에 보여 주고 기기에 저장해 리포트와 통계로 제공합니다.

LLM으로 코칭 문장을 다듬는 기능은 Pleos 에뮬레이터 v2.1에서 LLM 추론 SDK 지원이 종료되어 이번 구현에서 제외했습니다.

## 6. 팀원 및 역할

TIREON은 총 3명의 팀원이 함께 개발하는 프로젝트입니다.

| 이름   | 담당 역할 |
| ------ | --------- |
| 박다윗 | 시연용 UI 개편(Pleos 디자인 가이드), 주행 기록 중 내비게이션 연동과 포그라운드 서비스, 연습·주행 음성 안내, 연습 미션 판정 개선, 제출 APK 빌드 |
| 이채린 | 공모전 기획서·제출 문서 작성, 코드 검토와 버그 수정(후진 판정·실시간 코칭 안내), 에뮬레이터 검증 |
| 전정완 | 프로젝트 구조·CI·릴리스 서명 구성, `:core` 분석 엔진, Pleos 신호 수집 계층, 연습·주행 세션, 리포트·통계·설정·온보딩, TTS 코칭 |

각 팀원은 담당 기능 개발뿐 아니라 코드 리뷰와 프로젝트 품질 관리에도 참여합니다.

## 7. 실행 방법

### 제출 APK와 테스트 환경

| 항목 | 내용 |
| --- | --- |
| APK 위치 | [`apk/TIREON_TIREON_release.apk`](apk/TIREON_TIREON_release.apk) |
| 빌드 | release, 팀 서명 키로 서명 (APK Signature Scheme v2) |
| 빌드 소스 | [`cd7ff86`](https://github.com/TIREON1/TIREON/commit/cd7ff8681402810112c1e107b8801ac20d101672), 주행 기록 시작·종료 및 저장 실패 음성 안내 포함 |
| 빌드 실행 | [Release #43390314931](https://github.com/TIREON1/TIREON/actions/runs/43390314931) |
| APK SHA-256 | `1e8dcd54aff85cb534bea5562257d1b61a2798f728b76db113a4e3e8fe702991` |
| 버전 | 0.1.0 (versionCode 1), 패키지 `ai.tireon.app` |
| 개발·테스트 에뮬레이터 | Pleos Connect Emulator v2.1.0 (Android 14 / API 34, arm64·x86_64 시스템 이미지) |
| 화면 규격 | 2560×1440, 160dpi |

에뮬레이터가 실행 중일 때 다음 명령으로 설치합니다.

```bash
adb install -r apk/TIREON_TIREON_release.apk
```

디버그 빌드가 설치되어 있으면 서명이 달라 설치가 거부됩니다. `adb uninstall ai.tireon.app`으로 먼저 지웁니다.

차량 신호(기어, 속도, 조향, 방향지시등)는 에뮬레이터의 Virtual Console에서 바꿀 수 있습니다. 앱은 차량이 정지했을 때만 조작 화면을 보여 주고, 주행 중에는 시스템이 화면을 가립니다.

### 7.1 개발 환경 준비

프로젝트 실행을 위해 다음 환경이 필요합니다.

- Android Studio
- JDK 21
- Kotlin
- Gradle
- Pleos Connect SDK
- Pleos Connect Emulator

### 7.2 프로젝트 가져오기

저장소를 로컬 환경에 복제합니다.

```bash
git clone https://github.com/TIREON1/TIREON.git

cd TIREON
```

Android Studio에서 프로젝트 폴더를 열고 Gradle 동기화를 진행합니다.

Android SDK 경로는 Android Studio가 자동으로 `local.properties`에 기록합니다. 이 파일은 커밋하지 않습니다.

### 7.3 SDK 및 개발 환경 설정

1. Android Studio에서 프로젝트를 엽니다.
2. JDK 21을 설정합니다.
3. 필요한 Pleos Connect SDK 및 개발 환경을 설정합니다.
4. Gradle Sync를 실행하여 의존성을 동기화합니다.
5. 필요한 경우 프로젝트에서 요구하는 로컬 설정을 진행합니다.

Pleos Connect SDK 아티팩트는 `ai.pleos.playground` 그룹으로 Pleos Nexus에서 받아옵니다. SDK를 사용하는 코드는 `:pleos` 모듈에만 두어 다른 모듈로 SDK 타입이 노출되지 않게 합니다.

차량 데이터 조회는 다음 순서를 따릅니다.

1. `Vehicle(context)` 생성 후 `initialize()`
2. `getXxx()`로 도메인 객체 획득
3. `checkXxxCapabilities`로 지원 여부 확인
4. 지원되는 경우에만 값을 조회
5. 화면 종료 시 `release()`

`Odometer` 조회에는 `pleos.car.permission.CAR_DRIVE` 권한이 필요하며, `:pleos` 모듈의 Manifest에 선언되어 있습니다. 이 권한은 런타임 승인 창이 아니라 Manifest 선언으로 부여됩니다.

### 7.4 애플리케이션 실행

1. Pleos Connect Emulator를 실행합니다.
2. Android Studio에서 프로젝트 실행 구성을 선택합니다.
3. 대상 에뮬레이터를 선택합니다.
4. Run을 실행하여 애플리케이션을 설치하고 실행합니다.

Pleos 시스템 이미지는 Android Studio SDK Manager에 Pleos 저장소를 추가해 설치합니다
([공식 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)).
Apple Silicon Mac은 arm64 이미지, Windows·Linux는 x86_64 이미지를 씁니다. AVD는 Cold Boot로
설정합니다. Quick Boot에서는 차량 신호(VHAL)가 동작하지 않습니다.

에뮬레이터는 GPU 가속을 켜고 실행합니다. AVD 이름은 각자 만든 이름으로 바꿉니다.

```bash
# macOS
~/Library/Android/sdk/emulator/emulator -avd Pleos_Connect_v2.1 \
  -no-snapshot-load -no-boot-anim -gpu host
```

```bash
# Windows (Git Bash)
"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd Pleos_Connect_v2.1.0 \
  -no-snapshot-load -no-boot-anim -gpu host
```

`-gpu swiftshader_indirect`(소프트웨어 렌더링)를 쓰면 차량 화면의 3D 렌더링이
CPU로 돌아 에뮬레이터가 느려집니다. 계측 테스트도 실패합니다. `-gpu host`를
사용하면 macOS에서는 Metal, Windows에서는 호스트 GPU 가속이 적용됩니다.

### 7.4.1 계측 테스트

계측 테스트는 Pleos 에뮬레이터에서만 실행됩니다. CI에서는 실행하지 않고 로컬에서 실행합니다.

```bash
# 에뮬레이터가 실행 중이어야 합니다
./gradlew :app:connectedDebugAndroidTest
```

CI는 계측 테스트를 실행하지 않고 컴파일만 검증합니다.

### 7.5 빌드

Gradle Wrapper를 사용하여 빌드합니다. JDK 21이 필요합니다.

```bash
# 디버그 APK
./gradlew assembleDebug

# 단위 테스트 및 Lint
./gradlew test lint

# 릴리스 APK (서명 설정 필요)
./gradlew assembleRelease
```

서명된 릴리스 APK는 `app/build/outputs/apk/release/app-release.apk`에 생성됩니다.

`./gradlew build`는 release 패키징을 포함하므로 서명 설정이 필요합니다. 서명 없이 전체 검증을 하려면 `./gradlew assembleDebug test lint`를 사용합니다.

### 7.6 릴리스 서명

릴리스 빌드는 저장소 루트의 `keystore.properties`를 사용합니다.

```bash
cp keystore.properties.example keystore.properties
```

| 항목 | 설명 |
| --- | --- |
| `storeFile` | 키스토어 파일의 절대 경로 (저장소 밖에 보관) |
| `storePassword` | 키스토어 비밀번호 |
| `keyAlias` | 키 별칭 |
| `keyPassword` | 키 비밀번호 |

`keystore.properties`와 키스토어 파일은 커밋하지 않습니다. 파일이 없거나 값이 비어 있으면 릴리스 빌드는 `verifyReleaseSigning` 단계에서 실패합니다.

CI의 릴리스 워크플로는 GitHub Secrets로 `keystore.properties`를 생성하므로 별도 로컬 설정이 필요하지 않습니다.

## 8. CI (Continuous Integration)

TIREON은 GitHub Actions를 활용하여 코드 변경 사항을 자동으로 검증합니다.

Pull Request 및 `main` 브랜치 변경 시 문서 검사를 실행하고, Android Gradle 프로젝트가 구성되면 빌드·테스트·Lint까지 자동으로 검증합니다.

### 8.1 CI 환경

| 항목             | 설정                |
| ---------------- | ------------------- |
| CI Platform      | GitHub Actions      |
| Build System     | Gradle              |
| Language         | Kotlin              |
| JDK              | 21                  |
| Trigger          | Pull Request / Push / Manual |
| Build Validation | 문서 검사 + Gradle Build (조건부) |

### 8.2 자동 검증 항목

- 문서 링크, 후행 공백, YAML 구문 검사 (항상 실행)
- Issue/PR 템플릿 YAML 유효성 검사
- Gradle 의존성 및 프로젝트 설정 확인 (`gradlew` 존재 시)
- Kotlin 코드 컴파일 및 단위 테스트 (`gradlew` 존재 시)
- Android 디버그 빌드 및 Lint (`gradlew` 존재 시)

CI는 서명 키를 사용하지 않으므로 `assembleRelease` 대신 `assembleDebug`로 검증합니다.

### 8.3 CI 실행 조건

| 이벤트       | 실행 조건                               |
| ------------ | --------------------------------------- |
| Pull Request | `main` 브랜치 대상으로 생성 및 업데이트 |
| Push         | `main` 브랜치에 변경 사항 반영          |
| Manual       | GitHub Actions에서 수동 실행            |

### 8.4 CI 결과 확인

1. GitHub 저장소의 **Actions** 탭에 접속합니다.
2. 실행된 Workflow를 선택합니다.
3. 빌드 및 검증 결과를 확인합니다.
4. 실패한 경우 로그를 확인하고 오류를 수정합니다.

`main` 브랜치 병합에는 `Docs & templates` 상태 체크 통과가 필요합니다.

CI는 코드의 기본적인 빌드 안정성을 검증하는 과정이며, 실제 차량 환경에서의 동작이나 기능적 정확성까지 보장하지는 않습니다.

## 9. 개발 및 협업

TIREON은 GitHub를 통해 소스 코드와 작업 내용을 관리합니다.

### Branch Strategy

| 브랜치       | 용도                 |
| ------------ | -------------------- |
| `main`       | 안정적인 통합 브랜치 |
| `feature/*`  | 신규 기능 개발       |
| `fix/*`      | 버그 수정            |
| `refactor/*` | 코드 구조 개선       |
| `docs/*`     | 문서 수정            |
| `chore/*`    | 빌드·설정·유지보수   |

### Commit Convention

커밋 메시지는 다음 형식을 사용합니다.

```text
type: 작업 내용
```

예시:

```text
feat: AI 운전 코치 화면 구현
fix: 차량 데이터 수신 오류 수정
refactor: 운전 점수 계산 로직 개선
docs: README 작성
```

### Pull Request

- `main` 브랜치에 직접 Push하지 않습니다.
- 작업 브랜치에서 개발한 후 Pull Request를 생성합니다.
- PR 병합에는 다른 팀원 1명의 승인이 필요합니다.
- AI가 생성하거나 수정한 코드도 작성자가 직접 검토합니다.
- 승인 이후 `main` 브랜치에 병합합니다.

자세한 협업 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고하세요.

---

## 10. 프로젝트 정보

| 항목          | 내용                             |
| ------------- | -------------------------------- |
| 프로젝트명    | TIREON (티레온)                  |
| 프로젝트 유형 | 차량용 애플리케이션              |
| 개발 플랫폼   | Pleos Connect                    |
| 주요 대상     | 초보 운전자                      |
| 핵심 기술     | 차량 신호 분석과 음성 운전 코칭 |
| 개발 인원     | 3명                              |

---

<p align="center">
  <b>TIREON</b>
</p>
