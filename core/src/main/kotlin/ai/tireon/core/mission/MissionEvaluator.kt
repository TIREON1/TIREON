package ai.tireon.core.mission

import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import kotlin.math.sqrt

/**
 * 미션 달성 여부를 판정한다.
 *
 * 측정값만 사용한다. 필요한 신호가 없으면 성공으로 처리하지 않고 판정 불가로
 * 돌려준다. 신호가 없다는 이유로 칭찬하면 안 되기 때문이다.
 */
class MissionEvaluator {

    fun evaluate(
        mission: PracticeMission,
        samples: List<DrivingSample>,
        events: List<DrivingEvent>,
    ): MissionResult {
        if (samples.size < MIN_SAMPLES) {
            return MissionResult.Insufficient("주행 데이터가 부족해요")
        }

        val spec = MissionCatalog.spec(mission)
        insufficientSignal(spec, samples)?.let { return it }

        return when (mission) {
            PracticeMission.SMOOTH_START -> judgeByAbsence(
                events = events,
                type = DrivingEventType.HARSH_ACCEL,
                successDetail = "급가속 없이 출발했어요",
                failureDetail = "급가속이 있었어요",
            )

            PracticeMission.SMOOTH_STOP -> judgeByAbsence(
                events = events,
                type = DrivingEventType.HARSH_BRAKE,
                successDetail = "부드럽게 정지했어요",
                failureDetail = "급제동이 있었어요",
            )

            PracticeMission.TURN_SIGNAL -> judgeByAbsence(
                events = events,
                type = DrivingEventType.MISSING_TURN_SIGNAL,
                successDetail = "방향지시등을 잘 켰어요",
                failureDetail = "방향지시등 없이 조향한 구간이 있었어요",
            )

            PracticeMission.REVERSE_SENSE -> judgeReverse(samples)
            PracticeMission.STEADY_SPEED -> judgeSteadySpeed(samples)
        }
    }

    /** 미션이 요구하는 신호가 전부 없는지 확인한다. */
    private fun insufficientSignal(
        spec: MissionSpec,
        samples: List<DrivingSample>,
    ): MissionResult.Insufficient? {
        if (spec.requiresSpeed && samples.all { it.speedMps == null }) {
            return MissionResult.Insufficient("속도 신호를 받지 못했어요")
        }
        if (spec.requiresSteering && samples.all { it.steeringDeg == null }) {
            return MissionResult.Insufficient("조향 신호를 받지 못했어요")
        }
        if (spec.requiresGear && samples.all { it.gear == null }) {
            return MissionResult.Insufficient("기어 신호를 받지 못했어요")
        }
        if (spec.requiresTurnSignal && samples.all { it.turnSignal == null }) {
            return MissionResult.Insufficient("방향지시등 신호를 받지 못했어요")
        }
        return null
    }

    private fun judgeByAbsence(
        events: List<DrivingEvent>,
        type: DrivingEventType,
        successDetail: String,
        failureDetail: String,
    ): MissionResult {
        val count = events.count { it.type == type }
        return if (count == 0) {
            MissionResult.Success(successDetail)
        } else {
            MissionResult.Failed("$failureDetail ($count 번)")
        }
    }

    /**
     * 후진 기어로 [REQUIRED_REVERSE_MS] 이상 이어지고 조향 입력이 있었는지 본다.
     *
     * 기어가 후진인 구간이 연속으로 유지되어야 한다. 잠깐 스친 것은 인정하지 않는다.
     */
    private fun judgeReverse(samples: List<DrivingSample>): MissionResult {
        var longestRunMs = 0L
        var runStartMs: Long? = null
        var lastReverseMs = 0L
        var steeringDuringRun = 0f

        for (sample in samples) {
            if (sample.gear == Gear.REVERSE) {
                if (runStartMs == null) {
                    runStartMs = sample.timestampMs
                    steeringDuringRun = 0f
                }
                lastReverseMs = sample.timestampMs
                val steering = sample.steeringDeg
                if (steering != null) {
                    steeringDuringRun = maxOf(steeringDuringRun, kotlin.math.abs(steering))
                }
                longestRunMs = maxOf(longestRunMs, lastReverseMs - runStartMs)
            } else {
                runStartMs = null
            }
        }

        return when {
            longestRunMs < REQUIRED_REVERSE_MS ->
                MissionResult.Failed("후진으로 3초 이상 유지해 보세요")

            steeringDuringRun < MIN_STEERING_DEG ->
                MissionResult.Failed("후진하면서 핸들을 조금 돌려보세요")

            else -> MissionResult.Success("후진 감각을 익혔어요")
        }
    }

    /**
     * 속도 표준편차가 [MAX_SPEED_STDDEV_KMH] 미만인지 본다.
     *
     * 단위는 km/h로 환산해 판단한다. 사용자가 화면에서 보는 값과 기준을 맞추기 위함이다.
     */
    private fun judgeSteadySpeed(samples: List<DrivingSample>): MissionResult {
        val speeds = samples.mapNotNull { it.speedMps }.map { it * MPS_TO_KMH }
        if (speeds.size < MIN_SAMPLES) {
            return MissionResult.Insufficient("속도 신호가 충분하지 않아요")
        }

        val mean = speeds.average()
        val variance = speeds.sumOf { (it - mean) * (it - mean) } / speeds.size
        val stdDev = sqrt(variance)

        return if (stdDev < MAX_SPEED_STDDEV_KMH) {
            MissionResult.Success("속도를 일정하게 유지했어요")
        } else {
            MissionResult.Failed("속도가 들쭉날쭉했어요 (편차 ${"%.1f".format(stdDev)}km/h)")
        }
    }

    private companion object {
        const val MIN_SAMPLES = 2
        const val REQUIRED_REVERSE_MS = 3_000L
        const val MIN_STEERING_DEG = 5f
        const val MAX_SPEED_STDDEV_KMH = 3.0
        const val MPS_TO_KMH = 3.6
    }
}
