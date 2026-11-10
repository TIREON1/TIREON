package ai.tireon.core.mission

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 주행 미션별 판정.
 *
 * 미션을 고르면 그 미션의 지표만 센다. 고르지 않으면 판정하지 않는다.
 * 미션 없이 주행한 기록에 "전체 점검" 결과가 붙으면 사용자가 오해한다.
 */
class DrivingMissionByMissionTest {

    private fun samples(count: Int = 30) = List(count) { index ->
        DrivingSample(
            timestampMs = index * 100L,
            speedMps = 10f,
            steeringDeg = 0f,
            brakePressed = false,
            turnSignal = null,
            gear = null,
        )
    }

    private fun event(type: DrivingEventType) = DrivingEvent(
        type = type,
        atMs = 0L,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `부드러운 주행은 급가속과 급제동만 센다`() {
        val result = DrivingMissionEvaluator().evaluate(
            mission = DrivingMission.SMOOTH_DRIVING,
            samples = samples(),
            events = listOf(
                event(DrivingEventType.HARSH_ACCEL),
                event(DrivingEventType.HARSH_BRAKE),
                event(DrivingEventType.HARSH_STEER),
                event(DrivingEventType.MISSING_TURN_SIGNAL),
            ),
        )

        assertEquals(2, (result as DrivingMissionResult.Scored).eventCount)
    }

    @Test
    fun `방향지시등 미션은 방향지시등 누락만 센다`() {
        val result = DrivingMissionEvaluator().evaluate(
            mission = DrivingMission.SIGNAL_HABIT,
            samples = samples(),
            events = listOf(
                event(DrivingEventType.HARSH_ACCEL),
                event(DrivingEventType.MISSING_TURN_SIGNAL),
                event(DrivingEventType.MISSING_TURN_SIGNAL),
            ),
        )

        assertEquals(2, (result as DrivingMissionResult.Scored).eventCount)
    }

    @Test
    fun `안정 조향 미션은 급조향만 센다`() {
        val result = DrivingMissionEvaluator().evaluate(
            mission = DrivingMission.STABLE_STEERING,
            samples = samples(),
            events = listOf(
                event(DrivingEventType.HARSH_STEER),
                event(DrivingEventType.HARSH_BRAKE),
            ),
        )

        assertEquals(1, (result as DrivingMissionResult.Scored).eventCount)
    }

    @Test
    fun `전체 점검은 모든 이벤트를 센다`() {
        val result = DrivingMissionEvaluator().evaluate(
            mission = DrivingMission.OVERALL,
            samples = samples(),
            events = listOf(
                event(DrivingEventType.HARSH_ACCEL),
                event(DrivingEventType.HARSH_BRAKE),
                event(DrivingEventType.HARSH_STEER),
                event(DrivingEventType.MISSING_TURN_SIGNAL),
            ),
        )

        assertEquals(4, (result as DrivingMissionResult.Scored).eventCount)
    }

    @Test
    fun `미션별 안내 문구가 그 미션의 지표만 말한다`() {
        val result = DrivingMissionEvaluator().evaluate(
            mission = DrivingMission.SIGNAL_HABIT,
            samples = samples(),
            events = listOf(event(DrivingEventType.MISSING_TURN_SIGNAL)),
        ) as DrivingMissionResult.Scored

        assertTrue(result.detail.contains("방향지시등"))
        assertTrue(!result.detail.contains("급가속"), "다른 미션 지표가 섞였다: ${result.detail}")
    }
}
