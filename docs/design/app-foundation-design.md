# TIREON 앱 기반 구조 설계

작성일: 2026-11-08
상태: 초안 (변경 가능)
관련 문서: [Pleos 신호 스파이크 결과](pleos-signal-spike.md)

> ## 이 문서는 변경될 수 있습니다
>
> TIREON은 3명이 함께 개발하는 프로젝트입니다. 이 문서는 초기 구조를 잡기 위한 합의안입니다.
>
> - 모든 팀원이 이 문서를 수정할 수 있습니다.
> - 설계를 바꾸려면 이 문서를 먼저 고치고, 관련 Issue에 이유를 남깁니다.
> - 구현이 이 문서와 달라졌다면 코드보다 이 문서를 먼저 갱신합니다.
> - 미확정 항목은 9장에 모아 두었습니다. 확인 결과에 따라 바뀔 수 있습니다.
>
> 특히 5장(판정 임계값)과 9장(미확인 항목)은 실측 데이터가 쌓이면 조정될 가능성이 높습니다.

---

## 1. 목적과 범위

### 1.1 목적

TIREON의 앱 셸과 구조를 세웁니다. 화면을 먼저 그리지 않고
안전 규정, 데이터 계약, 분석, 코칭 순서로 기반을 확정합니다.

### 1.2 범위

포함하는 것:

- 앱 셸 (액티비티 2개, 내비게이션, 테마)
- Pleos 신호 수집 계층 (`:pleos`)
- 분석과 점수 도메인 (`:core`)
- 연습 미션 5종, 주행 미션 4종 정의
- 룰 기반 코치와 TTS 음성 안내
- 세션 저장과 리포트

제외하는 것:

| 항목 | 제외 사유 |
|---|---|
| ADAS (전방 차량, 차선 인식) | 문서와 배포 아티팩트가 없음. API Compatibility상 Not supported |
| LLM 추론 SDK | 에뮬레이터 v2.1에서 지원 종료. Maven 아티팩트 없음 |
| Safety, Tire 도메인 | `PRIVATE_CAR_SAFETY` 권한은 일반 앱에 부여 불가 |
| 실시간 서버 동기화 | 네트워크가 끊겨도 핵심 기능이 유지되어야 함 |
| 계정과 프로필 | 1차 구현 범위 밖 |

### 1.3 성공 기준

1. 공터 연습 미션이 에뮬레이터에서 끝까지 동작한다.
2. 주행 후 리포트에 측정 근거가 있는 점수와 코칭이 나온다.
3. 네트워크를 끊어도 1번과 2번이 동작한다.
4. 주행 제한 상태에서 안전 규정을 위반하지 않는다.
5. `:core` 분석 로직이 JVM 단위 테스트로 검증된다.

---

## 2. 문서 근거와 제약

이 설계는 아래 공식 문서를 근거로 합니다. 구현 중 재확인이 필요한
항목은 9장에 따로 기록합니다.

| 주제 | 문서 | 확정 내용 |
|---|---|---|
| 속도 취득 경로 | [SDK API Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api) | 표준 데이터(속도, 기어, 연료)는 AAOS Car API로 접근 |
| 속도 가용성 | [API Compatibility](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability) | `PERF_VEHICLE_SPEED` O, `PERF_STEERING_ANGLE` X, `PERF_ODOMETER` X |
| 화면 모델 | [App Structure](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model) | 멀티윈도우 구조. Driving view는 앱이 숨길 수 없음. portrait와 landscape 모두 설계 |
| 주행 제한 | [Driving Restriction Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy) | DO 미선언 앱은 Drive 상태에서 사용 불가 |
| 제한 연동 | [Driving Restriction Development Guide](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide) | DO 선언 앱은 `CarUxRestrictions`를 직접 구독해 기능 제한 |
| 주의 분산 | [Driver Distraction](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline) | 큰 터치 타깃, 애니메이션 최소화, 텍스트 입력 지양 |
| TTS | [TextToSpeech SDK](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro) | `ON_DEVICE` 모드는 네트워크와 앱 등록이 불필요 |
| TTS 언어 | [Set locale](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale) | `ko-KR` 정식 지원 |
| TTS 권한 | [Declaring permissions](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/declaring-permissions) | `pleos.car.permission.TTS_SERVICE` |

### 2.1 신호 소스

스파이크 실측으로 확정된 소스만 사용합니다.

| 신호 | 소스 | 단위 | 비고 |
|---|---|---|---|
| 속도 | AAOS `PERF_VEHICLE_SPEED` | m/s | 실측 5~10Hz, 최소 간격 100ms |
| 기어 | Pleos `Gear` | enum | P, R, N, D |
| 누적 거리 | Pleos `Odometer` | km | 차분해서 사용 |
| 브레이크 페달 | Pleos `Brake` | boolean | 급제동 이중 확인용 |
| 조향각 | Pleos `SteeringWheel` | degree | 부호가 반전되어 들어옴 |
| 방향지시등 | Pleos `TurnSignal` | enum | OFF, LEFT, RIGHT |
| 배터리 | Pleos `EvBattery` | % | 참고 정보 |

---

## 3. 안전 모델

### 3.1 액티비티 분리

DO(Distraction Optimization)는 앱 전체가 아니라 액티비티 단위로 선언합니다.

| 액티비티 | `distractionOptimized` | 용도 |
|---|---|---|
| `MainActivity` | 선언 안 함 | 홈, 미션, 리포트, 설정. 주행 중에는 시스템이 차단 |
| `PracticeActivity` | `true` | 연습 세션. 주행 제한 중에도 유지 |

```xml
<activity
    android:name="ai.tireon.app.practice.PracticeActivity"
    android:exported="false">
    <meta-data
        android:name="distractionOptimized"
        android:value="true" />
</activity>
```

`MainActivity`는 DO를 선언하지 않으므로 주행 제한이 걸리면 플랫폼이
자동으로 가립니다. 앱이 차단 로직을 따로 구현하지 않습니다.

### 3.2 제한 판정

속도로 UI를 판정하지 않습니다. 지역과 차량별 규정은 플랫폼이 결정합니다.

```kotlin
// PracticeActivity 내부
uxRestrictionsManager.registerListener { restrictions ->
    val minimal = restrictions.isRequiresDistractionOptimization
    // minimal = false 이면 전체 UI
    // minimal = true 이면 숫자 1개만, 터치 요소 없음
}
```

속도는 표시용으로만 사용합니다.

### 3.3 PracticeActivity 상태

```
[준비]  제한 해제  → 미션 설명과 시작 버튼
   ↓ 시작
[진행]  제한 활성  → 숫자 1개와 음성, 터치 불가
   ↓ 정지
[요약]  제한 해제  → 결과 요약과 저장
```

제한이 해제되지 않으면 요약 화면을 띄우지 않습니다. 차가 움직이는 동안
결과를 보여주지 않는다는 뜻입니다.

### 3.4 백그라운드 정책

| 작업 | 백그라운드 진입 시 |
|---|---|
| 신호 수집 | 계속 (세션 진행 중일 때만) |
| TTS | `stop()` |
| UI 갱신 | 중단 |
| 세션 저장 | 이벤트 발생 시, 그리고 5초 주기 |

세션이 없으면 수집도 하지 않습니다. 불필요한 리스너를 남기지 않습니다.

---

## 4. 화면 구조

### 4.1 내비게이션

`MainActivity`는 Compose `NavHost`로 네 개의 목적지를 가집니다.

```
home       오늘의 점수, 추천 미션, 최근 세션
practice   미션 목록과 상세
report     세션 기록 목록, 리포트 상세
settings   프로필, 차량 정보, 안전 고지
```

`PracticeActivity`는 별도 액티비티입니다. `MainActivity`에서 인텐트로 진입합니다.

### 4.2 반응형 레이아웃

`screenOrientation`을 지정하지 않으면 portrait 1윈도우가 기본입니다.
landscape에서는 2윈도우로 확장될 수 있으므로 둘 다 설계합니다.

| 화면 | portrait | landscape |
|---|---|---|
| 홈 | 점수, 미션, 탭 순서로 세로 배치 | NavigationRail에 점수와 미션 상세를 2분할 |
| 미션 목록 | 세로 목록 | 2열 그리드 |
| 리포트 | 카드 세로 스택 | 좌측 목록, 우측 상세 |
| 연습 진행 | 숫자 1개 | 숫자 1개 (동일) |

`WindowSizeClass`로 분기합니다. 창 크기가 바뀌어도 레이아웃만 바뀌고
상태는 유지되어야 합니다.

### 4.3 타이포그래피와 터치

- 주행 중 노출 화면은 숫자 1개만 표시합니다. 텍스트, 아이콘, 목록은 두지 않습니다.
- 정차 화면도 터치 타깃을 크게 잡습니다. 정확한 dp는 구현 전에
  [Pleos Connect UI Design Guide](https://document.pleos.ai/en/docs/connect/guide/docs-design/Introduction/intro)를
  다시 확인합니다 (9장 미확인 항목).
- 화면 전환 외에는 애니메이션을 쓰지 않습니다.

---

## 5. 미션과 점수 모델

### 5.1 연습 미션 (공터, 음성 중심)

| ID | 미션 | 성공 조건 | 측정 신호 |
|---|---|---|---|
| `SMOOTH_START` | 부드러운 출발 | 급가속 0회 | 가속도 |
| `SMOOTH_STOP` | 부드러운 정지 | 급제동 0회 | 가속도와 브레이크 |
| `REVERSE_SENSE` | 후진 감각 | R 기어 3초 이상이고 조향 입력 있음 | 기어, 조향 |
| `TURN_SIGNAL` | 깜빡이 습관 | 조향 전 점등 누락 0회 | 방향지시등, 조향 |
| `STEADY_SPEED` | 정속 유지 | 15~25km/h 유지, 표준편차 3km/h 미만 | 속도 |

### 5.2 주행 미션 (도로, 무음 수집 후 리포트)

| ID | 미션 | 표시 지표 |
|---|---|---|
| `SMOOTH_DRIVING` | 부드러운 가감속 | 급가속과 급제동 횟수 |
| `SIGNAL_HABIT` | 방향지시등 습관 | 누락 횟수 |
| `STABLE_STEERING` | 급조향 방지 | 급조향 횟수 |
| `OVERALL` | 종합 점수 | 위 항목 가중 합산 |

주행 미션은 진행 중 UI를 노출하지 않습니다. 연습 미션도 제한이 활성이면
마찬가지로 숫자 1개만 표시합니다.

### 5.3 이벤트 판정

| 이벤트 | 조건 | 성격 |
|---|---|---|
| `HARSH_ACCEL` | 가속도가 +2.94 m/s² 이상으로 500ms 지속 | 상수, 업계 통용 0.3g |
| `HARSH_BRAKE` | 가속도가 -2.94 m/s² 이하로 500ms 지속하면서 브레이크 ON | 이중 확인으로 오탐 억제 |
| `HARSH_STEER` | 조향 각속도가 200°/s를 넘고 속도가 20km/h 초과 | 휴리스틱, 실측 후 조정 |
| `MISSING_TURN_SIGNAL` | 조향이 30° 이상 변하는 동안 `TurnSignal`이 OFF | 휴리스틱 |

임계값은 `Thresholds` 객체 하나에 모읍니다. 미션별 분기는 데이터가
쌓인 뒤에 판단합니다.

이벤트는 동작 한 번을 단위로 셉니다. 같은 급조향이 100ms 샘플 여러 개에
걸쳐 나타나도 1회이고, 회전 한 번에서 방향지시등 누락도 1회입니다.
이벤트 수가 그대로 감점이 되므로 중복 계산은 점수를 왜곡합니다.

### 5.4 샘플 유효성

속도 콜백 간격은 실측 100~200ms였습니다. 튈 수 있으므로 계산 전에 거릅니다.

| 조건 | 처리 |
|---|---|
| `dt < 50ms` | 중복 샘플로 보고 건너뜀. 구간은 끊지 않음 |
| `dt > 500ms` | 공백 구간. 가속도 계산에서 제외 |
| `speed == null` | 그 시점부터 판정하지 않음 |
| 유효 샘플 20개 미만 | `Insufficient` |

`dt`는 타임스탬프 차분으로 계산합니다. `getCarPropertyConfig()`가
null을 반환해서 선언된 샘플레이트를 신뢰할 수 없기 때문입니다.

노이즈 억제를 위해 가속도에 5샘플 이동평균을 적용합니다. 다만 평균 창이
차기 전에도 확보된 쌍만으로 판정합니다. 창이 찰 때까지 기다리면 500ms
지속 판정이 실제로는 더 길어져 이벤트를 놓칩니다.

### 5.5 점수

```
valid  = 거리 ≥ 0.5km AND 시간 ≥ 60s AND 유효 샘플 ≥ 20
rate   = 이벤트 수 / max(거리km, 1.0)
score  = clamp(100 - Σ(가중치 × rate), 0, 100)
```

| 이벤트 | 가중치 |
|---|---|
| 급제동 | 8 |
| 급가속 | 6 |
| 급조향 | 5 |
| 깜빡이 누락 | 4 |

데이터가 부족하면 점수를 만들지 않습니다. `Insufficient`로 표시해서
0점과 구분합니다. 0점은 측정했더니 나쁘다는 뜻이고,
`Insufficient`는 측정할 수 없다는 뜻입니다.

---

## 6. 모듈 구조

### 6.1 의존 방향

```
:app  ──→ :core
  │
  └────→ :pleos ──→ :core
```

역방향 의존은 없습니다. `:pleos`는 SDK 타입을 밖으로 노출하지 않습니다.

| 모듈 | 성격 | 책임 |
|---|---|---|
| `:core` | 순수 Kotlin JVM | 분석, 점수, 미션 정의, 코치 문장, 저장 인터페이스 |
| `:pleos` | Android Library | Vehicle SDK, AAOS `CarPropertyManager`, TTS 어댑터 |
| `:app` | Android App | Compose UI, ViewModel, 저장 구현, DI |

### 6.2 `:core` 공개 타입

```kotlin
data class DrivingSample(
    val timestampMs: Long,
    val speedMps: Float?,
    val steeringDeg: Float?,
    val brakePressed: Boolean?,
    val turnSignal: TurnSignal?,
    val gear: Gear?,
)

enum class DrivingEventType {
    HARSH_ACCEL, HARSH_BRAKE, HARSH_STEER, MISSING_TURN_SIGNAL,
}

data class DrivingEvent(
    val type: DrivingEventType,
    val atMs: Long,
    val value: Float,
    val confidence: Confidence,   // MEASURED, ESTIMATED
)

sealed interface ScoreResult {
    data class Scored(
        val score: DrivingScore,
        val breakdown: List<EventContribution>,
    ) : ScoreResult

    data class Insufficient(val reason: InsufficientReason) : ScoreResult
}

enum class InsufficientReason {
    DISTANCE_TOO_SHORT,    // 거리 < 0.5km
    DURATION_TOO_SHORT,    // 시간 < 60s
    NOT_ENOUGH_SAMPLES,    // 유효 샘플 < 20
}

data class EventContribution(
    val type: DrivingEventType,
    val count: Int,
    val penalty: Int,
)
```

`Insufficient`의 사유를 나눈 이유는 UI 문구가 달라야 하기 때문입니다.
"조금 더 길게 주행해 주세요"와 "차량 신호를 받지 못했어요"는 사용자가
취할 행동이 다릅니다.

`speedMps`가 nullable인 것이 이 계약의 핵심입니다. 측정 불가를 `0f`로
대체하지 않습니다. 0은 유효한 측정값이라 구분이 사라집니다.

### 6.3 `:pleos` 공개 타입

```kotlin
interface PleosSignalSource {
    fun samples(sessionActive: Boolean): Flow<DrivingSample>
}

interface CoachSpeaker {
    fun speak(text: String)
    fun stop()
}
```

SDK 타입은 반환하지 않습니다. `DrivingSample`은 `:core` 타입입니다.

### 6.4 기존 코드 정리

`DrivingViewModel`이 현재 `:pleos`에 있습니다. ViewModel은 UI 관심사이므로
`:app`으로 옮깁니다.

이동 시점은 앱 셸 단계입니다. 분석 계층은 SDK와 UI에 의존하지 않아 먼저
완성할 수 있습니다. 반대로 ViewModel을 옮기려면 `:app` 화면 구조가 먼저
정해져야 합니다. 분석 계층 작업에서 이 항목을 건드리지 않는 이유입니다.

그 외 기존 코드는 건드리지 않습니다.

### 6.5 저장

`kotlinx.serialization` 기반 JSON 파일 저장으로 시작합니다.

```kotlin
interface SessionRepository {
    suspend fun save(session: DrivingSession)
    suspend fun recent(limit: Int): List<DrivingSession>
    suspend fun find(id: String): DrivingSession?
}
```

Room은 1차 범위에서 과합니다. 세션 수가 늘면 그때 검토합니다.

---

## 7. AI 코치 계층

### 7.1 2단 구조

```
[측정 이벤트 :core]
      ↓
[룰 엔진 :core]          결정론적 문장 생성
      ↓  (선택)
[LLM 다듬기 :app]        실패 시 룰 문장 그대로 사용
      ↓
[TTS ON_DEVICE :pleos]   ko-KR
```

LLM이 없어도 앱은 완전히 동작합니다. LLM은 문장 품질을 올리는 선택
경로입니다.

### 7.2 코치 메시지

```kotlin
data class CoachMessage(
    val text: String,
    val severity: Severity,
    val evidence: String?,   // "감속도 -3.2 m/s² (측정)"
)
```

`evidence`를 항상 붙입니다. 측정값과 해석을 UI에서 분리해 보여주기
위함입니다.

문장 예시:

| 상황 | 문장 | 근거 |
|---|---|---|
| 급제동 1회 | "급제동이 1번 있었어요. 브레이크를 조금 더 미리 밟아보세요." | 감속도 -3.2 m/s² |
| 급가속 0회 | "출발이 아주 부드러웠어요." | 최대 가속 +1.8 m/s² |
| 깜빡이 누락 2회 | "방향지시등 없이 핸들을 돌린 구간이 2번 있었어요." | 조향 42° 변화 |
| 점수 85 | "오늘은 85점이에요. 급제동만 줄이면 90점대예요." | 없음 |

감점이 0으로 반올림된 항목은 개선 제안에서 뺍니다. 100점인데
"급제동을 줄이면 좋아져요"라고 하면 앞뒤가 맞지 않습니다.

### 7.3 LLM 가드레일

| 규칙 | 구현 |
|---|---|
| 타임아웃 | 3초. 넘으면 룰 문장 사용 |
| 숫자 불변 | 응답의 숫자가 원본에 없으면 응답을 버림 |
| 오프라인 | 네트워크가 없으면 시도하지 않음 |
| 비밀키 | `local.properties`에서 읽고 코드와 저장소에는 남기지 않음 |

숫자 불변 검증이 핵심입니다. LLM이 없는 측정값을 만들어내면 차량 안전
앱에서는 허위 정보가 됩니다.

### 7.4 TTS

```kotlin
val tts = TextToSpeech(context, Mode.ON_DEVICE)  // 오프라인
tts.initialize()
tts.setLocale(Locale.KOREA)
tts.speak(message.text)
```

| 상황 | 정책 |
|---|---|
| 연습 중 이벤트 | 즉시 재생 |
| 리포트 진입 | 요약 3문장을 순차 재생 |
| 연속 이벤트 | 최소 5초 간격 |
| 백그라운드나 화면 전환 | `stop()` |
| TTS 실패 | 무시. 화면이 주 경로 |

권한은 `pleos.car.permission.TTS_SERVICE`입니다.

---

## 8. 테스트 전략

### 8.1 `:core` 단위 테스트 (JVM)

| 대상 | 검증 |
|---|---|
| `EventDetector` | 합성 샘플 시퀀스로 각 이벤트 검출 |
| `EventDetector` 경계 | `dt` 50ms 미만 무시, 500ms 초과 제외 |
| `EventDetector` 결측 | `speedMps`가 null인 구간에서 이벤트를 만들지 않음 |
| `EventDetector` 회귀 | 주차 중 오탐, 한 동작의 중복 계산, 샘플 지터 |
| `ScoreCalculator` | 가중치와 rate 계산, clamp |
| `ScoreCalculator` | `Insufficient` 조건 3종과 경계값 |
| `ScoreCalculator` | NaN, 무한대, 음수 거리 |
| `CoachMessages` | 이벤트별 문장과 evidence 생성 |
| `CoachMessages` | 감점 0 항목 제외 |
| `CoachRefiner` 가드 | 숫자 불변 위반 응답 폐기 |

합성 데이터로 검증하므로 에뮬레이터 없이 빠르게 돌릴 수 있습니다.

### 8.2 에뮬레이터 검증

| 항목 | 방법 |
|---|---|
| DO 태그 동작 | 주행 상태에서 `MainActivity`가 차단되고 `PracticeActivity`가 유지되는지 확인 |
| `CarUxRestrictions` 수신 | 리스너 콜백 로그 |
| 신호 수집 | Virtual Console 주입과 logcat |
| GPS, TTS | 각각 실제 호출 |

### 8.3 빌드

```bash
./gradlew assembleDebug test lint
```

---

## 9. 미확인 항목

구현 착수 전이나 1단계에서 반드시 확인합니다. 확인하기 전에는 해당
기능을 완성으로 보고하지 않습니다.

| 항목 | 확인 시점 | 실패 시 대응 |
|---|---|---|
| 에뮬레이터에서 DO 태그가 주행 차단을 통과하는가 | 1단계 | userdebug 이미지 조건 재확인. 통과하지 못하면 연습 화면을 주차 전용으로 축소 |
| `CarUxRestrictions` 값이 에뮬레이터에서 오는가 | 1단계 | 기어와 속도 기반 보수적 폴백. 문서상 권장 경로가 아님을 함께 기록 |
| TTS ON_DEVICE 한국어 품질 | 2단계 | 화면 문장을 주 경로로 유지 |
| Pleos UI 가이드의 정확한 터치 타깃 dp | 구현 전 | 문서 재확인 후 상수 확정 |
| `PERF_VEHICLE_SPEED`가 실차에서도 O인지 | 실차 확보 시 | `checkCapabilities`로 감지해서 값 없음 처리 |
| `CAR_POWERTRAIN` 권한 심사 승인 여부 | 마켓 제출 시 | GPS 미분으로 폴백 |
| 조향각 부호 반전이 실차에서도 같은지 | 실차 확보 시 | 절댓값 임계와 방향 정규화를 유지 |

---

## 10. 구현 순서

1. 신호 계층: AAOS 속도 리더, Pleos 어댑터, `PleosSignalSource`
2. 분석 계층: `EventDetector`, `ScoreCalculator`와 단위 테스트
3. 코치 계층: `CoachMessages`, TTS 래퍼
4. 앱 셸: `MainActivity`, 내비게이션, 테마, 반응형 레이아웃
5. 연습 세션: `PracticeActivity`, DO 선언, `CarUxRestrictions`
6. 리포트: 저장소, 리포트 화면
7. LLM 연동 (선택): `CoachRefiner`

1단계와 2단계에서 9장의 상위 두 항목을 함께 검증합니다.
