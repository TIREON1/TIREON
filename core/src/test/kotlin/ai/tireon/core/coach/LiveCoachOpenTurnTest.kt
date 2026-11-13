package ai.tireon.core.coach

import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 회전 중에 켠 방향지시등은 누락이 아니다.
 *
 * 실시간 안내는 끝나지 않은 회전을 판정하지 않아야 최종 결과와 같은 말을 한다.
 */
class LiveCoachOpenTurnTest {

    private val samples = mutableListOf<DrivingSample>()
    private var t = 0L

    private fun drive(steer: Float, signal: TurnSignal = TurnSignal.OFF) {
        samples += DrivingSample(t, 5.6f, steer, false, signal, Gear.DRIVE)
        t += 100L
    }

    private fun straight() = repeat(30) { drive(0f) }

    private fun steerTo40() = (1..10).forEach { drive(4f * it) }

    private fun steerBack() = (9 downTo 0).forEach { drive(4f * it) }

    @Test
    fun `회전 중에는 누락을 안내하지 않고 회전 중 켠 깜빡이는 누락이 아니다`() {
        val coach = LiveCoach()
        straight()
        steerTo40()

        assertTrue(coach.observe(samples).isEmpty())

        repeat(20) { drive(40f, TurnSignal.RIGHT) }
        steerBack()

        assertTrue(coach.observe(samples).isEmpty())
        assertTrue(EventDetector().detect(samples).isEmpty())
    }

    @Test
    fun `깜빡이 없이 회전을 마치면 그때 누락을 안내한다`() {
        val coach = LiveCoach()
        straight()
        steerTo40()
        coach.observe(samples)
        repeat(20) { drive(40f) }
        steerBack()

        val announced = coach.observe(samples).map { it.type }

        assertEquals(listOf(DrivingEventType.MISSING_TURN_SIGNAL), announced)
    }

    @Test
    fun `세션이 회전 중에 끝나면 최종 판정에는 포함한다`() {
        straight()
        steerTo40()

        val final = EventDetector().detect(samples).map { it.type }

        assertEquals(listOf(DrivingEventType.MISSING_TURN_SIGNAL), final)
    }
}
