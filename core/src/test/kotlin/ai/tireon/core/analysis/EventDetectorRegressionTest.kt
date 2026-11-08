package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 리뷰에서 지적된 오탐·누락 회귀 테스트.
 *
 * 코드보다 사용자 영향이 큰 경우를 고정한다: 맞게 한 운전을 틀렸다고 하거나,
 * 실제 위험 운전을 놓치는 경우.
 */
class EventDetectorRegressionTest {

    private val detector = EventDetector()

    private fun sample(
        atMs: Long,
        steering: Float? = 0f,
        speed: Float? = 20f,
        signal: TurnSignal? = TurnSignal.OFF,
        gear: Gear? = Gear.DRIVE,
        brake: Boolean? = false,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = brake,
        turnSignal = signal,
        gear = gear,
    )

    private fun ramp(from: Float, to: Float, step: Float, stepMs: Long = 100L): List<Float> {
        val values = mutableListOf<Float>()
        var current = from
        val direction = if (to >= from) step else -step
        while (if (direction > 0) current <= to else current >= to) {
            values += current
            current += direction
        }
        return values
    }

    private fun samplesOf(
        steerings: List<Float>,
        stepMs: Long = 100L,
        signal: (Int) -> TurnSignal? = { TurnSignal.OFF },
        speed: Float? = 20f,
        gear: Gear? = Gear.DRIVE,
    ) = steerings.mapIndexed { i, deg ->
        sample(atMs = i * stepMs, steering = deg, speed = speed, signal = signal(i), gear = gear)
    }

    @Test
    fun `방향지시등 없이 90도 회전해도 누락은 1회만 센다`() {
        val samples = samplesOf(ramp(0f, 90f, 6f))

        assertEquals(
            1,
            detector.detect(samples).count { it.type == DrivingEventType.MISSING_TURN_SIGNAL },
        )
    }

    @Test
    fun `방향지시등 없이 180도 회전해도 누락은 1회만 센다`() {
        val samples = samplesOf(ramp(0f, 180f, 6f))

        assertEquals(
            1,
            detector.detect(samples).count { it.type == DrivingEventType.MISSING_TURN_SIGNAL },
        )
    }

    @Test
    fun `누락으로 기록할 때 회전 전체의 최대 조향 변화를 남긴다`() {
        val samples = samplesOf(ramp(0f, 90f, 6f))

        val event = detector.detect(samples)
            .first { it.type == DrivingEventType.MISSING_TURN_SIGNAL }

        assertTrue(event.value >= 84f, "최대 변화가 90도에 가까워야 하는데 ${event.value}였다")
    }

    @Test
    fun `방향지시등을 켰다가 회전 중 꺼져도 누락으로 세지 않는다`() {
        val up = ramp(0f, 90f, 6f)
        val down = ramp(84f, 0f, -6f)
        val all = up + down
        val samples = samplesOf(
            steerings = all,
            stepMs = 100L,
            signal = { i -> if (i < up.size / 2) TurnSignal.LEFT else TurnSignal.OFF },
        )

        assertTrue(
            detector.detect(samples).none { it.type == DrivingEventType.MISSING_TURN_SIGNAL },
        )
    }

    @Test
    fun `주차 중 조향은 방향지시등 누락으로 세지 않는다`() {
        val samples = samplesOf(
            steerings = ramp(0f, 200f, 10f, stepMs = 1000L),
            stepMs = 1000L,
            speed = 0f,
            gear = Gear.PARK,
        )

        assertTrue(
            detector.detect(samples).none { it.type == DrivingEventType.MISSING_TURN_SIGNAL },
        )
    }

    @Test
    fun `후진 중 조향은 방향지시등 누락으로 세지 않는다`() {
        val samples = samplesOf(
            steerings = ramp(0f, 200f, 10f, stepMs = 1000L),
            stepMs = 1000L,
            speed = 3f,
            gear = Gear.REVERSE,
        )

        assertTrue(
            detector.detect(samples).none { it.type == DrivingEventType.MISSING_TURN_SIGNAL },
        )
    }

    @Test
    fun `단일 스파이크 조향은 급조향으로 세지 않는다`() {
        val samples = samplesOf(listOf(0f, 60f, 0f), stepMs = 100L)

        assertTrue(detector.detect(samples).none { it.type == DrivingEventType.HARSH_STEER })
    }

    @Test
    fun `유효 구간 중간의 30ms 중복 샘플이 급제동을 잃지 않게 한다`() {
        // 1.5초 급제동. 일부러 중간에 30ms 간격 샘플을 끼워 넣는다.
        val base = (0 until 16).map { i ->
            sample(atMs = i * 100L, speed = 20f - 4f * 0.1f * i, brake = true)
        }
        val jittered = base.toMutableList()
        jittered.add(8, sample(atMs = 730L, speed = 20f - 4f * 0.73f, brake = true))

        assertEquals(
            1,
            detector.detect(jittered).count { it.type == DrivingEventType.HARSH_BRAKE },
        )
    }

    @Test
    fun `500ms 지속된 급가속을 검출한다`() {
        val samples = (0 until 6).map { i ->
            sample(atMs = i * 100L, speed = 10f + 3.5f * 0.1f * i, steering = null, signal = null)
        }

        assertEquals(
            1,
            detector.detect(samples).count { it.type == DrivingEventType.HARSH_ACCEL },
        )
    }

    @Test
    fun `700ms 지속된 급가속을 검출한다`() {
        val samples = (0 until 8).map { i ->
            sample(atMs = i * 100L, speed = 10f + 3.5f * 0.1f * i, steering = null, signal = null)
        }

        assertEquals(
            1,
            detector.detect(samples).count { it.type == DrivingEventType.HARSH_ACCEL },
        )
    }

    @Test
    fun `임계값과 정확히 같은 가속을 검출한다`() {
        val samples = (0 until 20).map { i ->
            sample(
                atMs = i * 100L,
                speed = 10f + Thresholds.HARSH_ACCEL_MPS2 * 0.1f * i,
                steering = null,
                signal = null,
            )
        }

        assertEquals(
            1,
            detector.detect(samples).count { it.type == DrivingEventType.HARSH_ACCEL },
        )
    }

    @Test
    fun `시작 시각이 뒤로 가는 샘플은 무시한다`() {
        val samples = listOf(
            sample(atMs = 0L, speed = 10f, steering = null, signal = null),
            sample(atMs = 500L, speed = 11f, steering = null, signal = null),
            sample(atMs = 300L, speed = 30f, steering = null, signal = null),
            sample(atMs = 600L, speed = 30.1f, steering = null, signal = null),
        )

        assertTrue(detector.detect(samples).none { it.type == DrivingEventType.HARSH_ACCEL })
    }

    @Test
    fun `빈 목록과 한 개짜리 목록은 빈 결과를 준다`() {
        assertTrue(detector.detect(emptyList()).isEmpty())
        assertTrue(detector.detect(listOf(sample(atMs = 0L))).isEmpty())
    }
}
