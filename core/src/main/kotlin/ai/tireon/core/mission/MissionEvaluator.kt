package ai.tireon.core.mission

import ai.tireon.core.analysis.EventDetector
import ai.tireon.core.analysis.Thresholds
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 미션 달성 여부를 판정한다.
 *
 * 측정값만 사용한다. 필요한 신호가 없으면 성공으로 처리하지 않고 판정 불가로
 * 돌려준다. 신호가 없다는 이유로 칭찬하면 안 되기 때문이다.
 *
 * 같은 이유로 미션이 요구하는 동작을 실제로 했을 때만 성공이다. 급가속이
 * 0회라는 것은 차를 움직이지 않아도 참이다.
 */
class MissionEvaluator {

    private val detector = EventDetector()

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
            PracticeMission.SMOOTH_START -> judgeStart(samples, events)
            PracticeMission.SMOOTH_STOP -> judgeStop(samples, events)
            PracticeMission.TURN_SIGNAL -> judgeTurnSignal(samples, events)
            PracticeMission.REVERSE_SENSE -> judgeReverse(samples)
            PracticeMission.STEADY_SPEED -> judgeSteadySpeed(samples)
            PracticeMission.RIGHT_TURN_STOP -> judgeRightTurnStop(samples)
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

    /** 정지 상태에서 [MOVING_KMH] 이상으로 출발했고 급가속이 없었는지 본다. */
    private fun judgeStart(
        samples: List<DrivingSample>,
        events: List<DrivingEvent>,
    ): MissionResult {
        val harsh = events.count { it.type == DrivingEventType.HARSH_ACCEL }
        return when {
            harsh > 0 -> MissionResult.Failed("급가속이 있었어요 (${harsh}번)")

            !speedWent(samples, from = ::isStopped, to = ::isMoving) ->
                MissionResult.Failed("정지 상태에서 시속 10km 이상으로 출발해 보세요")

            else -> MissionResult.Success("급가속 없이 출발했어요")
        }
    }

    /** [MOVING_KMH] 이상으로 달리다가 완전히 멈췄고 급제동이 없었는지 본다. */
    private fun judgeStop(
        samples: List<DrivingSample>,
        events: List<DrivingEvent>,
    ): MissionResult {
        val harsh = events.count { it.type == DrivingEventType.HARSH_BRAKE }
        return when {
            harsh > 0 -> MissionResult.Failed("급제동이 있었어요 (${harsh}번)")

            !speedWent(samples, from = ::isMoving, to = ::isStopped) ->
                MissionResult.Failed("시속 10km 이상으로 달리다가 완전히 멈춰 보세요")

            else -> MissionResult.Success("부드럽게 정지했어요")
        }
    }

    /** 방향지시등을 켜고 회전한 적이 있고 누락이 없었는지 본다. */
    private fun judgeTurnSignal(
        samples: List<DrivingSample>,
        events: List<DrivingEvent>,
    ): MissionResult {
        val missing = events.count { it.type == DrivingEventType.MISSING_TURN_SIGNAL }
        return when {
            missing > 0 ->
                MissionResult.Failed("방향지시등 없이 조향한 구간이 있었어요 (${missing}번)")

            detector.countSignaledTurns(samples) == 0 ->
                MissionResult.Failed("방향지시등을 켜고 핸들을 돌려보세요")

            else -> MissionResult.Success("방향지시등을 잘 켰어요")
        }
    }

    /** 속도가 [from] 상태였다가 그 뒤에 [to] 상태가 된 적이 있는지 본다. */
    private fun speedWent(
        samples: List<DrivingSample>,
        from: (Double) -> Boolean,
        to: (Double) -> Boolean,
    ): Boolean {
        var fromSeen = false
        for (sample in samples) {
            val kmh = sample.speedKmh() ?: continue
            if (!fromSeen) {
                fromSeen = from(kmh)
            } else if (to(kmh)) {
                return true
            }
        }
        return false
    }

    /**
     * 후진으로 [REQUIRED_REVERSE_MS] 이상 움직였고 [MAX_REVERSE_KMH]를 넘지 않았는지 본다.
     *
     * 후진 기어로 움직인 구간이 연속으로 유지되어야 한다. 기어만 넣고 서 있거나
     * 잠깐 스친 것은 인정하지 않는다.
     */
    private fun judgeReverse(samples: List<DrivingSample>): MissionResult {
        var longestRunMs = 0L
        var runStartMs: Long? = null
        var maxKmh: Double? = null

        for (sample in samples) {
            val kmh = sample.speedKmh()
            if (sample.gear == Gear.REVERSE && kmh != null) {
                maxKmh = maxOf(maxKmh ?: 0.0, kmh)
            }
            if (sample.gear == Gear.REVERSE && kmh != null && !isStopped(kmh)) {
                val start = runStartMs ?: sample.timestampMs.also { runStartMs = it }
                longestRunMs = maxOf(longestRunMs, sample.timestampMs - start)
            } else {
                runStartMs = null
            }
        }

        val reversed = samples.any { it.gear == Gear.REVERSE }
        return when {
            reversed && maxKmh == null ->
                MissionResult.Insufficient("속도 신호를 받지 못했어요")

            maxKmh != null && maxKmh > MAX_REVERSE_KMH ->
                MissionResult.Failed("후진 속도가 빨랐어요 (최고 시속 ${"%.0f".format(maxKmh)}km)")

            longestRunMs < REQUIRED_REVERSE_MS ->
                MissionResult.Failed("후진으로 3초 이상 움직여 보세요")

            else -> MissionResult.Success("천천히 후진했어요")
        }
    }

    /**
     * [STEADY_MIN_KMH]~[STEADY_MAX_KMH]를 [REQUIRED_STEADY_MS] 이상 이어 간 구간의
     * 속도 표준편차가 [MAX_SPEED_STDDEV_KMH] 미만인지 본다.
     *
     * 세션 전체로 편차를 내면 안 된다. 연습은 정차 상태에서 시작해 정차로 끝나므로
     * 출발·정지 구간이 섞여 정속을 잘 지켜도 실패가 된다.
     *
     * 단위는 km/h로 환산해 판단한다. 사용자가 화면에서 보는 값과 기준을 맞추기 위함이다.
     */
    private fun judgeSteadySpeed(samples: List<DrivingSample>): MissionResult {
        if (samples.count { it.speedMps != null } < MIN_SAMPLES) {
            return MissionResult.Insufficient("속도 신호가 충분하지 않아요")
        }

        val bestStdDev = steadyRuns(samples).minOfOrNull(::stdDev)
            ?: return MissionResult.Failed("시속 15~25km를 5초 이상 유지해 보세요")

        return if (bestStdDev < MAX_SPEED_STDDEV_KMH) {
            MissionResult.Success("속도를 일정하게 유지했어요")
        } else {
            MissionResult.Failed("속도가 들쭉날쭉했어요 (편차 ${"%.1f".format(bestStdDev)}km/h)")
        }
    }

    /** 목표 속도 범위를 벗어나지 않고 [REQUIRED_STEADY_MS] 이상 이어진 구간들의 속도(km/h). */
    private fun steadyRuns(samples: List<DrivingSample>): List<List<Double>> {
        val runs = mutableListOf<List<Double>>()
        var run = mutableListOf<Double>()
        var runStartMs = 0L
        var runEndMs = 0L

        fun closeRun() {
            if (run.isNotEmpty() && runEndMs - runStartMs >= REQUIRED_STEADY_MS) runs += run
            run = mutableListOf()
        }

        for (sample in samples) {
            val kmh = sample.speedKmh()
            if (kmh == null || kmh < STEADY_MIN_KMH || kmh > STEADY_MAX_KMH) {
                closeRun()
                continue
            }
            if (run.isEmpty()) runStartMs = sample.timestampMs
            runEndMs = sample.timestampMs
            run += kmh
        }
        closeRun()

        return runs
    }

    /**
     * 달리다가 오른쪽 방향지시등을 켠 채 [REQUIRED_PAUSE_MS] 이상 완전히 멈추고,
     * 그 뒤에 움직이면서 핸들을 [Thresholds.MISSING_SIGNAL_STEER_DEG] 이상 돌렸는지 본다.
     *
     * 회전 방향은 조향각 부호로 가리지 않는다. 어느 쪽이 양수인지 실차에서 확인하지
     * 못했기 때문에 방향은 방향지시등으로만 확인한다.
     */
    private fun judgeRightTurnStop(samples: List<DrivingSample>): MissionResult {
        var approached = false
        var pauseStartMs: Long? = null
        var paused = false
        var steerAtPause: Float? = null
        var signaledRight = false

        for (sample in samples) {
            val kmh = sample.speedKmh()
            val right = sample.turnSignal == TurnSignal.RIGHT
            if (right) signaledRight = true

            if (paused) {
                val steer = sample.steeringDeg
                val reference = steerAtPause
                if (kmh != null && !isStopped(kmh) && steer != null && reference != null &&
                    abs(steer - reference) >= Thresholds.MISSING_SIGNAL_STEER_DEG
                ) {
                    return MissionResult.Success("멈춘 뒤에 우회전했어요")
                }
                continue
            }

            if (kmh != null && isMoving(kmh)) approached = true

            if (approached && right && kmh != null && isStopped(kmh)) {
                val start = pauseStartMs ?: sample.timestampMs.also { pauseStartMs = it }
                sample.steeringDeg?.let { steerAtPause = it }
                if (sample.timestampMs - start >= REQUIRED_PAUSE_MS) paused = true
            } else {
                pauseStartMs = null
                steerAtPause = null
            }
        }

        return when {
            paused -> MissionResult.Failed("멈춘 뒤에 핸들을 돌려 우회전까지 해 보세요")

            signaledRight -> MissionResult.Failed("우회전 전에 2초 이상 완전히 멈춰 보세요")

            else -> MissionResult.Failed("오른쪽 방향지시등을 켜고 멈춘 뒤 우회전해 보세요")
        }
    }

    private fun stdDev(values: List<Double>): Double {
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    /** 후진 중 속도가 음수로 올 때를 대비해 크기만 본다. */
    private fun DrivingSample.speedKmh(): Double? = speedMps?.let { abs(it) * MPS_TO_KMH }

    private fun isStopped(kmh: Double): Boolean = kmh < STOPPED_KMH

    private fun isMoving(kmh: Double): Boolean = kmh >= MOVING_KMH

    private companion object {
        const val MIN_SAMPLES = 2
        const val STOPPED_KMH = 1.0
        const val MOVING_KMH = 10.0
        const val REQUIRED_REVERSE_MS = 3_000L
        const val MAX_REVERSE_KMH = 10.0
        const val STEADY_MIN_KMH = 15.0
        const val STEADY_MAX_KMH = 25.0
        const val REQUIRED_STEADY_MS = 5_000L
        const val REQUIRED_PAUSE_MS = 2_000L
        const val MAX_SPEED_STDDEV_KMH = 3.0
        const val MPS_TO_KMH = 3.6
    }
}
