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
