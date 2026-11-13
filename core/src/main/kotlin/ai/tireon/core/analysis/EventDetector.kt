package ai.tireon.core.analysis

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.math.abs

/**
 * 주행 샘플 시퀀스에서 운전 습관 이벤트를 찾는다.
 *
 * 입력은 시간 오름차순으로 정렬된 샘플이라고 가정한다. 정렬은 수집 계층에서 보장한다.
 * 판정에 필요한 값이 없으면(null) 그 구간은 건너뛴다. 추정으로 채우지 않는다.
 *
 * 이벤트는 "동작 한 번"을 단위로 센다. 같은 급조향이 100ms 샘플 여러 개에 걸쳐
 * 나타나도 1회이며, 같은 회전 동작에서 방향지시등 누락도 1회다.
 * 이벤트 수가 그대로 감점이 되므로 중복 계산은 점수를 왜곡한다.
 */
class EventDetector {

    /**
     * @param includeOpenTurn 아직 끝나지 않은 회전 동작도 방향지시등 누락 판정에 넣을지.
     *   세션 종료 후 최종 판정은 true, 실시간 안내는 false를 쓴다. 회전 중에 켠
     *   방향지시등은 누락이 아니므로, 끝나지 않은 회전을 실시간으로 안내하면
     *   최종 결과와 다른 말을 하게 된다.
     */
    fun detect(
        samples: List<DrivingSample>,
        includeOpenTurn: Boolean = true,
    ): List<DrivingEvent> {
        if (samples.size < 2) return emptyList()

        val ordered = dropOutOfOrder(samples)
        if (ordered.size < 2) return emptyList()

        val events = buildList {
            addAll(detectAcceleration(ordered))
            addAll(detectHarshSteering(ordered))
            addAll(detectMissingTurnSignal(ordered, includeOpenTurn))
        }
        return events.sortedBy { it.atMs }
    }

    /**
     * 방향지시등을 켜고 한 회전 동작 수.
     *
     * 누락 이벤트가 없다는 것만으로는 회전을 했는지 알 수 없다. 연습 미션이
     * 실제로 동작이 있었는지 확인할 때 쓴다.
     */
    fun countSignaledTurns(samples: List<DrivingSample>): Int =
        scanTurnManeuvers(dropOutOfOrder(samples)).count { it.signaled }

    /**
     * 시간이 뒤로 가거나 같은 샘플을 버린다.
     *
     * 수집 계층의 버그나 콜백 중복으로 순서가 깨질 수 있고, 그 상태로 나눗셈을 하면
     * 음수 dt가 나와 가속도가 반대로 뒤집힌다.
     */
    private fun dropOutOfOrder(samples: List<DrivingSample>): List<DrivingSample> {
        val result = ArrayList<DrivingSample>(samples.size)
        var lastTimestamp = Long.MIN_VALUE
        for (sample in samples) {
            if (sample.timestampMs <= lastTimestamp) continue
            result += sample
            lastTimestamp = sample.timestampMs
        }
        return result
    }

    // region 가속도

    private fun detectAcceleration(samples: List<DrivingSample>): List<DrivingEvent> {
        val events = mutableListOf<DrivingEvent>()

        var runStartMs: Long? = null
        var runEndMs = 0L
        var runExtreme = 0f
        var runBrake = false

        fun closeRun() {
            val start = runStartMs ?: return
            val durationMs = runEndMs - start
            val type = when {
                runExtreme >= Thresholds.HARSH_ACCEL_MPS2 -> DrivingEventType.HARSH_ACCEL
                runExtreme <= Thresholds.HARSH_BRAKE_MPS2 && runBrake -> DrivingEventType.HARSH_BRAKE
                else -> null
            }
            if (type != null && durationMs >= Thresholds.EVENT_MIN_DURATION_MS) {
                events += DrivingEvent(
                    type = type,
                    atMs = start,
                    value = abs(runExtreme),
                    confidence = Confidence.MEASURED,
                )
            }
            runStartMs = null
            runExtreme = 0f
            runBrake = false
        }

        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]
            val gapMs = current.timestampMs - previous.timestampMs

            // 중복 샘플은 버리되 구간을 끊지 않는다. 끊으면 실제 이벤트가
            // 사라지거나 하나의 제동이 두 건으로 갈라진다.
            if (gapMs < Thresholds.MIN_SAMPLE_GAP_MS) continue

            // 실제 공백이거나 속도를 모르면 그 시점부터는 판정하지 않는다.
            if (gapMs > Thresholds.MAX_SAMPLE_GAP_MS ||
                previous.speedMps == null ||
                current.speedMps == null
            ) {
                closeRun()
                continue
            }

            val accel = smoothedAcceleration(samples, index)
            val overThreshold = accel >= Thresholds.HARSH_ACCEL_MPS2 ||
                accel <= Thresholds.HARSH_BRAKE_MPS2

            if (!overThreshold) {
                closeRun()
                continue
            }

            if (runStartMs == null) {
                // 가속도는 (previous, current) 구간의 값이므로 구간 시작점부터 센다.
                runStartMs = previous.timestampMs
                runExtreme = accel
                runBrake = previous.brakePressed == true || current.brakePressed == true
            } else {
                if (abs(accel) > abs(runExtreme)) runExtreme = accel
                if (current.brakePressed == true || previous.brakePressed == true) runBrake = true
            }
            runEndMs = current.timestampMs
        }
        closeRun()

        return events
    }

    /**
     * [index] 시점의 이동평균 가속도(m/s²).
     *
     * 창이 다 차기 전에는 확보된 쌍만으로 평균을 낸다. 창이 찰 때까지 기다리면
     * 문서에 정의된 500ms 지속 판정이 실제로는 더 길어져 이벤트를 놓친다.
     */
    private fun smoothedAcceleration(samples: List<DrivingSample>, index: Int): Float {
        var sum = 0f
        var count = 0
        for (back in 0 until Thresholds.SMOOTHING_WINDOW) {
            val current = index - back
            if (current < 1) break

            // 후진 중 차속은 음수로 온다(AAOS PERF_VEHICLE_SPEED). 부호를 그대로 쓰면
            // 후진 중 제동이 급가속으로 뒤집히므로 크기만 본다.
            val v1 = samples[current - 1].speedMps?.let(::abs) ?: continue
            val v2 = samples[current].speedMps?.let(::abs) ?: continue
            val stepMs = samples[current].timestampMs - samples[current - 1].timestampMs
            if (stepMs < Thresholds.MIN_SAMPLE_GAP_MS || stepMs > Thresholds.MAX_SAMPLE_GAP_MS) continue

            sum += (v2 - v1) / (stepMs / 1000f)
            count++
        }
        return if (count == 0) 0f else sum / count
    }

    // endregion

    // region 급조향

    /**
     * 속도와 조향을 모두 알 때만 조향각 변화율을 판정한다.
     *
     * 연속된 구간은 하나의 조향 동작으로 묶는다. 250도/초로 400ms 돌린 것을
     * 4회로 세면 감점이 4배가 되기 때문이다. 한 샘플짜리 스파이크도 동작으로
     * 보지 않는다.
     */
    private fun detectHarshSteering(samples: List<DrivingSample>): List<DrivingEvent> {
        val events = mutableListOf<DrivingEvent>()

        var runStartMs: Long? = null
        var runEndMs = 0L
        var runMaxRate = 0f
        var runPairs = 0
        var runDirection = 0

        fun closeRun() {
            val start = runStartMs
            if (start != null && runPairs >= Thresholds.STEER_MIN_CONFIRMING_PAIRS) {
                events += DrivingEvent(
                    type = DrivingEventType.HARSH_STEER,
                    atMs = start,
                    value = runMaxRate,
                    confidence = Confidence.MEASURED,
                )
            }
            runStartMs = null
            runMaxRate = 0f
            runPairs = 0
            runDirection = 0
        }

        for (index in 1 until samples.size) {
            val previous = samples[index - 1]
            val current = samples[index]

            val steerBefore = previous.steeringDeg
            val steerAfter = current.steeringDeg
            val speed = current.speedMps ?: previous.speedMps
            val dtMs = current.timestampMs - previous.timestampMs

            val calculable = steerBefore != null && steerAfter != null &&
                speed != null && speed > Thresholds.HARSH_STEER_MIN_SPEED_MPS &&
                dtMs in Thresholds.MIN_SAMPLE_GAP_MS..Thresholds.MAX_SAMPLE_GAP_MS

            if (!calculable) {
                closeRun()
                continue
            }

            val angleRate = abs(steerAfter - steerBefore) / (dtMs / 1000f)
            if (angleRate > Thresholds.HARSH_STEER_DEG_PER_SEC) {
                // 조향 방향이 뒤집히면 별개의 동작이다. 0→60→0 같은 한 샘플 왕복을
                // 하나의 동작으로 이어 붙이면 스파이크가 이벤트로 남는다.
                val delta = steerAfter - steerBefore
                val currentDirection = if (delta > 0f) 1 else -1
                val start = runStartMs
                if (start == null || previous.timestampMs != runEndMs ||
                    currentDirection != runDirection
                ) {
                    closeRun()
                    runStartMs = previous.timestampMs
                    runMaxRate = angleRate
                    runPairs = 1
                    runDirection = currentDirection
                } else {
                    runMaxRate = maxOf(runMaxRate, angleRate)
                    runPairs++
                }
                runEndMs = current.timestampMs
            } else {
                closeRun()
            }
        }
        closeRun()

        return events
    }

    // endregion

    // region 방향지시등 누락

    /**
     * 조향이 연속된 구간을 "회전 동작 한 번"으로 보고 방향지시등을 확인한다.
     *
     * - 동작은 기준점 대비 [Thresholds.MISSING_SIGNAL_STEER_DEG] 이상 벌어지면 시작된다.
     * - 회전 직전([Thresholds.SIGNAL_LEAD_WINDOW_MS] 이내)이나 회전 중에 한 번이라도
     *   방향지시등을 켰다면 누락으로 세지 않는다. 회전 중 자동으로 꺼지는 것은
     *   정상 동작이라 처벌하면 안 된다.
     * - 동작이 끝나면(조향이 기준점 근처로 돌아오거나 방향을 바꿈) 1회로 확정하고
     *   회전 전체의 최대 변화량을 근거로 남긴다.
     * - 주차·후진 중 조향은 도로 주행이 아니므로 판정하지 않는다.
     */
    private fun detectMissingTurnSignal(
        samples: List<DrivingSample>,
        includeOpenTurn: Boolean,
    ): List<DrivingEvent> =
        scanTurnManeuvers(samples, includeOpenTurn)
            .filter { !it.signaled && !it.signalUnknown }
            .map {
                DrivingEvent(
                    type = DrivingEventType.MISSING_TURN_SIGNAL,
                    atMs = it.startMs,
                    value = it.peakDeg,
                    confidence = Confidence.MEASURED,
                )
            }

    private fun scanTurnManeuvers(
        samples: List<DrivingSample>,
        includeOpenTurn: Boolean = true,
    ): List<TurnManeuver> {
        val maneuvers = mutableListOf<TurnManeuver>()

        var anchor: Float? = null
        var maneuverStartMs = 0L
        var maneuverPeak = 0f
        var maneuverActive = false
        var signaled = false
        var signalUnknown = false
        var lastSignalMs: Long? = null
        var lastUnknownMs: Long? = null
        var direction = 0

        fun isRecent(atMs: Long?, nowMs: Long): Boolean =
            atMs != null && nowMs - atMs <= Thresholds.SIGNAL_LEAD_WINDOW_MS

        fun closeManeuver() {
            if (maneuverActive) {
                maneuvers += TurnManeuver(
                    startMs = maneuverStartMs,
                    peakDeg = maneuverPeak,
                    signaled = signaled,
                    signalUnknown = signalUnknown,
                )
            }
            maneuverActive = false
            maneuverPeak = 0f
            signaled = false
            signalUnknown = false
            direction = 0
        }

        fun resetSegment() {
            closeManeuver()
            anchor = null
            lastSignalMs = null
            lastUnknownMs = null
        }

        for (sample in samples) {
            val steer = sample.steeringDeg
            if (steer == null) {
                resetSegment()
                continue
            }

            if (!isRoadDriving(sample)) {
                resetSegment()
                continue
            }

            when (sample.turnSignal) {
                TurnSignal.LEFT, TurnSignal.RIGHT -> {
                    lastSignalMs = sample.timestampMs
                    if (maneuverActive) signaled = true
                }
                TurnSignal.OFF -> Unit
                null -> {
                    lastUnknownMs = sample.timestampMs
                    if (maneuverActive) signalUnknown = true
                }
            }

            val reference = anchor
            if (reference == null) {
                anchor = steer
                continue
            }

            val delta = steer - reference
            val magnitude = abs(delta)
            val currentDirection = when {
                delta > 0f -> 1
                delta < 0f -> -1
                else -> 0
            }

            if (!maneuverActive) {
                if (magnitude >= Thresholds.MISSING_SIGNAL_STEER_DEG) {
                    maneuverActive = true
                    maneuverStartMs = sample.timestampMs
                    signaled = isRecent(lastSignalMs, sample.timestampMs)
                    signalUnknown = isRecent(lastUnknownMs, sample.timestampMs)
                    maneuverPeak = magnitude
                    direction = currentDirection
                }
                continue
            }

            // 방향이 바뀌면 조향 동작이 끝난 것으로 본다.
            if (currentDirection != 0 && direction != 0 && currentDirection != direction) {
                closeManeuver()
                anchor = steer
                continue
            }

            maneuverPeak = maxOf(maneuverPeak, magnitude)

            // 기준점 근처로 돌아오면 동작 종료.
            if (magnitude < Thresholds.MISSING_SIGNAL_STEER_DEG) {
                closeManeuver()
                anchor = steer
            }
        }
        if (includeOpenTurn) closeManeuver()

        return maneuvers
    }

    /** 회전 동작 한 번과 그 구간의 방향지시등 상태. */
    private data class TurnManeuver(
        val startMs: Long,
        val peakDeg: Float,
        val signaled: Boolean,
        val signalUnknown: Boolean,
    )

    /** 주차·후진·저속은 도로 주행이 아니므로 방향지시등을 요구하지 않는다. */
    private fun isRoadDriving(sample: DrivingSample): Boolean {
        if (sample.gear == Gear.PARK || sample.gear == Gear.REVERSE) return false
        val speed = sample.speedMps ?: return false
        return speed > 0f
    }

    // endregion
}
