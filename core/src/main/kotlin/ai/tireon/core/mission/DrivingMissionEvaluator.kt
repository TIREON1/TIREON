package ai.tireon.core.mission

import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample

/**
 * 주행 미션의 지표를 계산한다.
 *
 * 측정된 이벤트만 센다. 데이터가 없으면 0회가 아니라 판정 불가로 돌려준다.
 */
class DrivingMissionEvaluator {

    fun evaluate(
        mission: DrivingMission,
        samples: List<DrivingSample>,
        events: List<DrivingEvent>,
    ): DrivingMissionResult {
        if (samples.size < MIN_SAMPLES) {
            return DrivingMissionResult.Insufficient("주행 데이터가 부족해요")
        }
        if (samples.all { it.speedMps == null }) {
            return DrivingMissionResult.Insufficient("속도 신호를 받지 못했어요")
        }

        val accel = events.countOf(DrivingEventType.HARSH_ACCEL)
        val brake = events.countOf(DrivingEventType.HARSH_BRAKE)
        val steer = events.countOf(DrivingEventType.HARSH_STEER)
        val signal = events.countOf(DrivingEventType.MISSING_TURN_SIGNAL)

        return when (mission) {
            DrivingMission.SMOOTH_DRIVING -> DrivingMissionResult.Scored(
                eventCount = accel + brake,
                detail = "급가속 ${accel}회, 급제동 ${brake}회",
            )

            DrivingMission.SIGNAL_HABIT -> DrivingMissionResult.Scored(
                eventCount = signal,
                detail = "방향지시등 누락 ${signal}회",
            )

            DrivingMission.STABLE_STEERING -> DrivingMissionResult.Scored(
                eventCount = steer,
                detail = "급조향 ${steer}회",
            )

            DrivingMission.OVERALL -> DrivingMissionResult.Scored(
                eventCount = accel + brake + steer + signal,
                detail = "급가속 ${accel}회, 급제동 ${brake}회, " +
                    "급조향 ${steer}회, 방향지시등 누락 ${signal}회",
            )
        }
    }

    private fun List<DrivingEvent>.countOf(type: DrivingEventType): Int =
        count { it.type == type }

    private companion object {
        const val MIN_SAMPLES = 20
    }
}
