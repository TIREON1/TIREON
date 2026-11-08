# TIREON 초기 단계 (`:core` 분석·코치 기반) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 주행 샘플에서 이벤트를 검출하고 점수와 코칭 문장을 만드는 순수 Kotlin 기반을 세운다.

**Architecture:** `:core`는 Android 의존이 없는 Kotlin JVM 모듈이다. 입력은 `List<DrivingSample>`, 출력은 `List<DrivingEvent>`와 `ScoreResult`다. 모든 판정은 순수 함수이며 합성 데이터로 단위 테스트한다. 이 단계에서는 SDK·UI·저장을 건드리지 않는다.

**Tech Stack:** Kotlin 2.3.21, JVM 21, JUnit 4, kotlin-test

**Spec:** `docs/design/app-foundation-design.md` (5장 미션·점수 모델, 6.2장 `:core` 공개 타입, 7.2장 코치 메시지)

## Global Constraints

- `:core`는 순수 Kotlin JVM 모듈이다. Android·Pleos SDK 의존을 추가하지 않는다.
- 측정 불가는 `null`로 표현한다. `0f`로 대체하지 않는다.
- 모든 임계값은 `Thresholds` 한 파일에만 둔다. 다른 파일에 숫자를 하드코딩하지 않는다.
- `:pleos`가 아닌 모듈로 SDK 타입을 노출하지 않는다. 이 단계에서는 SDK를 쓰지 않는다.
- 주석은 코드가 이미 말하는 내용을 반복하지 않는다. 비자명한 결정에만 단다.
- 커밋 메시지는 `type: 한국어 내용` 형식을 따른다 (`feat`, `test`, `refactor`).
- `AGENTS.md`, `.omo/` 등 AI 설정 파일을 커밋하지 않는다.
- 검증 명령: `./gradlew :core:test`

## Review Focus

이 단계 코드가 실제로 받게 될 입력 중, 테스트가 없으면 조용히 틀릴 것들이다.

1. **속도 샘플이 `null`인 구간** — 측정 불가를 0으로 읽으면 감속도가 거대한 급제동으로 둔갑한다. `null` 구간은 계산에서 제외되어야 한다.
2. **타임스탬프가 뒤로 가거나 중복인 샘플** — `dt ≤ 0`이면 나눗셈이 무한대·NaN이 된다. 이벤트가 생기면 안 된다.
3. **샘플 간격이 비정상인 경우** — 30ms 중복 샘플은 가속도를 부풀리고, 2초 공백은 실제 감속을 놓친다. 두 경우 모두 계산에서 빠져야 한다.
4. **경계값 근처** — 임계값과 정확히 같은 값(2.94 m/s², 200°/s)에서 이벤트가 생길지 말지가 구현마다 달라지면 점수가 재현되지 않는다. 문서의 판정을 코드와 테스트로 고정한다.
5. **데이터 부족과 0점의 혼동** — 짧은 세션에 0점을 주면 사용자는 "내 운전이 최악"으로 읽는다. 부족은 `Insufficient`로 분리되어야 한다.

---

### Task 1: 신호·이벤트 모델, 임계값, 가속도 이벤트 검출

**Files:**
- Create: `core/src/main/kotlin/ai/tireon/core/driving/DrivingSample.kt`
- Create: `core/src/main/kotlin/ai/tireon/core/driving/DrivingEvent.kt`
- Create: `core/src/main/kotlin/ai/tireon/core/analysis/Thresholds.kt`
- Create: `core/src/main/kotlin/ai/tireon/core/analysis/EventDetector.kt`
- Test: `core/src/test/kotlin/ai/tireon/core/analysis/EventDetectorAccelerationTest.kt`

**Interfaces:**
- Consumes: 없음 (모듈 최초 도입)
- Produces:
  - `data class DrivingSample(timestampMs: Long, speedMps: Float?, steeringDeg: Float?, brakePressed: Boolean?, turnSignal: TurnSignal?, gear: Gear?)`
  - `enum class TurnSignal { OFF, LEFT, RIGHT }`
  - `enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }`
  - `enum class Confidence { MEASURED, ESTIMATED }`
  - `enum class DrivingEventType { HARSH_ACCEL, HARSH_BRAKE, HARSH_STEER, MISSING_TURN_SIGNAL }`
  - `data class DrivingEvent(type: DrivingEventType, atMs: Long, value: Float, confidence: Confidence)`
  - `object Thresholds` (아래 상수)
  - `class EventDetector { fun detect(samples: List<DrivingSample>): List<DrivingEvent> }`

- [ ] **Step 1: 모델과 임계값 파일을 만든다**

`driving/DrivingSample.kt`:

```kotlin
package ai.tireon.core.driving

data class DrivingSample(
    val timestampMs: Long,
    val speedMps: Float?,
    val steeringDeg: Float?,
    val brakePressed: Boolean?,
    val turnSignal: TurnSignal?,
    val gear: Gear?,
)

enum class TurnSignal { OFF, LEFT, RIGHT }

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }
```

`driving/DrivingEvent.kt`:

```kotlin
package ai.tireon.core.driving

enum class DrivingEventType {
    HARSH_ACCEL,
    HARSH_BRAKE,
    HARSH_STEER,
    MISSING_TURN_SIGNAL,
}

enum class Confidence { MEASURED, ESTIMATED }

data class DrivingEvent(
    val type: DrivingEventType,
    val atMs: Long,
    val value: Float,
    val confidence: Confidence,
)
```

`analysis/Thresholds.kt`:

```kotlin
package ai.tireon.core.analysis

object Thresholds {
    const val HARSH_ACCEL_MPS2 = 2.94f
    const val HARSH_BRAKE_MPS2 = -2.94f
    const val HARSH_STEER_DEG_PER_SEC = 200f
    const val HARSH_STEER_MIN_SPEED_MPS = 5.56f
    const val MISSING_SIGNAL_STEER_DEG = 30f
    const val EVENT_MIN_DURATION_MS = 500L
    const val MIN_SAMPLE_GAP_MS = 50L
    const val MAX_SAMPLE_GAP_MS = 500L
    const val SMOOTHING_WINDOW = 5
    const val VALID_MIN_DISTANCE_KM = 0.5f
    const val VALID_MIN_DURATION_SEC = 60
    const val VALID_MIN_SAMPLES = 20
    const val W_HARSH_BRAKE = 8
    const val W_HARSH_ACCEL = 6
    const val W_HARSH_STEER = 5
    const val W_MISSING_SIGNAL = 4
}
```

- [ ] **Step 2: 가속도 검출 테스트를 쓴다 (실패 예정)**

`EventDetectorAccelerationTest.kt`:

```kotlin
package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventDetectorAccelerationTest {

    private val detector = EventDetector()

    /** stepMs 간격으로 일정 가속도를 만드는 샘플 목록. */
    private fun accelerating(
        startMps: Float,
        accelMps2: Float,
        steps: Int,
        stepMs: Long = 100L,
        brake: Boolean = false,
    ): List<DrivingSample> {
        val dtSec = stepMs / 1000f
        return (0 until steps).map { i ->
            sample(
                atMs = i * stepMs,
                speed = startMps + accelMps2 * dtSec * i,
                brake = brake,
            )
        }
    }

    private fun sample(
        atMs: Long,
        speed: Float?,
        steering: Float? = 0f,
        brake: Boolean? = false,
        signal: TurnSignal? = TurnSignal.OFF,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = brake,
        turnSignal = signal,
        gear = Gear.DRIVE,
    )

    @Test
    fun `임계값을 넘는 가속이 500ms 지속되면 급가속으로 검출한다`() {
        val events = detector.detect(accelerating(startMps = 10f, accelMps2 = 3.5f, steps = 51))

        assertEquals(1, events.count { it.type == DrivingEventType.HARSH_ACCEL })
    }

    @Test
    fun `임계값 미만 가속은 검출하지 않는다`() {
        val events = detector.detect(accelerating(startMps = 10f, accelMps2 = 2.9f, steps = 21))

        assertTrue(events.none { it.type == DrivingEventType.HARSH_ACCEL })
    }

    @Test
    fun `임계값을 살짝 넘는 가속은 검출한다`() {
        val events = detector.detect(accelerating(startMps = 10f, accelMps2 = 3.0f, steps = 21))

        assertEquals(1, events.count { it.type == DrivingEventType.HARSH_ACCEL })
    }

    @Test
    fun `500ms에 못 미치면 급가속으로 검출하지 않는다`() {
        val events = detector.detect(accelerating(startMps = 10f, accelMps2 = 3.5f, steps = 5))

        assertTrue(events.none { it.type == DrivingEventType.HARSH_ACCEL })
    }

    @Test
    fun `브레이크를 밟은 감속만 급제동으로 검출한다`() {
        val withBrake = detector.detect(
            accelerating(startMps = 20f, accelMps2 = -3.5f, steps = 51, brake = true),
        )
        val withoutBrake = detector.detect(
            accelerating(startMps = 20f, accelMps2 = -3.5f, steps = 51, brake = false),
        )

        assertEquals(1, withBrake.count { it.type == DrivingEventType.HARSH_BRAKE })
        assertTrue(withoutBrake.none { it.type == DrivingEventType.HARSH_BRAKE })
    }

    @Test
    fun `가속도가 음수여도 급가속으로 기록하지 않는다`() {
        val events = detector.detect(
            accelerating(startMps = 20f, accelMps2 = -3.5f, steps = 51, brake = true),
        )

        assertTrue(events.none { it.type == DrivingEventType.HARSH_ACCEL })
    }

    @Test
    fun `샘플 간격이 50ms 미만이면 계산하지 않는다`() {
        val events = detector.detect(
            accelerating(startMps = 10f, accelMps2 = 3.5f, steps = 40, stepMs = 30L),
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun `샘플 간격이 500ms를 넘으면 계산하지 않는다`() {
        val events = detector.detect(
            accelerating(startMps = 10f, accelMps2 = 3.5f, steps = 10, stepMs = 800L),
        )

        assertTrue(events.isEmpty())
    }

    @Test
    fun `속도가 없는 구간에서는 이벤트를 만들지 않는다`() {
        val samples = (0 until 21).map { sample(atMs = it * 100L, speed = null) }

        assertTrue(detector.detect(samples).isEmpty())
    }

    @Test
    fun `타임스탬프가 중복이면 이벤트를 만들지 않는다`() {
        val samples = (0 until 21).map { sample(atMs = 0L, speed = 10f + it) }

        assertTrue(detector.detect(samples).isEmpty())
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run: `./gradlew :core:test --tests '*EventDetectorAccelerationTest*'`
Expected: FAIL — `EventDetector` 미정의로 컴파일 실패

- [ ] **Step 4: `EventDetector`의 가속도 검출을 구현한다**

`EventDetector.kt`. 반환은 `List<DrivingEvent>`이고, 이 단계에서는 가속도만 판정한다. 조향·방향지시등은 Task 2에서 추가한다.

구현 방향:
- 인접 샘플 쌍에서 `dt = ts[i] - ts[i-1]`을 구하고, `dt`가 `MIN_SAMPLE_GAP_MS..MAX_SAMPLE_GAP_MS` 밖이면 건너뛴다.
- 두 샘플의 `speedMps`가 모두 null이면 건너뛴다.
- `accel = (v2 - v1) / (dt / 1000f)`를 계산하고 최근 `SMOOTHING_WINDOW`개 평균을 쓴다.
- 평균 가속도가 `HARSH_ACCEL_MPS2` 초과인 구간이 `EVENT_MIN_DURATION_MS` 이상 이어지면 `HARSH_ACCEL`, `HARSH_BRAKE_MPS2` 미만이면 `HARSH_BRAKE`로 기록한다.
- `HARSH_BRAKE`는 그 구간의 샘플 중 `brakePressed == true`가 하나라도 있어야 한다.
- 이벤트의 `atMs`는 구간 시작 시각, `value`는 구간 내 최대 절댓값 가속도, `confidence`는 `MEASURED`.
- 한 번 이벤트를 기록하면 그 구간을 종료하고 다음 구간부터 다시 센다.

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :core:test --tests '*EventDetectorAccelerationTest*'`
Expected: PASS (10 tests)

- [ ] **Step 6: 커밋**

```bash
git add core/src/main/kotlin/ai/tireon/core/driving core/src/main/kotlin/ai/tireon/core/analysis core/src/test/kotlin/ai/tireon/core/analysis
git commit -m "feat: 주행 샘플 모델과 가속도 이벤트 검출 추가"
```

---

### Task 2: 조향·방향지시등 이벤트 검출

**Files:**
- Modify: `core/src/main/kotlin/ai/tireon/core/analysis/EventDetector.kt`
- Test: `core/src/test/kotlin/ai/tireon/core/analysis/EventDetectorSteeringTest.kt`

**Interfaces:**
- Consumes: Task 1의 `DrivingSample`, `DrivingEvent`, `Thresholds`
- Produces: `EventDetector.detect`가 `HARSH_STEER`, `MISSING_TURN_SIGNAL`도 반환

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

`EventDetectorSteeringTest.kt`:

```kotlin
package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventDetectorSteeringTest {

    private val detector = EventDetector()

    private fun sample(
        atMs: Long,
        steering: Float?,
        speed: Float? = 20f,
        signal: TurnSignal? = TurnSignal.OFF,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = false,
        turnSignal = signal,
        gear = Gear.DRIVE,
    )

    /** 100ms 간격으로 250도/초의 조향 변화를 만든다. */
    private fun steeringAt(degPerSec: Float): List<DrivingSample> {
        val stepDeg = degPerSec * 0.1f
        return (0 until 5).map { i -> sample(atMs = i * 100L, steering = stepDeg * i) }
    }

    @Test
    fun `속도가 충분할 때 빠른 조향 변화를 급조향으로 검출한다`() {
        assertEquals(
            1,
            detector.detect(steeringAt(250f)).count { it.type == DrivingEventType.HARSH_STEER },
        )
    }

    @Test
    fun `임계값 미만의 조향 변화는 급조향으로 검출하지 않는다`() {
        assertTrue(
            detector.detect(steeringAt(150f)).none { it.type == DrivingEventType.HARSH_STEER },
        )
    }

    @Test
    fun `저속에서는 급조향으로 검출하지 않는다`() {
        val samples = (0 until 5).map { i ->
            sample(atMs = i * 100L, steering = 25f * i, speed = 3f)
        }

        assertTrue(detector.detect(samples).none { it.type == DrivingEventType.HARSH_STEER })
    }

    @Test
    fun `속도를 모르면 급조향을 판정하지 않는다`() {
        val samples = (0 until 5).map { i ->
            sample(atMs = i * 100L, steering = 25f * i, speed = null)
        }

        assertTrue(detector.detect(samples).none { it.type == DrivingEventType.HARSH_STEER })
    }

    @Test
    fun `방향지시등 없이 30도 이상 조향하면 누락으로 검출한다`() {
        val samples = listOf(0f, 20f, 40f)
            .mapIndexed { i, deg -> sample(atMs = i * 1000L, steering = deg, signal = TurnSignal.OFF) }

        assertEquals(
            1,
            detector.detect(samples).count { it.type == DrivingEventType.MISSING_TURN_SIGNAL },
        )
    }

    @Test
    fun `방향지시등을 켠 조향은 누락으로 검출하지 않는다`() {
        val samples = listOf(0f, 20f, 40f)
            .mapIndexed { i, deg -> sample(atMs = i * 1000L, steering = deg, signal = TurnSignal.LEFT) }

        assertTrue(detector.detect(samples).none { it.type == DrivingEventType.MISSING_TURN_SIGNAL })
    }

    @Test
    fun `조향이 30도 미만이면 누락으로 검출하지 않는다`() {
        val samples = listOf(0f, 10f, 20f)
            .mapIndexed { i, deg -> sample(atMs = i * 1000L, steering = deg, signal = TurnSignal.OFF) }

        assertTrue(detector.detect(samples).none { it.type == DrivingEventType.MISSING_TURN_SIGNAL })
    }

    @Test
    fun `한 번의 조향 구간에서 누락을 중복 계산하지 않는다`() {
        val samples = listOf(0f, 15f, 30f, 40f, 45f)
            .mapIndexed { i, deg -> sample(atMs = i * 1000L, steering = deg, signal = TurnSignal.OFF) }

        assertEquals(
            1,
            detector.detect(samples).count { it.type == DrivingEventType.MISSING_TURN_SIGNAL },
        )
    }

    @Test
    fun `조향 값이 없는 구간에서는 조향 이벤트를 만들지 않는다`() {
        val samples = (0 until 5).map { sample(atMs = it * 100L, steering = null) }

        assertTrue(detector.detect(samples).isEmpty())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :core:test --tests '*EventDetectorSteeringTest*'`
Expected: FAIL — 조향 이벤트 미구현

- [ ] **Step 3: 조향·방향지시등 검출을 추가한다**

`EventDetector.detect`에 두 판정을 더한다. 가속도 결과와 합쳐 `timestampMs` 오름차순으로 반환한다.

- **급조향:** 인접 샘플에서 `dt`가 유효하고 양쪽 `steeringDeg`가 null이 아니며 `speedMps`가 `HARSH_STEER_MIN_SPEED_MPS` 초과일 때 `|Δsteering| / dtSec > HARSH_STEER_DEG_PER_SEC`이면 `HARSH_STEER`. `value`는 계산된 각속도.
  - 속도가 null이면 급조향 판정을 하지 않는다(근거 없이 판정하지 않는다).
- **방향지시등 누락:** 조향 값이 연속으로 존재하는 구간마다 기준점 `anchor`를 잡는다. 같은 구간 안에서 `|steering - anchor| >= MISSING_SIGNAL_STEER_DEG`가 되는 순간, 그 구간에서 지나온 샘플의 `turnSignal`이 **모두 `OFF`**였으면 `MISSING_TURN_SIGNAL`을 1회 기록하고 `anchor`를 현재 조향값으로 옮긴다. 하나라도 `LEFT`/`RIGHT`였거나 `turnSignal`이 null이면 기록하지 않고 `anchor`만 옮긴다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :core:test --tests '*EventDetectorSteeringTest*'`
Expected: PASS (9 tests)

- [ ] **Step 5: 기존 테스트가 깨지지 않았는지 확인한다**

Run: `./gradlew :core:test`
Expected: PASS (Task 1 테스트 포함 전체)

- [ ] **Step 6: 커밋**

```bash
git add core/src/main/kotlin/ai/tireon/core/analysis/EventDetector.kt core/src/test/kotlin/ai/tireon/core/analysis/EventDetectorSteeringTest.kt
git commit -m "feat: 조향과 방향지시등 이벤트 검출 추가"
```

---

### Task 3: 점수 계산

**Files:**
- Create: `core/src/main/kotlin/ai/tireon/core/analysis/ScoreCalculator.kt`
- Test: `core/src/test/kotlin/ai/tireon/core/analysis/ScoreCalculatorTest.kt`

**Interfaces:**
- Consumes: `DrivingEvent`, `DrivingEventType`, `Thresholds`, 기존 `ai.tireon.core.DrivingScore`
- Produces:
  - `data class EventContribution(val type: DrivingEventType, val count: Int, val penalty: Int)`
  - `sealed interface ScoreResult { data class Scored(val score: DrivingScore, val breakdown: List<EventContribution>) : ScoreResult; data class Insufficient(val reason: InsufficientReason) : ScoreResult }`
  - `enum class InsufficientReason { DISTANCE_TOO_SHORT, DURATION_TOO_SHORT, NOT_ENOUGH_SAMPLES }`
  - `class ScoreCalculator { fun calculate(events: List<DrivingEvent>, distanceKm: Float, durationSec: Int, sampleCount: Int): ScoreResult }`

기존 `ai.tireon.core.DrivingScore`가 0..100 범위를 이미 검증하므로 점수 타입으로 재사용한다. 새 Int 래퍼를 만들지 않는다.

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

`ScoreCalculatorTest.kt`:

```kotlin
package ai.tireon.core.analysis

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScoreCalculatorTest {

    private val calculator = ScoreCalculator()

    private fun event(type: DrivingEventType, atMs: Long = 0L) = DrivingEvent(
        type = type,
        atMs = atMs,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `이벤트가 없으면 100점이다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 5f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(100, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `거리 1km당 이벤트 비율로 감점한다`() {
        // 급제동 2회 / 2km = 1.0회/km * 8점 = 8점 감점
        val result = calculator.calculate(
            events = listOf(event(DrivingEventType.HARSH_BRAKE), event(DrivingEventType.HARSH_BRAKE)),
            distanceKm = 2f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(92, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `거리가 1km 미만이면 1km로 나눈다`() {
        // 급가속 1회 / max(0.5km, 1.0km) = 1.0 * 6점 = 6점 감점
        val result = calculator.calculate(
            events = listOf(event(DrivingEventType.HARSH_ACCEL)),
            distanceKm = 0.8f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(94, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `감점이 100을 넘어도 0점 아래로 내려가지 않는다`() {
        val events = List(50) { event(DrivingEventType.HARSH_BRAKE) }
        val result = calculator.calculate(
            events = events,
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(0, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `거리가 0_5km 미만이면 점수를 만들지 않는다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 0.4f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(
            InsufficientReason.DISTANCE_TOO_SHORT,
            assertIs<ScoreResult.Insufficient>(result).reason,
        )
    }

    @Test
    fun `주행 시간이 60초 미만이면 점수를 만들지 않는다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 5f,
            durationSec = 59,
            sampleCount = 500,
        )

        assertEquals(
            InsufficientReason.DURATION_TOO_SHORT,
            assertIs<ScoreResult.Insufficient>(result).reason,
        )
    }

    @Test
    fun `유효 샘플이 20개 미만이면 점수를 만들지 않는다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 5f,
            durationSec = 600,
            sampleCount = 19,
        )

        assertEquals(
            InsufficientReason.NOT_ENOUGH_SAMPLES,
            assertIs<ScoreResult.Insufficient>(result).reason,
        )
    }

    @Test
    fun `이벤트 종류별 횟수와 감점을 함께 돌려준다`() {
        val result = calculator.calculate(
            events = listOf(
                event(DrivingEventType.HARSH_BRAKE),
                event(DrivingEventType.HARSH_STEER),
                event(DrivingEventType.HARSH_STEER),
            ),
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 500,
        )

        val breakdown = assertIs<ScoreResult.Scored>(result).breakdown
        val brake = breakdown.first { it.type == DrivingEventType.HARSH_BRAKE }
        val steer = breakdown.first { it.type == DrivingEventType.HARSH_STEER }

        assertEquals(1, brake.count)
        assertEquals(8, brake.penalty)
        assertEquals(2, steer.count)
        assertEquals(10, steer.penalty)
    }

    @Test
    fun `발생하지 않은 이벤트는 breakdown에 넣지 않는다`() {
        val result = calculator.calculate(
            events = listOf(event(DrivingEventType.HARSH_BRAKE)),
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 500,
        )

        val breakdown = assertIs<ScoreResult.Scored>(result).breakdown
        assertTrue(breakdown.none { it.type == DrivingEventType.HARSH_STEER })
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :core:test --tests '*ScoreCalculatorTest*'`
Expected: FAIL — `ScoreCalculator` 미정의로 컴파일 실패

- [ ] **Step 3: `ScoreCalculator`를 구현한다**

`ScoreCalculator.kt`. 판정 순서를 고정한다: 거리 → 시간 → 샘플 수. 세 조건을 모두 만족할 때만 점수를 만든다.

감점 계산:
```
rate = count / max(distanceKm, 1f)
penalty = (weight * rate).roundToInt()
score = (100 - sum(penalties)).coerceIn(0, 100)
```

가중치는 `Thresholds.W_HARSH_BRAKE`(8), `W_HARSH_ACCEL`(6), `W_HARSH_STEER`(5), `W_MISSING_SIGNAL`(4)를 쓴다. `DrivingScore` 생성자를 통해 범위를 최종 검증한다.

`breakdown`은 이벤트가 1회 이상인 타입만 담고, `penalty` 내림차순으로 정렬한다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :core:test --tests '*ScoreCalculatorTest*'`
Expected: PASS (9 tests)

- [ ] **Step 5: 커밋**

```bash
git add core/src/main/kotlin/ai/tireon/core/analysis/ScoreCalculator.kt core/src/test/kotlin/ai/tireon/core/analysis/ScoreCalculatorTest.kt
git commit -m "feat: 주행 점수 계산과 데이터 부족 판정 추가"
```

---

### Task 4: 룰 기반 코치 문장

**Files:**
- Create: `core/src/main/kotlin/ai/tireon/core/coach/CoachMessage.kt`
- Create: `core/src/main/kotlin/ai/tireon/core/coach/CoachMessages.kt`
- Test: `core/src/test/kotlin/ai/tireon/core/coach/CoachMessagesTest.kt`

**Interfaces:**
- Consumes: `DrivingEvent`, `DrivingEventType`, `ScoreResult`, `EventContribution`
- Produces:
  - `enum class Severity { GOOD, WARN, CRITICAL }`
  - `data class CoachMessage(val text: String, val severity: Severity, val evidence: String?)`
  - `class CoachMessages { fun build(events: List<DrivingEvent>, result: ScoreResult): List<CoachMessage> }`

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

`CoachMessagesTest.kt`:

```kotlin
package ai.tireon.core.coach

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.EventContribution
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoachMessagesTest {

    private val coach = CoachMessages()

    private fun event(type: DrivingEventType, value: Float, atMs: Long = 1_000L) = DrivingEvent(
        type = type,
        atMs = atMs,
        value = value,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `급제동 횟수와 측정 근거를 함께 전달한다`() {
        val events = listOf(
            event(DrivingEventType.HARSH_BRAKE, -3.2f),
            event(DrivingEventType.HARSH_BRAKE, -4.1f),
        )
        val result = ScoreResult.Scored(
            score = DrivingScore(84),
            breakdown = listOf(EventContribution(DrivingEventType.HARSH_BRAKE, 2, 16)),
        )

        val message = coach.build(events, result).first { it.text.contains("급제동") }

        assertTrue(message.text.contains("2"))
        assertTrue(message.evidence!!.contains("4.1"))
    }

    @Test
    fun `발생하지 않은 항목은 칭찬 문장으로 만든다`() {
        val result = ScoreResult.Scored(
            score = DrivingScore(100),
            breakdown = emptyList(),
        )

        val messages = coach.build(emptyList(), result)

        assertTrue(messages.any { it.text.contains("부드러") || it.text.contains("좋") })
    }

    @Test
    fun `점수와 다음 목표를 한 문장으로 만든다`() {
        val result = ScoreResult.Scored(
            score = DrivingScore(85),
            breakdown = listOf(EventContribution(DrivingEventType.HARSH_BRAKE, 1, 8)),
        )

        val messages = coach.build(emptyList(), result)

        assertTrue(messages.any { it.text.contains("85") })
    }

    @Test
    fun `데이터 부족은 점수 문장을 만들지 않는다`() {
        val result = ScoreResult.Insufficient(InsufficientReason.DISTANCE_TOO_SHORT)

        val messages = coach.build(emptyList(), result)

        assertTrue(messages.none { it.text.contains("점이에요") })
        assertTrue(messages.any { it.text.contains("주행") })
    }

    @Test
    fun `모든 메시지에 근거가 있거나 근거가 필요 없는 문장이다`() {
        val events = listOf(event(DrivingEventType.HARSH_STEER, 250f))
        val result = ScoreResult.Scored(
            score = DrivingScore(95),
            breakdown = listOf(EventContribution(DrivingEventType.HARSH_STEER, 1, 5)),
        )

        val messages = coach.build(events, result)

        assertTrue(messages.isNotEmpty())
        assertTrue(messages.all { it.text.isNotBlank() })
    }

    @Test
    fun `위험도가 높은 항목을 먼저 보여준다`() {
        val events = listOf(
            event(DrivingEventType.MISSING_TURN_SIGNAL, 45f),
            event(DrivingEventType.HARSH_BRAKE, -5f),
        )
        val result = ScoreResult.Scored(
            score = DrivingScore(80),
            breakdown = listOf(
                EventContribution(DrivingEventType.HARSH_BRAKE, 1, 8),
                EventContribution(DrivingEventType.MISSING_TURN_SIGNAL, 1, 4),
            ),
        )

        val messages = coach.build(events, result)
        val firstIssue = messages.first { it.severity != Severity.GOOD }

        assertEquals(Severity.CRITICAL, firstIssue.severity)
        assertTrue(firstIssue.text.contains("급제동"))
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :core:test --tests '*CoachMessagesTest*'`
Expected: FAIL — `CoachMessages` 미정의로 컴파일 실패

- [ ] **Step 3: `CoachMessages`를 구현한다**

`CoachMessages.kt`. 문장은 한국어로 쓰고, 측정값을 `String.format`으로 넣는다. 형식은 `%.1f`.

문장 규칙:

| 조건 | text | evidence | severity |
|---|---|---|---|
| 급제동 n>0 | "급제동이 n번 있었어요. 브레이크를 조금 더 미리 밟아보세요." | "감속도 -x.x m/s² (측정)" | `CRITICAL` |
| 급가속 n>0 | "급가속이 n번 있었어요. 출발할 때 가속 페달을 천천히 밟아보세요." | "가속도 +x.x m/s² (측정)" | `WARN` |
| 급조향 n>0 | "핸들을 급하게 돌린 구간이 n번 있었어요." | "조향 속도 x.x °/s (측정)" | `WARN` |
| 깜빡이 누락 n>0 | "방향지시등 없이 핸들을 돌린 구간이 n번 있었어요." | "조향 x.x° 변화 (측정)" | `WARN` |
| 이벤트 0건 | "이번 주행은 아주 부드러웠어요." | null | `GOOD` |
| 점수 | "오늘은 n점이에요." (감점 1위 항목이 있으면 "…만 줄이면 더 좋아져요."를 덧붙인다) | null | `GOOD` (80점 이상) / `WARN` (80점 미만) |
| `DISTANCE_TOO_SHORT` | "조금 더 길게 주행하면 점수를 낼 수 있어요." | null | `WARN` |
| `DURATION_TOO_SHORT` | "1분 이상 주행하면 점수를 낼 수 있어요." | null | `WARN` |
| `NOT_ENOUGH_SAMPLES` | "차량 신호를 충분히 받지 못했어요." | null | `WARN` |

정렬: `CRITICAL` → `WARN` → `GOOD`. 점수 문장은 항상 마지막.

`evidence`의 최댓값은 해당 타입 이벤트의 `value` 절댓값 최대다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :core:test --tests '*CoachMessagesTest*'`
Expected: PASS (6 tests)

- [ ] **Step 5: 전체 테스트와 Lint를 확인한다**

Run: `./gradlew :core:test`
Expected: PASS (기존 `DrivingScoreTest` 포함)

- [ ] **Step 6: 문서와 코드가 어긋나지 않았는지 확인한다**

`docs/design/app-foundation-design.md` 6.2장의 `ScoreResult`가 `score: DrivingScore`로 되어 있는지 확인한다. 다르면 문서를 코드에 맞춘다.

Run: `python3 .github/scripts/check_docs.py`
Expected: `검사 통과`

- [ ] **Step 7: 커밋**

```bash
git add core/src/main/kotlin/ai/tireon/core/coach core/src/test/kotlin/ai/tireon/core/coach docs/design
git commit -m "feat: 룰 기반 코치 문장 생성 추가"
```

---

## 다음 계획

이 계획은 `:core` 기반만 다룬다. 아래는 후속 계획으로 분리한다.

| 후속 계획 | 내용 |
|---|---|
| 신호 계층 | AAOS 속도 리더, Pleos 어댑터, `PleosSignalSource`, DO·`CarUxRestrictions` 실측 |
| 앱 셸 | `MainActivity`, 내비게이션, 반응형 레이아웃 |
| 연습 세션 | `PracticeActivity`, TTS, 세션 저장 |
| 리포트 | 저장 구현, 리포트 화면 |
| LLM 다듬기 | `CoachRefiner`와 숫자 불변 가드(7.3장). 네트워크가 필요하므로 마지막 |
