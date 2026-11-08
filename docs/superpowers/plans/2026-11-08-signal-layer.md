# 신호 수집 계층 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 차량 신호를 `:core`의 `DrivingSample` 스트림으로 변환하는 `:pleos` 계층을 만든다.

**Architecture:** AAOS 속도 리더와 Pleos Vehicle SDK 리스너를 조합해 1Hz 샘플을 만듭니다. SDK 타입은 `:pleos` 안에서 `:core` 타입으로 변환합니다. `CarUxRestrictions`는 별도 순수 함수로 분리해 JVM에서 시험합니다.

**Tech Stack:** Kotlin 2.3.21, Android Library, Pleos Vehicle SDK 2.3.2, AAOS `CarPropertyManager`, kotlinx-coroutines

**Spec:** `docs/design/app-foundation-design.md` (3.2 제한 판정, 6.3 `:pleos` 공개 타입, 2.1 신호 소스)

## Global Constraints

- SDK 타입은 `:pleos` 밖으로 노출하지 않습니다. 공개 API는 `:core` 타입만 받고 반환합니다.
- 측정 불가는 `null`로 표현합니다. `0f`로 대체하지 않습니다.
- 조향각은 Pleos SDK에서 부호가 반전되어 들어옵니다. 정규화 계층에서 흡수합니다.
- `android.car.jar`는 `compileOnly`로 컴파일하고 런타임은 차량 프레임워크가 제공합니다.
- 리스너는 세션이 활성일 때만 등록하고, 종료 시 반드시 해제합니다.
- 커밋 메시지는 `type: 한국어 내용` 형식을 따릅니다.
- 검증 명령: `./gradlew :pleos:test :pleos:assembleDebug`

## Review Focus

이 계층이 실제로 받게 될 조건 중, 테스트가 없으면 조용히 틀릴 것들입니다.

1. 차량 서비스가 초기화되지 않았거나 도중에 끊긴 경우. 리스너가 죽은 채로 샘플이 끊기면 세션이 빈 데이터로 끝납니다.
2. 값이 오지 않는 신호. Pleos SDK는 미지원 도메인에서 콜백을 부르지 않을 수 있습니다. 초기값을 0으로 두면 급제동으로 오인됩니다.
3. 조향각 부호 반전. 좌회전이 우회전으로 기록되면 방향지시등 판정이 뒤집힙니다.
4. 세션을 반복 시작/종료할 때 리스너가 중복 등록되는 경우. 이벤트가 배로 계산됩니다.
5. `CarUxRestrictions`가 null이거나 콜백이 오지 않는 경우. UI가 축소되지 않으면 안전 규정을 위반합니다.

---

### Task 1: 제한 상태 판정 (순수 함수)

**Files:**
- Create: `pleos/src/main/kotlin/ai/tireon/pleos/restriction/DrivingRestriction.kt`
- Test: `pleos/src/test/kotlin/ai/tireon/pleos/restriction/DrivingRestrictionTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `enum class DrivingRestriction { NONE, MINIMAL_UI }`
  - `object RestrictionPolicy { fun of(requiresDistractionOptimization: Boolean?): DrivingRestriction }`

`CarUxRestrictions`를 직접 받지 않고 boolean만 받습니다. Android 타입 없이 JVM에서 시험하기 위함입니다.

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

```kotlin
package ai.tireon.pleos.restriction

import kotlin.test.Test
import kotlin.test.assertEquals

class DrivingRestrictionTest {

    @Test
    fun `제한이 없으면 전체 UI를 허용한다`() {
        assertEquals(
            DrivingRestriction.NONE,
            RestrictionPolicy.of(requiresDistractionOptimization = false),
        )
    }

    @Test
    fun `제한이 걸리면 최소 UI로 축소한다`() {
        assertEquals(
            DrivingRestriction.MINIMAL_UI,
            RestrictionPolicy.of(requiresDistractionOptimization = true),
        )
    }

    @Test
    fun `상태를 모르면 최소 UI로 본다`() {
        // 제한 상태를 확인하지 못한 채 전체 UI를 띄우면 안전 규정을 위반할 수 있다.
        assertEquals(
            DrivingRestriction.MINIMAL_UI,
            RestrictionPolicy.of(requiresDistractionOptimization = null),
        )
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :pleos:test --tests '*DrivingRestrictionTest*'`
Expected: FAIL. `DrivingRestriction` 미정의로 컴파일 실패

- [ ] **Step 3: 구현한다**

`RestrictionPolicy.of`는 `true`이거나 `null`이면 `MINIMAL_UI`, `false`이면 `NONE`을 반환합니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :pleos:test --tests '*DrivingRestrictionTest*'`
Expected: PASS (3 tests)

- [ ] **Step 5: 커밋**

```bash
git add pleos/src/main/kotlin/ai/tireon/pleos/restriction pleos/src/test/kotlin/ai/tireon/pleos/restriction
git commit -m "feat: 주행 제한 상태 판정 추가"
```

---

### Task 2: 신호 집계기

**Files:**
- Create: `pleos/src/main/kotlin/ai/tireon/pleos/signal/SignalAggregator.kt`
- Test: `pleos/src/test/kotlin/ai/tireon/pleos/signal/SignalAggregatorTest.kt`

**Interfaces:**
- Consumes: `:core`의 `DrivingSample`, `Gear`, `TurnSignal`
- Produces:
  ```kotlin
  class SignalAggregator(private val clock: () -> Long) {
      fun updateSpeed(mps: Float?)
      fun updateSteering(deg: Float?)       // 이미 정규화된 값
      fun updateBrake(pressed: Boolean?)
      fun updateTurnSignal(signal: TurnSignal?)
      fun updateGear(gear: Gear?)
      fun snapshot(): DrivingSample
      fun reset()
  }
  ```

여러 리스너가 서로 다른 스레드에서 값을 밀어넣습니다. 집계기는 마지막 값을 보관하고, 요청 시점에 하나의 샘플로 묶습니다. 초기값은 모두 `null`입니다.

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

```kotlin
package ai.tireon.pleos.signal

import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignalAggregatorTest {

    private var now = 1_000L
    private val aggregator = SignalAggregator { now }

    @Test
    fun `초기 상태는 모두 null이다`() {
        val sample = aggregator.snapshot()

        assertEquals(1_000L, sample.timestampMs)
        assertNull(sample.speedMps)
        assertNull(sample.steeringDeg)
        assertNull(sample.brakePressed)
        assertNull(sample.turnSignal)
        assertNull(sample.gear)
    }

    @Test
    fun `들어온 값만 채워진다`() {
        aggregator.updateSpeed(10f)
        aggregator.updateGear(Gear.DRIVE)

        val sample = aggregator.snapshot()

        assertEquals(10f, sample.speedMps)
        assertEquals(Gear.DRIVE, sample.gear)
        assertNull(sample.steeringDeg)
        assertNull(sample.brakePressed)
    }

    @Test
    fun `마지막 값으로 갱신된다`() {
        aggregator.updateSpeed(10f)
        aggregator.updateSpeed(20f)
        aggregator.updateTurnSignal(TurnSignal.LEFT)
        aggregator.updateTurnSignal(TurnSignal.OFF)

        val sample = aggregator.snapshot()

        assertEquals(20f, sample.speedMps)
        assertEquals(TurnSignal.OFF, sample.turnSignal)
    }

    @Test
    fun `타임스탬프는 스냅샷 시점이다`() {
        now = 5_000L

        assertEquals(5_000L, aggregator.snapshot().timestampMs)
    }

    @Test
    fun `reset은 모든 값을 비운다`() {
        aggregator.updateSpeed(10f)
        aggregator.updateBrake(true)

        aggregator.reset()

        val sample = aggregator.snapshot()
        assertNull(sample.speedMps)
        assertNull(sample.brakePressed)
    }

    @Test
    fun `null을 넣으면 값이 비워진다`() {
        aggregator.updateSpeed(10f)

        aggregator.updateSpeed(null)

        assertNull(aggregator.snapshot().speedMps)
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :pleos:test --tests '*SignalAggregatorTest*'`
Expected: FAIL. `SignalAggregator` 미정의

- [ ] **Step 3: 구현한다**

값은 `@Volatile` 필드로 보관합니다. 여러 스레드에서 쓰고 한 스레드에서 읽기 때문입니다. `reset()`은 모든 필드를 null로 되돌립니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :pleos:test --tests '*SignalAggregatorTest*'`
Expected: PASS (6 tests)

- [ ] **Step 5: 커밋**

```bash
git add pleos/src/main/kotlin/ai/tireon/pleos/signal pleos/src/test/kotlin/ai/tireon/pleos/signal
git commit -m "feat: 차량 신호 집계기 추가"
```

---

### Task 3: SDK 값 변환

**Files:**
- Create: `pleos/src/main/kotlin/ai/tireon/pleos/signal/SignalMapper.kt`
- Test: `pleos/src/test/kotlin/ai/tireon/pleos/signal/SignalMapperTest.kt`

**Interfaces:**
- Consumes: Pleos SDK의 `VehicleGear`, `TurnSignalValue`
- Produces:
  ```kotlin
  internal object SignalMapper {
      fun gear(value: VehicleGear?): Gear?
      fun turnSignal(value: TurnSignalValue?): TurnSignal?
      fun steering(rawDeg: Float?): Float?
  }
  ```

`SignalMapper`는 `internal`입니다. SDK 타입이 공개 API로 새지 않게 하기 위함입니다.

- [ ] **Step 1: 테스트를 쓴다 (실패 예정)**

테스트는 `:pleos` 모듈 안에 있으므로 `internal`에 접근할 수 있습니다.

```kotlin
package ai.tireon.pleos.signal

import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignalMapperTest {

    @Test
    fun `기어 값을 도메인 기어로 바꾼다`() {
        assertEquals(Gear.PARK, SignalMapper.gear(VehicleGear.GEAR_PARK))
        assertEquals(Gear.REVERSE, SignalMapper.gear(VehicleGear.GEAR_REVERSE))
        assertEquals(Gear.NEUTRAL, SignalMapper.gear(VehicleGear.GEAR_NEUTRAL))
        assertEquals(Gear.DRIVE, SignalMapper.gear(VehicleGear.GEAR_DRIVE))
    }

    @Test
    fun `숫자 기어는 드라이브로 본다`() {
        assertEquals(Gear.DRIVE, SignalMapper.gear(VehicleGear.GEAR_1))
        assertEquals(Gear.DRIVE, SignalMapper.gear(VehicleGear.GEAR_6))
    }

    @Test
    fun `알 수 없는 기어는 UNKNOWN이다`() {
        assertEquals(Gear.UNKNOWN, SignalMapper.gear(VehicleGear.GEAR_UNKNOWN))
        assertNull(SignalMapper.gear(null))
    }

    @Test
    fun `방향지시등 값을 도메인 값으로 바꾼다`() {
        assertEquals(TurnSignal.OFF, SignalMapper.turnSignal(TurnSignalValue.OFF))
        assertEquals(TurnSignal.LEFT, SignalMapper.turnSignal(TurnSignalValue.LEFT))
        assertEquals(TurnSignal.RIGHT, SignalMapper.turnSignal(TurnSignalValue.RIGHT))
        assertNull(SignalMapper.turnSignal(null))
    }

    @Test
    fun `조향각 부호를 정규화한다`() {
        // Pleos SDK는 주입값의 부호를 반전해 전달한다. 좌회전이 양수가 되도록 맞춘다.
        assertEquals(120f, SignalMapper.steering(-120f))
        assertEquals(-200f, SignalMapper.steering(200f))
    }

    @Test
    fun `조향각이 없으면 null이다`() {
        assertNull(SignalMapper.steering(null))
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

Run: `./gradlew :pleos:test --tests '*SignalMapperTest*'`
Expected: FAIL. `SignalMapper` 미정의

- [ ] **Step 3: 구현한다**

`steering`은 부호를 뒤집습니다. 스파이크에서 확인한 사실입니다.

- [ ] **Step 4: 통과를 확인한다**

Run: `./gradlew :pleos:test --tests '*SignalMapperTest*'`
Expected: PASS (6 tests)

- [ ] **Step 5: 커밋**

```bash
git add pleos/src/main/kotlin/ai/tireon/pleos/signal/SignalMapper.kt pleos/src/test/kotlin/ai/tireon/pleos/signal/SignalMapperTest.kt
git commit -m "feat: Pleos 신호를 도메인 타입으로 변환"
```

---

### Task 4: AAOS 속도 리더

**Files:**
- Create: `pleos/src/main/kotlin/ai/tireon/pleos/signal/AaosSpeedReader.kt`
- Modify: `pleos/build.gradle.kts` (`android.car.jar` compileOnly 추가)
- Modify: `pleos/src/main/AndroidManifest.xml` (`CAR_POWERTRAIN` 권한)

**Interfaces:**
- Consumes: 없음
- Produces:
  ```kotlin
  class AaosSpeedReader(context: Context) {
      fun start(onSpeed: (Float?) -> Unit): Boolean
      fun stop()
  }
  ```

`start`는 등록에 성공하면 `true`를 반환합니다. 실패해도 예외를 던지지 않습니다. 속도는 보조 신호가 아니라 핵심이지만, 없다고 앱이 멈추면 안 됩니다.

이 Task는 에뮬레이터가 필요하므로 단위 테스트 대신 빌드와 수동 검증으로 확인합니다. 실제 값 수신은 이미 스파이크에서 확인했습니다.

- [ ] **Step 1: 의존성과 권한을 추가한다**

`pleos/build.gradle.kts`에 `compileOnly(files(...))`로 `android.car.jar`를 추가합니다. `pleos/src/main/AndroidManifest.xml`에 `<uses-permission android:name="android.car.permission.CAR_POWERTRAIN" />`을 추가합니다.

- [ ] **Step 2: 리더를 구현한다**

`PERF_VEHICLE_SPEED`(0x11600207)를 `registerCallback`으로 구독합니다. 콜백마다 m/s 값을 `onSpeed`로 전달합니다. `stop()`은 콜백을 해제하고 `Car`를 disconnect합니다. 등록 중 예외가 나면 `false`를 반환합니다.

- [ ] **Step 3: 빌드를 확인한다**

Run: `./gradlew :pleos:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add pleos/build.gradle.kts pleos/src/main/AndroidManifest.xml pleos/src/main/kotlin/ai/tireon/pleos/signal/AaosSpeedReader.kt
git commit -m "feat: AAOS 차속 리더 추가"
```

---

### Task 5: Pleos 신호 소스

**Files:**
- Create: `pleos/src/main/kotlin/ai/tireon/pleos/signal/PleosSignalSource.kt`
- Modify: `pleos/src/main/kotlin/ai/tireon/pleos/vehicle/VehicleSession.kt` (도메인 접근자 추가)

**Interfaces:**
- Consumes: Task 2의 `SignalAggregator`, Task 3의 `SignalMapper`, Task 4의 `AaosSpeedReader`, 기존 `VehicleSession`
- Produces:
  ```kotlin
  interface PleosSignalSource {
      fun samples(sessionActive: Boolean): Flow<DrivingSample>
  }

  class DefaultPleosSignalSource(
      context: Context,
      private val intervalMs: Long = 1_000L,
  ) : PleosSignalSource
  ```

- [ ] **Step 1: `VehicleSession`에 도메인 접근자를 추가한다**

`gear()`, `brake()`, `steeringWheel()`, `turnSignal()`이 초기화 상태를 확인하고 `VehicleResult`로 감싸 반환하게 합니다. 기존 `odometer()` 패턴을 그대로 따릅니다.

- [ ] **Step 2: 신호 소스를 구현한다**

`sessionActive`가 `true`일 때만 리스너를 등록합니다. Pleos 리스너 4종(기어, 브레이크, 조향, 방향지시등)과 `AaosSpeedReader`를 연결해 `SignalAggregator`에 값을 밀어넣고, `intervalMs`마다 `snapshot()`을 emit합니다. `false`가 되면 모든 리스너를 해제합니다.

중복 등록을 막기 위해 이미 등록된 상태인지 확인합니다.

- [ ] **Step 3: 빌드를 확인한다**

Run: `./gradlew :pleos:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 전체 테스트를 확인한다**

Run: `./gradlew :pleos:test`
Expected: PASS (Task 1~3 테스트 포함)

- [ ] **Step 5: 커밋**

```bash
git add pleos/src/main/kotlin/ai/tireon/pleos/signal/PleosSignalSource.kt pleos/src/main/kotlin/ai/tireon/pleos/vehicle/VehicleSession.kt
git commit -m "feat: Pleos 신호 소스 구현"
```

---

### Task 6: 에뮬레이터 검증

**Files:** 없음 (검증만)

- [ ] **Step 1: 신호 수집을 확인한다**

앱에 임시 화면을 두고 `DefaultPleosSignalSource`를 1Hz로 구독해 logcat으로 출력합니다. Virtual Console로 기어, 브레이크, 조향, 방향지시등을 주입하며 샘플이 바뀌는지 확인합니다.

- [ ] **Step 2: 결과를 문서에 기록한다**

`docs/design/pleos-signal-spike.md`에 통합 검증 결과를 추가합니다. 실패한 신호가 있으면 그 사실을 적습니다.

- [ ] **Step 3: 임시 화면을 제거하고 커밋한다**

```bash
git add docs/design/pleos-signal-spike.md
git commit -m "docs: 신호 수집 계층 검증 결과 기록"
```

---

## 남는 위험

- `CarUxRestrictions`가 제한 상태일 때의 동작은 에뮬레이터에서 재현할 수 없습니다. Task 1의 순수 함수 테스트가 유일한 검증입니다.
- TTS는 이 계획 범위 밖입니다. 별도 계획으로 진행합니다.
- 세션 저장도 이 계획 범위 밖입니다.
