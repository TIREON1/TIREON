# 연습 세션 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** DO를 선언한 `PracticeActivity`에서 신호를 수집하고, 제한 상태에 따라 UI를 축소하며, 세션 결과를 요약한다.

**Architecture:** 세션 상태 전이를 순수 함수(`PracticeSessionState`)로 분리해 JVM에서 시험합니다. 액티비티는 상태를 그리기만 하고, 신호 수집과 분석은 `:pleos`와 `:core`의 기존 부품을 조립합니다.

**Tech Stack:** Compose, lifecycle-viewmodel-compose, kotlinx-coroutines, 기존 `:core`·`:pleos` 부품

**Spec:** `docs/design/app-foundation-design.md` (3.1~3.3 안전 모델, 5.1 연습 미션)

## Global Constraints

- `PracticeActivity`만 `distractionOptimized=true`를 선언합니다. 다른 액티비티는 선언하지 않습니다.
- 제한 활성 시 화면에는 숫자 하나만 표시하고 터치 요소를 두지 않습니다.
- 제한이 해제되기 전에는 요약 화면으로 넘어가지 않습니다.
- 측정 불가를 0으로 표시하지 않습니다.
- SDK 타입을 `:app`으로 노출하지 않습니다.
- 커밋 메시지는 `type: 한국어 내용` 형식을 따릅니다.
- 검증 명령: `./gradlew assembleDebug test lint`

## Review Focus

1. 제한 상태를 모르는 경우(null). 전체 UI를 띄우면 안전 규정을 위반합니다.
2. 세션 도중 액티비티가 백그라운드로 갈 때. 수집은 계속하되 UI 갱신과 음성은 멈춰야 합니다.
3. 세션을 반복 시작할 때. 리스너가 중복 등록되어 이벤트가 배로 계산되면 안 됩니다.
4. 신호가 하나도 오지 않을 때. 빈 세션으로 끝나야 하고 크래시가 없어야 합니다.
5. 요약 화면 진입 조건. 차가 움직이는 동안 결과를 노출하면 안 됩니다.

---

### Task 1: 세션 상태 전이

**Files:**
- Create: `app/src/main/kotlin/ai/tireon/app/practice/PracticeSessionState.kt`
- Test: `app/src/test/kotlin/ai/tireon/app/practice/PracticeSessionStateTest.kt`

**Interfaces:**
- Consumes: `DrivingRestriction` (`:pleos`)
- Produces:
  ```kotlin
  enum class PracticePhase { READY, RUNNING, FINISHED }
  data class PracticeSessionState(
      val phase: PracticePhase,
      val restriction: DrivingRestriction,
      val elapsedSec: Int,
      val sampleCount: Int,
  ) {
      val showMinimalUi: Boolean
      companion object {
          fun initial(): PracticeSessionState
          fun PracticeSessionState.start(restriction: DrivingRestriction?): PracticeSessionState
          fun PracticeSessionState.tick(restriction: DrivingRestriction?, sampleCount: Int): PracticeSessionState
          fun PracticeSessionState.finish(restriction: DrivingRestriction?): PracticeSessionState
      }
  }
  ```

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

```kotlin
package ai.tireon.app.practice

import ai.tireon.pleos.restriction.DrivingRestriction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeSessionStateTest {

    @Test
    fun `처음에는 준비 상태다`() {
        val state = PracticeSessionState.initial()

        assertEquals(PracticePhase.READY, state.phase)
        assertEquals(DrivingRestriction.NONE, state.restriction)
        assertFalse(state.showMinimalUi)
    }

    @Test
    fun `시작하면 진행 상태가 된다`() {
        val state = PracticeSessionState.initial().start(DrivingRestriction.NONE)

        assertEquals(PracticePhase.RUNNING, state.phase)
    }

    @Test
    fun `제한이 걸리면 최소 UI로 바뀐다`() {
        val state = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.MINIMAL_UI, sampleCount = 5)

        assertTrue(state.showMinimalUi)
        assertEquals(5, state.sampleCount)
    }

    @Test
    fun `제한 상태를 모르면 최소 UI로 본다`() {
        val state = PracticeSessionState.initial().start(null)

        assertTrue(state.showMinimalUi)
    }

    @Test
    fun `진행 중에는 초가 증가한다`() {
        var state = PracticeSessionState.initial().start(DrivingRestriction.NONE)
        repeat(3) { state = state.tick(DrivingRestriction.NONE, state.sampleCount) }

        assertEquals(3, state.elapsedSec)
    }

    @Test
    fun `제한이 남아 있으면 종료할 수 없다`() {
        val running = PracticeSessionState.initial().start(DrivingRestriction.NONE)

        val state = running.finish(DrivingRestriction.MINIMAL_UI)

        assertEquals(PracticePhase.RUNNING, state.phase)
    }

    @Test
    fun `제한이 풀리면 종료된다`() {
        val running = PracticeSessionState.initial().start(DrivingRestriction.MINIMAL_UI)

        val state = running.finish(DrivingRestriction.NONE)

        assertEquals(PracticePhase.FINISHED, state.phase)
    }

    @Test
    fun `준비 상태에서는 종료되지 않는다`() {
        val state = PracticeSessionState.initial().finish(DrivingRestriction.NONE)

        assertEquals(PracticePhase.READY, state.phase)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests '*PracticeSessionStateTest*'`
Expected: FAIL. `PracticeSessionState` 미정의

- [ ] **Step 3: 구현한다**

`restriction`이 `null`이면 `MINIMAL_UI`로 봅니다. `finish`는 `RUNNING`이면서 제한이 `NONE`일 때만 `FINISHED`로 바꿉니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests '*PracticeSessionStateTest*'`
Expected: PASS (8 tests)

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/kotlin/ai/tireon/app/practice app/src/test/kotlin/ai/tireon/app/practice
git commit -m "feat: 연습 세션 상태 전이 추가"
```

---

### Task 2: 세션 요약 계산

**Files:**
- Create: `app/src/main/kotlin/ai/tireon/app/practice/PracticeSummary.kt`
- Test: `app/src/test/kotlin/ai/tireon/app/practice/PracticeSummaryTest.kt`

**Interfaces:**
- Consumes: `DrivingEvent`, `ScoreResult`, `InsufficientReason`
- Produces:
  ```kotlin
  data class PracticeSummary(
      val scoreText: String?,
      val message: String,
      val eventCount: Int,
  ) {
      companion object {
          fun from(result: ScoreResult, events: List<DrivingEvent>): PracticeSummary
      }
  }
  ```

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

```kotlin
package ai.tireon.app.practice

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PracticeSummaryTest {

    private fun event(type: DrivingEventType) = DrivingEvent(
        type = type,
        atMs = 0L,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `점수가 있으면 숫자와 이벤트 수를 보여준다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Scored(DrivingScore(88), emptyList()),
            events = listOf(event(DrivingEventType.HARSH_BRAKE)),
        )

        assertEquals("88", summary.scoreText)
        assertEquals(1, summary.eventCount)
        assertEquals("연습이 끝났어요", summary.message)
    }

    @Test
    fun `데이터가 부족하면 점수 없이 이유를 안내한다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Insufficient(InsufficientReason.DURATION_TOO_SHORT),
            events = emptyList(),
        )

        assertNull(summary.scoreText)
        assertEquals("1분 이상 연습하면 점수를 낼 수 있어요", summary.message)
        assertEquals(0, summary.eventCount)
    }

    @Test
    fun `샘플 부족은 신호 문제로 안내한다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Insufficient(InsufficientReason.NOT_ENOUGH_SAMPLES),
            events = emptyList(),
        )

        assertEquals("차량 신호를 충분히 받지 못했어요", summary.message)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests '*PracticeSummaryTest*'`
Expected: FAIL

- [ ] **Step 3: 구현한다**

`Scored`이면 점수 문자열과 "연습이 끝났어요"를, `Insufficient`이면 사유별 문구를 넣습니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests '*PracticeSummaryTest*'`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add app/src/main/kotlin/ai/tireon/app/practice/PracticeSummary.kt app/src/test/kotlin/ai/tireon/app/practice/PracticeSummaryTest.kt
git commit -m "feat: 연습 세션 요약 계산 추가"
```

---

### Task 3: 세션 ViewModel

**Files:**
- Create: `app/src/main/kotlin/ai/tireon/app/practice/PracticeViewModel.kt`
- Create: `pleos/src/main/kotlin/ai/tireon/pleos/restriction/RestrictionMonitor.kt`

**Interfaces:**
- Consumes: Task 1의 `PracticeSessionState`, `PleosSignalSource`, `EventDetector`
- Produces:
  ```kotlin
  class RestrictionMonitor(context: Context) {
      fun observe(): Flow<DrivingRestriction>
      fun stop()
  }

  class PracticeViewModel(
      signalSource: PleosSignalSource,
      restrictionMonitor: RestrictionMonitor,
  ) : ViewModel() {
      val state: StateFlow<PracticeSessionState>
      val summary: StateFlow<PracticeSummary?>
      fun start()
      fun stop()
  }
  ```

- [ ] **Step 1: 제한 감시자를 만든다**

`CarUxRestrictionsManager`를 구독해 `DrivingRestriction`을 흘려보냅니다. 콜백이 오지 않으면 `null`을 흘려 `MINIMAL_UI`로 처리되게 합니다. SDK 타입은 감시자 밖으로 나오지 않습니다.

- [ ] **Step 2: ViewModel을 만든다**

`start()`하면 세션 흐름을 켜고, 샘플을 모아 `EventDetector`에 넘깁니다. `stop()`하면 수집을 끄고 `ScoreCalculator`로 점수를 내 `summary`를 채웁니다.

- [ ] **Step 3: 빌드로 확인한다**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add app/src/main/kotlin/ai/tireon/app/practice pleos/src/main/kotlin/ai/tireon/pleos/restriction
git commit -m "feat: 연습 세션 ViewModel과 제한 감시자 추가"
```

---

### Task 4: PracticeActivity와 화면

**Files:**
- Create: `app/src/main/kotlin/ai/tireon/app/practice/PracticeActivity.kt`
- Create: `app/src/main/kotlin/ai/tireon/app/practice/PracticeScreen.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: Task 1~3
- Produces: 없음 (화면)

- [ ] **Step 1: 매니페스트에 DO를 선언한다**

```xml
<activity
    android:name="ai.tireon.app.practice.PracticeActivity"
    android:exported="false"
    android:theme="@style/Theme.TIREON">
    <meta-data
        android:name="distractionOptimized"
        android:value="true" />
</activity>
```

다른 액티비티에는 선언하지 않습니다.

- [ ] **Step 2: 화면을 만든다**

세 상태를 그립니다.
- READY: 미션 이름, 설명, 시작 버튼
- RUNNING + 제한: 숫자 하나(속도)만. 터치 요소 없음
- RUNNING + 제한 없음: 미션 이름, 경과 시간, 정지 버튼
- FINISHED: 요약 카드, 홈으로 버튼

- [ ] **Step 3: 빌드와 Lint를 확인한다**

Run: `./gradlew :app:assembleDebug :app:lintDebug`
Expected: BUILD SUCCESSFUL, 새 Lint 오류 없음

- [ ] **Step 4: 커밋**

```bash
git add app/src
git commit -m "feat: PracticeActivity와 연습 화면 추가"
```

---

### Task 5: 앱 셸 연결

**Files:**
- Modify: `app/src/main/kotlin/ai/tireon/app/ui/TireonApp.kt`
- Modify: `app/src/main/kotlin/ai/tireon/app/ui/TireonDestination.kt`

**Interfaces:**
- Consumes: Task 4의 `PracticeActivity`
- Produces: 없음

- [ ] **Step 1: 연습 목적지가 PracticeActivity를 띄우게 한다**

홈의 "연습 시작"과 연습 탭이 `PracticeActivity`를 인텐트로 엽니다. 연습 탭은 자체 화면 대신 진입 안내와 시작 버튼을 둡니다.

- [ ] **Step 2: 빌드로 확인한다**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add app/src
git commit -m "feat: 앱 셸에서 연습 세션 진입 연결"
```

---

### Task 6: 에뮬레이터 검증

**Files:** 없음 (검증만)

- [ ] **Step 1: 주차 상태에서 연습 화면 확인**

AVD를 띄우고 `PracticeActivity`를 실행합니다. READY 상태에서 시작 버튼이 보이는지 확인합니다.

- [ ] **Step 2: 주행 상태에서 최소 UI 확인**

Virtual Console로 기어를 Drive, 속도를 올립니다. 화면이 숫자 하나로 줄어드는지 확인합니다.

- [ ] **Step 3: 행동 이벤트 수집 확인**

급제동과 방향지시등 누락을 만들어 이벤트가 기록되는지 logcat으로 확인합니다.

- [ ] **Step 4: 결과를 문서에 기록한다**

`docs/design/pleos-signal-spike.md`에 연습 세션 검증 결과를 추가합니다.

- [ ] **Step 5: 커밋**

```bash
git add docs/design
git commit -m "docs: 연습 세션 검증 결과 기록"
```

---

## 남는 위험

- 실시간 음성 코칭(TTS)은 이 계획 범위 밖입니다. 별도 계획으로 진행합니다.
- 세션 저장과 리포트도 범위 밖입니다. 요약은 화면에만 표시하고 저장하지 않습니다.
- 제한 상태에서 숫자 하나만 보이는지 여부는 에뮬레이터에서 확인해야 합니다.
