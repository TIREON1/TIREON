# 미션 카탈로그 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 설계 5.1의 연습 미션 5종을 데이터로 정의하고, 미션별 성공 판정을 계산하며, 화면에서 선택할 수 있게 한다.

**Architecture:** `:core`에 미션 카탈로그와 판정기를 둡니다. 판정은 순수 함수라 JVM에서 시험합니다. 화면은 카탈로그를 읽어 목록과 상세를 그립니다.

**Tech Stack:** Kotlin, Compose, 기존 `:core` 분석 부품

**Spec:** `docs/design/app-foundation-design.md` (5.1 연습 미션)

## Global Constraints

- 미션 정의는 `:core`에 둡니다. 화면 문구는 리소스로 분리합니다.
- 성공 판정은 측정값만 사용합니다. 추정하지 않습니다.
- 데이터가 부족하면 성공도 실패도 아닌 `Insufficient`입니다.
- 새 SDK 의존을 추가하지 않습니다.
- 커밋 메시지는 `type: 한국어 내용` 형식을 따릅니다.
- 검증 명령: `./gradlew assembleDebug test lint`

## Review Focus

1. 신호가 하나도 없는 세션. 성공으로 판정되면 안 됩니다.
2. 미션마다 요구하는 신호가 다를 때. 필요한 신호가 없으면 `Insufficient`여야 합니다.
3. 주차 상태에서 조향을 돌리는 경우. 후진 미션이 아닌데 성공하면 안 됩니다.
4. 경계값. 목표에 정확히 도달했을 때 통과여야 합니다.
5. 미션을 바꿔가며 반복 실행. 이전 미션 결과가 남으면 안 됩니다.

---

### Task 1: 미션 카탈로그

**Files:**
- Create: `core/src/main/kotlin/ai/tireon/core/mission/PracticeMission.kt`
- Create: `core/src/main/kotlin/ai/tireon/core/mission/MissionCatalog.kt`
- Test: `core/src/test/kotlin/ai/tireon/core/mission/MissionCatalogTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class PracticeMission {
      SMOOTH_START, SMOOTH_STOP, REVERSE_SENSE, TURN_SIGNAL, STEADY_SPEED,
  }

  data class MissionSpec(
      val mission: PracticeMission,
      val nameResId: Int,          // 화면 문구 리소스는 :app에서 매핑
      val requiresSpeed: Boolean,
      val requiresSteering: Boolean,
      val requiresGear: Boolean,
      val requiresTurnSignal: Boolean,
  )

  object MissionCatalog {
      fun all(): List<PracticeMission>
      fun spec(mission: PracticeMission): MissionSpec
  }
  ```

`nameResId`를 `:core`에 두면 Android 리소스에 의존하게 됩니다. `:core`는 순수 Kotlin이므로 **문자열 키만** 두고 화면에서 리소스로 매핑합니다.

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

```kotlin
package ai.tireon.core.mission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissionCatalogTest {

    @Test
    fun `연습 미션은 다섯 종류다`() {
        assertEquals(5, MissionCatalog.all().size)
    }

    @Test
    fun `모든 미션에 스펙이 있다`() {
        MissionCatalog.all().forEach { mission ->
            val spec = MissionCatalog.spec(mission)
            assertEquals(mission, spec.mission)
        }
    }

    @Test
    fun `부드러운 출발은 속도만 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.SMOOTH_START)

        assertTrue(spec.requiresSpeed)
        assertTrue(!spec.requiresSteering)
        assertTrue(!spec.requiresGear)
    }

    @Test
    fun `후진 감각은 기어와 조향이 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.REVERSE_SENSE)

        assertTrue(spec.requiresGear)
        assertTrue(spec.requiresSteering)
    }

    @Test
    fun `깜빡이 습관은 방향지시등과 조향이 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.TURN_SIGNAL)

        assertTrue(spec.requiresTurnSignal)
        assertTrue(spec.requiresSteering)
    }

    @Test
    fun `정속 유지는 속도만 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.STEADY_SPEED)

        assertTrue(spec.requiresSpeed)
        assertTrue(!spec.requiresSteering)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :core:test --tests '*MissionCatalogTest*'`
Expected: FAIL. `MissionCatalog` 미정의

- [ ] **Step 3: 구현한다**

각 미션의 필요 신호를 정의합니다. `SMOOTH_START`와 `SMOOTH_STOP`은 속도만, `REVERSE_SENSE`는 기어와 조향, `TURN_SIGNAL`은 방향지시등과 조향, `STEADY_SPEED`는 속도만 필요합니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :core:test --tests '*MissionCatalogTest*'`
Expected: PASS (6 tests)

- [ ] **Step 5: 커밋**

```bash
git add core/src
git commit -m "feat: 연습 미션 카탈로그 추가"
```

---

### Task 2: 미션 판정기

**Files:**
- Create: `core/src/main/kotlin/ai/tireon/core/mission/MissionResult.kt`
- Create: `core/src/main/kotlin/ai/tireon/core/mission/MissionEvaluator.kt`
- Test: `core/src/test/kotlin/ai/tireon/core/mission/MissionEvaluatorTest.kt`

**Interfaces:**
- Consumes: Task 1의 `PracticeMission`, `DrivingSample`, `DrivingEvent`
- Produces:
  ```kotlin
  sealed interface MissionResult {
      data class Success(val detail: String) : MissionResult
      data class Failed(val detail: String) : MissionResult
      data class Insufficient(val reason: String) : MissionResult
  }

  class MissionEvaluator {
      fun evaluate(
          mission: PracticeMission,
          samples: List<DrivingSample>,
          events: List<DrivingEvent>,
      ): MissionResult
  }
  ```

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

```kotlin
package ai.tireon.core.mission

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertIs

class MissionEvaluatorTest {

    private val evaluator = MissionEvaluator()

    private fun sample(
        atMs: Long,
        speed: Float? = 5f,
        steering: Float? = 0f,
        signal: TurnSignal? = TurnSignal.OFF,
        gear: Gear? = Gear.DRIVE,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = false,
        turnSignal = signal,
        gear = gear,
    )

    @Test
    fun `샘플이 없으면 판정하지 않는다`() {
        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_START,
            samples = emptyList(),
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `부드러운 출발은 급가속이 없으면 성공이다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 5f + it * 0.1f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_START,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `부드러운 출발은 급가속이 있으면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L) }
        val events = listOf(
            DrivingEvent(DrivingEventType.HARSH_ACCEL, 0L, 3.5f, Confidence.MEASURED),
        )

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_START,
            samples = samples,
            events = events,
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `부드러운 정지는 급제동이 있으면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L) }
        val events = listOf(
            DrivingEvent(DrivingEventType.HARSH_BRAKE, 0L, 3.5f, Confidence.MEASURED),
        )

        val result = evaluator.evaluate(
            mission = PracticeMission.SMOOTH_STOP,
            samples = samples,
            events = events,
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `후진 감각은 기어 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, gear = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `후진 감각은 후진으로 3초 이상 주행하면 성공이다`() {
        val samples = (0 until 40).map {
            sample(it * 100L, speed = 2f, steering = 10f, gear = Gear.REVERSE)
        }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `후진 감각은 드라이브 기어만으로는 성공하지 못한다`() {
        val samples = (0 until 40).map { sample(it * 100L, gear = Gear.DRIVE) }

        val result = evaluator.evaluate(
            mission = PracticeMission.REVERSE_SENSE,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `깜빡이 습관은 누락이 없으면 성공이다`() {
        val samples = (0 until 30).map { sample(it * 100L, signal = TurnSignal.LEFT) }

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `깜빡이 습관은 누락이 있으면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L) }
        val events = listOf(
            DrivingEvent(DrivingEventType.MISSING_TURN_SIGNAL, 0L, 45f, Confidence.MEASURED),
        )

        val result = evaluator.evaluate(
            mission = PracticeMission.TURN_SIGNAL,
            samples = samples,
            events = events,
        )

        assertIs<MissionResult.Failed>(result)
    }

    @Test
    fun `정속 유지는 속도 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = null) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Insufficient>(result)
    }

    @Test
    fun `정속 유지는 속도가 일정하면 성공이다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = 5.5f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Success>(result)
    }

    @Test
    fun `정속 유지는 속도가 크게 흔들리면 실패다`() {
        val samples = (0 until 30).map { sample(it * 100L, speed = if (it % 2 == 0) 1f else 9f) }

        val result = evaluator.evaluate(
            mission = PracticeMission.STEADY_SPEED,
            samples = samples,
            events = emptyList(),
        )

        assertIs<MissionResult.Failed>(result)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :core:test --tests '*MissionEvaluatorTest*'`
Expected: FAIL

- [ ] **Step 3: 구현한다**

미션별 판정:
- `SMOOTH_START`: `HARSH_ACCEL` 이벤트 0건이면 성공
- `SMOOTH_STOP`: `HARSH_BRAKE` 이벤트 0건이면 성공
- `REVERSE_SENSE`: `REVERSE` 기어 샘플이 3초 이상 이어지고 조향 입력이 있으면 성공
- `TURN_SIGNAL`: `MISSING_TURN_SIGNAL` 0건이면 성공
- `STEADY_SPEED`: 속도 표준편차가 3km/h 미만이면 성공

필요 신호가 전부 null이면 `Insufficient`입니다. 샘플이 2개 미만이면 `Insufficient`입니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :core:test --tests '*MissionEvaluatorTest*'`
Expected: PASS (12 tests)

- [ ] **Step 5: 커밋**

```bash
git add core/src
git commit -m "feat: 미션 성공 판정 추가"
```

---

### Task 3: 미션 선택 화면

**Files:**
- Create: `app/src/main/kotlin/ai/tireon/app/practice/MissionSelection.kt`
- Modify: `app/src/main/kotlin/ai/tireon/app/ui/PracticeEntryScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/test/kotlin/ai/tireon/app/practice/MissionSelectionTest.kt`

**Interfaces:**
- Consumes: Task 1의 `PracticeMission`, `MissionCatalog`
- Produces:
  ```kotlin
  data class MissionDisplay(
      val mission: PracticeMission,
      val nameResId: Int,
      val descriptionResId: Int,
  )

  object MissionSelection {
      fun all(): List<MissionDisplay>
      fun of(mission: PracticeMission): MissionDisplay
  }
  ```

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

```kotlin
package ai.tireon.app.practice

import ai.tireon.core.mission.MissionCatalog
import ai.tireon.core.mission.PracticeMission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissionSelectionTest {

    @Test
    fun `카탈로그의 모든 미션에 문구가 있다`() {
        MissionCatalog.all().forEach { mission ->
            val display = MissionSelection.of(mission)
            assertEquals(mission, display.mission)
            assertTrue(display.nameResId != 0)
            assertTrue(display.descriptionResId != 0)
        }
    }

    @Test
    fun `화면 목록은 카탈로그 순서를 따른다`() {
        val missions = MissionSelection.all().map { it.mission }

        assertEquals(MissionCatalog.all(), missions)
    }

    @Test
    fun `미션마다 문구가 다르다`() {
        val names = MissionSelection.all().map { it.nameResId }

        assertEquals(names.size, names.distinct().size)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests '*MissionSelectionTest*'`
Expected: FAIL

- [ ] **Step 3: 구현한다**

미션별 이름과 설명 리소스 ID를 매핑합니다. `strings.xml`에 5종 문구를 추가합니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests '*MissionSelectionTest*'`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add app/src
git commit -m "feat: 미션 문구 매핑 추가"
```

---

### Task 4: 연습 진입 화면에 미션 목록

**Files:**
- Modify: `app/src/main/kotlin/ai/tireon/app/ui/PracticeEntryScreen.kt`
- Modify: `app/src/main/kotlin/ai/tireon/app/practice/PracticeActivity.kt`
- Modify: `app/src/main/kotlin/ai/tireon/app/practice/PracticeScreen.kt`
- Modify: `app/src/main/kotlin/ai/tireon/app/practice/PracticeViewModel.kt`
- Modify: `app/src/main/AndroidManifest.xml` (인텐트 extra)

- [ ] **Step 1: 미션을 인텐트로 전달한다**

`PracticeActivity`가 `EXTRA_MISSION`을 읽어 ViewModel에 넘깁니다. 없으면 `SMOOTH_START`를 기본값으로 씁니다.

- [ ] **Step 2: 진입 화면에 목록을 그린다**

`PracticeEntryScreen`이 미션 5종을 카드로 나열하고, 선택하면 해당 미션으로 `PracticeActivity`를 엽니다.

- [ ] **Step 3: 화면이 미션 문구를 쓴다**

`PracticeScreen`이 하드코딩된 "부드러운 출발" 대신 전달받은 `MissionDisplay`를 씁니다.

- [ ] **Step 4: 빌드와 Lint를 확인한다**

Run: `./gradlew assembleDebug test lint`
Expected: BUILD SUCCESSFUL, Lint 오류 없음

- [ ] **Step 5: 커밋**

```bash
git add app/src
git commit -m "feat: 미션 선택과 연습 진입 연결"
```

---

### Task 5: 세션 요약에 미션 결과 표시

**Files:**
- Modify: `app/src/main/kotlin/ai/tireon/app/practice/PracticeViewModel.kt`
- Modify: `app/src/main/kotlin/ai/tireon/app/practice/PracticeSummary.kt`
- Modify: `app/src/test/kotlin/ai/tireon/app/practice/PracticeSummaryTest.kt`

- [ ] **Step 1: 테스트를 먼저 고친다**

`PracticeSummary`에 `missionSuccess: Boolean?`을 추가하고, 판정 불가면 null입니다. 기존 테스트에 단언을 더합니다.

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests '*PracticeSummaryTest*'`
Expected: FAIL

- [ ] **Step 3: 구현한다**

`MissionEvaluator` 결과를 요약에 넣습니다. 화면은 성공이면 "목표 달성", 실패면 "다시 해볼까요", 판정 불가면 아무것도 표시하지 않습니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew assembleDebug test lint`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add app/src
git commit -m "feat: 세션 요약에 미션 달성 여부 표시"
```

---

### Task 6: 계측 테스트 갱신

**Files:**
- Modify: `app/src/androidTest/kotlin/ai/tireon/app/practice/PracticeScreenTest.kt`

- [ ] **Step 1: 미션 문구를 인자로 넘기게 바꾼다**

`PracticeScreen`이 `MissionDisplay`를 받으므로 테스트도 넘깁니다.

- [ ] **Step 2: 미션 달성 표시를 검증하는 테스트를 더한다**

성공, 실패, 판정 불가 세 경우를 확인합니다.

- [ ] **Step 3: 에뮬레이터에서 실행한다**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: 전체 PASS

- [ ] **Step 4: 커밋**

```bash
git add app/src/androidTest
git commit -m "test: 미션 화면 계측 테스트 갱신"
```

---

## 남는 위험

- `STEADY_SPEED`의 표준편차 임계값은 임의로 3km/h로 두었습니다. 실측 후 조정이 필요합니다.
- 미션별 목표 시간(3초, 5초 등)도 휴리스틱입니다.
- 미션 결과는 화면에만 표시하고 저장하지 않습니다. 저장 스키마는 다음 작업입니다.
