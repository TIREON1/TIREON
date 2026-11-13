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
        val spec = DrivingMissionCatalog.of(mission)
        insufficientSignal(spec, samples)?.let { return it }

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

    /** 미션 정의가 요구하는 신호가 샘플에 하나도 없으면 판정하지 않는다. */
    private fun insufficientSignal(
        spec: DrivingMissionSpec,
        samples: List<DrivingSample>,
    ): DrivingMissionResult.Insufficient? = when {
        spec.requiresSpeed && samples.all { it.speedMps == null } ->
            DrivingMissionResult.Insufficient("속도 신호를 받지 못했어요")

        spec.requiresSteering && samples.all { it.steeringDeg == null } ->
            DrivingMissionResult.Insufficient("조향 신호를 받지 못했어요")

        spec.requiresTurnSignal && samples.all { it.turnSignal == null } ->
            DrivingMissionResult.Insufficient("방향지시등 신호를 받지 못했어요")

        spec.requiresBrake && samples.all { it.brakePressed == null } ->
            DrivingMissionResult.Insufficient("브레이크 신호를 받지 못했어요")

        else -> null
    }

    private companion object {
        const val MIN_SAMPLES = 20
    }
}
