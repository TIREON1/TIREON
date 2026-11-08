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

    /** 100ms 간격으로 일정한 조향 속도를 만든다. */
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
