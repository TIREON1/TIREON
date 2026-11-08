package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 속도 적분으로 주행 거리를 추정한다.
 *
 * Pleos Odometer는 누적 거리만 주고 트립 거리를 주지 않는다. 세션 거리는 속도를
 * 적분해 구해야 한다.
 */
class DistanceEstimatorTest {

    private fun sample(atMs: Long, speed: Float?) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = 0f,
        brakePressed = false,
        turnSignal = TurnSignal.OFF,
        gear = Gear.DRIVE,
    )

    @Test
    fun `샘플이 없으면 0이다`() {
        assertEquals(0f, DistanceEstimator.estimateKm(emptyList()))
    }

    @Test
    fun `샘플이 하나면 0이다`() {
        assertEquals(0f, DistanceEstimator.estimateKm(listOf(sample(0L, 10f))))
    }

    @Test
    fun `일정 속도로 1초 이동하면 속도만큼 간다`() {
        // 10 m/s로 1초 = 10m = 0.01km
        val samples = listOf(sample(0L, 10f), sample(1_000L, 10f))

        assertEquals(0.01f, DistanceEstimator.estimateKm(samples), 0.0001f)
    }

    @Test
    fun `여러 구간을 누적한다`() {
        // 10 m/s로 2초 = 20m = 0.02km
        val samples = listOf(
            sample(0L, 10f),
            sample(1_000L, 10f),
            sample(2_000L, 10f),
        )

        assertEquals(0.02f, DistanceEstimator.estimateKm(samples), 0.0001f)
    }

    @Test
    fun `속도가 없는 구간은 건너뛴다`() {
        val samples = listOf(
            sample(0L, 10f),
            sample(1_000L, null),
            sample(2_000L, 10f),
        )

        // 두 번째 구간(0~1초)만 계산된다
        assertEquals(0.01f, DistanceEstimator.estimateKm(samples), 0.0001f)
    }

    @Test
    fun `타임스탬프가 뒤로 가면 건너뛴다`() {
        val samples = listOf(
            sample(2_000L, 10f),
            sample(1_000L, 10f),
        )

        assertEquals(0f, DistanceEstimator.estimateKm(samples))
    }

    @Test
    fun `모든 속도가 없으면 0이다`() {
        val samples = listOf(sample(0L, null), sample(1_000L, null))

        assertEquals(0f, DistanceEstimator.estimateKm(samples))
    }

    @Test
    fun `빠른 속도는 더 먼 거리를 만든다`() {
        val slow = listOf(sample(0L, 5f), sample(1_000L, 5f))
        val fast = listOf(sample(0L, 20f), sample(1_000L, 20f))

        assertEquals(
            true,
            DistanceEstimator.estimateKm(fast) > DistanceEstimator.estimateKm(slow),
        )
    }
}
