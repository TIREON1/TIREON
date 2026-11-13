package ai.tireon.core.mission

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 주행 미션 판정.
 *
 * 연습 미션과 달리 주행 미션은 "무엇을 잘했는지"를 지표로 보여준다.
 * 데이터가 부족하면 판정하지 않는다.
 */
class DrivingMissionTest {

    private val evaluator = DrivingMissionEvaluator()

    private fun event(type: DrivingEventType, value: Float = 3f) = DrivingEvent(
        type = type,
        atMs = 0L,
        value = value,
        confidence = Confidence.MEASURED,
    )

    private fun sample(
        atMs: Long,
        speed: Float? = 10f,
        steering: Float? = 0f,
        signal: TurnSignal? = TurnSignal.OFF,
        brake: Boolean? = false,
    ) = DrivingSample(
        timestampMs = atMs,
        speedMps = speed,
        steeringDeg = steering,
        brakePressed = brake,
        turnSignal = signal,
        gear = Gear.DRIVE,
    )

    @Test
    fun `샘플이 부족하면 판정하지 않는다`() {
        val result = evaluator.evaluate(
            mission = DrivingMission.OVERALL,
            samples = emptyList(),
            events = emptyList(),
        )

        assertIs<DrivingMissionResult.Insufficient>(result)
    }

    @Test
    fun `부드러운 가감속 미션은 급가속과 급제동 횟수를 센다`() {
        val samples = (0 until 30).map { sample(it * 1_000L) }
        val events = listOf(
            event(DrivingEventType.HARSH_ACCEL),
            event(DrivingEventType.HARSH_ACCEL),
            event(DrivingEventType.HARSH_BRAKE),
        )

        val result = evaluator.evaluate(
            mission = DrivingMission.SMOOTH_DRIVING,
            samples = samples,
            events = events,
        )

        val scored = assertIs<DrivingMissionResult.Scored>(result)
        assertEquals(3, scored.eventCount)
        assertEquals("급가속 2회, 급제동 1회", scored.detail)
    }

    @Test
    fun `방향지시등 습관 미션은 누락 횟수를 센다`() {
        val samples = (0 until 30).map { sample(it * 1_000L) }
        val events = listOf(event(DrivingEventType.MISSING_TURN_SIGNAL))

        val result = evaluator.evaluate(
            mission = DrivingMission.SIGNAL_HABIT,
            samples = samples,
            events = events,
        )

        val scored = assertIs<DrivingMissionResult.Scored>(result)
        assertEquals(1, scored.eventCount)
        assertEquals("방향지시등 누락 1회", scored.detail)
    }

    @Test
    fun `급조향 방지 미션은 급조향 횟수를 센다`() {
        val samples = (0 until 30).map { sample(it * 1_000L) }
        val events = listOf(
            event(DrivingEventType.HARSH_STEER),
            event(DrivingEventType.HARSH_STEER),
        )

        val result = evaluator.evaluate(
            mission = DrivingMission.STABLE_STEERING,
            samples = samples,
            events = events,
        )

        val scored = assertIs<DrivingMissionResult.Scored>(result)
        assertEquals(2, scored.eventCount)
    }

    @Test
    fun `이벤트가 없으면 0회로 표시한다`() {
        val samples = (0 until 30).map { sample(it * 1_000L) }

        val result = evaluator.evaluate(
            mission = DrivingMission.SMOOTH_DRIVING,
            samples = samples,
            events = emptyList(),
        )

        val scored = assertIs<DrivingMissionResult.Scored>(result)
        assertEquals(0, scored.eventCount)
        assertTrue(scored.detail.contains("급가속 0회"))
    }

    @Test
    fun `종합 미션은 모든 지표를 담는다`() {
        val samples = (0 until 30).map { sample(it * 1_000L) }
        val events = listOf(
            event(DrivingEventType.HARSH_ACCEL),
            event(DrivingEventType.HARSH_BRAKE),
            event(DrivingEventType.HARSH_STEER),
            event(DrivingEventType.MISSING_TURN_SIGNAL),
        )

        val result = evaluator.evaluate(
            mission = DrivingMission.OVERALL,
            samples = samples,
            events = events,
        )

        val scored = assertIs<DrivingMissionResult.Scored>(result)
        assertEquals(4, scored.eventCount)
        assertTrue(scored.detail.contains("급가속"))
        assertTrue(scored.detail.contains("급제동"))
        assertTrue(scored.detail.contains("급조향"))
        assertTrue(scored.detail.contains("방향지시등"))
    }

    @Test
    fun `부드러운 가감속 미션은 브레이크 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 1_000L, brake = null) }

        val result = evaluator.evaluate(
            mission = DrivingMission.SMOOTH_DRIVING,
            samples = samples,
            events = emptyList(),
        )

        assertIs<DrivingMissionResult.Insufficient>(result)
    }

    @Test
    fun `방향지시등 습관 미션은 방향지시등 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 1_000L, signal = null) }

        val result = evaluator.evaluate(
            mission = DrivingMission.SIGNAL_HABIT,
            samples = samples,
            events = emptyList(),
        )

        assertIs<DrivingMissionResult.Insufficient>(result)
    }

    @Test
    fun `안정적인 조향 미션은 조향 신호가 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 1_000L, steering = null) }

        val result = evaluator.evaluate(
            mission = DrivingMission.STABLE_STEERING,
            samples = samples,
            events = emptyList(),
        )

        assertIs<DrivingMissionResult.Insufficient>(result)
    }

    @Test
    fun `전체 점검 미션은 필요한 신호가 하나라도 없으면 판정하지 않는다`() {
        val samples = (0 until 30).map { sample(it * 1_000L, brake = null) }

        val result = evaluator.evaluate(
            mission = DrivingMission.OVERALL,
            samples = samples,
            events = emptyList(),
        )

        assertIs<DrivingMissionResult.Insufficient>(result)
    }

    @Test
    fun `미션 목록은 네 종류다`() {
        assertEquals(4, DrivingMission.entries.size)
    }
}
