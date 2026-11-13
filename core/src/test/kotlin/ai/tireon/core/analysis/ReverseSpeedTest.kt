package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 후진 중 차속은 음수로 온다(AAOS PERF_VEHICLE_SPEED).
 *
 * 같은 제동을 전진과 후진에서 똑같이 판정해야 한다.
 */
class ReverseSpeedTest {

    /** 3m/s까지 4초에 걸쳐 천천히 올렸다가 브레이크로 1초 만에 멈춘다. [sign]이 -1이면 후진. */
    private fun brakeToStop(sign: Float, gear: Gear): List<DrivingSample> {
        val samples = mutableListOf<DrivingSample>()
        var t = 0L
        fun add(speed: Float, brake: Boolean) {
            samples += DrivingSample(t, sign * speed, 0f, brake, TurnSignal.OFF, gear)
            t += 100L
        }
        for (i in 0..40) add(3.0f * i / 40f, brake = false)
        for (i in 1..10) add(3.0f - 3.0f * i / 10f, brake = true)
        repeat(11) { add(0f, brake = true) }
        return samples
    }

    @Test
    fun `후진 중 급제동은 급가속이 아니라 급제동이다`() {
        val forward = EventDetector().detect(brakeToStop(1f, Gear.DRIVE)).map { it.type }
        val reverse = EventDetector().detect(brakeToStop(-1f, Gear.REVERSE)).map { it.type }

        assertEquals(listOf(DrivingEventType.HARSH_BRAKE), forward)
        assertEquals(forward, reverse)
    }

    @Test
    fun `후진 거리는 음수가 아니다`() {
        val forward = DistanceEstimator.estimateKm(brakeToStop(1f, Gear.DRIVE))
        val reverse = DistanceEstimator.estimateKm(brakeToStop(-1f, Gear.REVERSE))

        assertEquals(forward, reverse, 1e-6f)
    }
}
