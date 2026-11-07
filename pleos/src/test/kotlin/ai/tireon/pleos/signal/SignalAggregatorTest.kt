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
