package ai.tireon.core.analysis

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.TurnSignal
import kotlin.math.abs

/**
 * 주행 샘플 시퀀스에서 운전 습관 이벤트를 찾는다.
 *
 * 입력은 시간 오름차순으로 정렬된 샘플이라고 가정한다. 정렬은 수집 계층에서 보장한다.
 * 판정에 필요한 값이 없으면(null) 그 구간은 건너뛴다. 추정으로 채우지 않는다.
 */
class EventDetector {

    fun detect(samples: List<DrivingSample>): List<DrivingEvent> {
        if (samples.size < 2) return emptyList()

        val events = buildList {
            addAll(detectAcceleration(samples))
            addAll(detectHarshSteering(samples))
            addAll(detectMissingTurnSignal(samples))
        }
        return events.sortedBy { it.atMs }
    }

    /**
     * 속도와 조향을 모두 알 때만 조향각 변화율을 판정한다.
     *
     * 연속된 구간은 하나의 조향 동작으로 묶는다. 250도/초로 400ms 돌린 것을
     * 4회로 세면 감점이 4배가 되기 때문이다.
     */
    private fun detectHarshSteering(samples: List<DrivingSample>): List<DrivingEvent> {
        val events = mutableListOf<DrivingEvent>()

        var runStartMs: Long? = null
        var runEndMs = 0L
        var runMaxRate = 0f

        fun closeRun() {
            val start = runStartMs ?: return
            events += DrivingEvent(
                type = DrivingEventType.HARSH_STEER,
                atMs = start,
                value = runMaxRate,
                confidence = Confidence.MEASURED,
            )
            runStartMs = null
            runMaxRate = 0f
        }

        for (i in 1 until samples.size) {
            val previous = samples[i - 1]
            val current = samples[i]

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
                val start = runStartMs
                if (start != null && previous.timestampMs == runEndMs) {
                    runMaxRate = maxOf(runMaxRate, angleRate)
                } else {
                    closeRun()
                    runStartMs = previous.timestampMs
                    runMaxRate = angleRate
                }
                runEndMs = current.timestampMs
            } else {
                closeRun()
            }
        }
        closeRun()

        return events
    }

    /**
     * 조향이 연속된 구간마다 기준점을 잡고, 기준점 대비 30도 이상 벌어지는 순간
     * 그 구간의 방향지시등이 계속 OFF였는지 확인한다.
     *
     * 한 번 기록하면 기준점을 현재 값으로 옮겨 같은 조향 동작을 중복 계산하지 않는다.
     */
    private fun detectMissingTurnSignal(samples: List<DrivingSample>): List<DrivingEvent> {
        val events = mutableListOf<DrivingEvent>()

        var anchorSteer: Float? = null
        var signalOnInSegment = false
        var signalUnknownInSegment = false

        fun resetSegment() {
            anchorSteer = null
            signalOnInSegment = false
            signalUnknownInSegment = false
        }

        for (sample in samples) {
            val steer = sample.steeringDeg
            if (steer == null) {
                resetSegment()
                continue
            }
            if (anchorSteer == null) {
                anchorSteer = steer
                signalOnInSegment = false
                signalUnknownInSegment = false
            }

            when (sample.turnSignal) {
                TurnSignal.LEFT, TurnSignal.RIGHT -> signalOnInSegment = true
                TurnSignal.OFF -> Unit
                null -> signalUnknownInSegment = true
            }

            val anchor = anchorSteer ?: continue
            if (abs(steer - anchor) >= Thresholds.MISSING_SIGNAL_STEER_DEG) {
                if (!signalOnInSegment && !signalUnknownInSegment) {
                    events += DrivingEvent(
                        type = DrivingEventType.MISSING_TURN_SIGNAL,
                        atMs = sample.timestampMs,
                        value = abs(steer - anchor),
                        confidence = Confidence.MEASURED,
                    )
                }
                anchorSteer = steer
                signalOnInSegment = false
                signalUnknownInSegment = false
            }
        }
        return events
    }

    private fun detectAcceleration(samples: List<DrivingSample>): List<DrivingEvent> {
        val events = mutableListOf<DrivingEvent>()

        // 연속 유효 구간을 모은다. 결측이나 간격 이탈이 나오면 구간을 끊는다.
        var segment = mutableListOf<AccelPoint>()
        var lastTimestamp = Long.MIN_VALUE

        fun flushSegment() {
            events += judgeAcceleration(segment)
            segment = mutableListOf()
        }

        for (sample in samples) {
            val previousTs = lastTimestamp
            lastTimestamp = sample.timestampMs

            val speed = sample.speedMps
            if (speed == null || previousTs == Long.MIN_VALUE) {
                flushSegment()
                if (speed != null) segment += AccelPoint(sample.timestampMs, 0f, sample.brakePressed)
                continue
            }

            val gapMs = sample.timestampMs - previousTs
            if (gapMs < Thresholds.MIN_SAMPLE_GAP_MS || gapMs > Thresholds.MAX_SAMPLE_GAP_MS) {
                flushSegment()
                continue
            }

            segment += AccelPoint(sample.timestampMs, 0f, sample.brakePressed)
            if (segment.size >= Thresholds.SMOOTHING_WINDOW) {
                val accel = smoothedAcceleration(samples, sample.timestampMs)
                segment[segment.lastIndex] = segment.last().copy(accelMps2 = accel)
            }
        }
        flushSegment()

        return events
    }

    private fun smoothedAcceleration(samples: List<DrivingSample>, atMs: Long): Float {
        val index = samples.indexOfFirst { it.timestampMs == atMs }
        if (index < 1) return 0f

        var sum = 0f
        var count = 0
        for (i in maxOf(1, index - Thresholds.SMOOTHING_WINDOW + 1)..index) {
            val v1 = samples[i - 1].speedMps ?: continue
            val v2 = samples[i].speedMps ?: continue
            val dtMs = samples[i].timestampMs - samples[i - 1].timestampMs
            if (dtMs < Thresholds.MIN_SAMPLE_GAP_MS || dtMs > Thresholds.MAX_SAMPLE_GAP_MS) continue
            sum += (v2 - v1) / (dtMs / 1000f)
            count++
        }
        return if (count == 0) 0f else sum / count
    }

    /**
     * 한 구간에서 급가속/급제동을 판정한다.
     *
     * 500ms 이상 지속된 구간만 이벤트로 기록하고, 한 구간에서 한 번만 센다.
     * 급제동은 브레이크 페달이 함께 눌린 경우에만 인정한다.
     */
    private fun judgeAcceleration(segment: List<AccelPoint>): List<DrivingEvent> {
        if (segment.size < 2) return emptyList()

        val events = mutableListOf<DrivingEvent>()
        var runStart: AccelPoint? = null
        var runMax = 0f
        var runBrake = false

        fun closeRun(endMs: Long) {
            val start = runStart ?: return
            val durationMs = endMs - start.timestampMs
            val magnitude = abs(runMax)
            val type = when {
                runMax >= Thresholds.HARSH_ACCEL_MPS2 -> DrivingEventType.HARSH_ACCEL
                runMax <= Thresholds.HARSH_BRAKE_MPS2 && runBrake -> DrivingEventType.HARSH_BRAKE
                else -> null
            }
            if (type != null && durationMs >= Thresholds.EVENT_MIN_DURATION_MS) {
                events += DrivingEvent(
                    type = type,
                    atMs = start.timestampMs,
                    value = magnitude,
                    confidence = Confidence.MEASURED,
                )
            }
            runStart = null
            runMax = 0f
            runBrake = false
        }

        for (point in segment) {
            val overThreshold = point.accelMps2 >= Thresholds.HARSH_ACCEL_MPS2 ||
                point.accelMps2 <= Thresholds.HARSH_BRAKE_MPS2

            if (!overThreshold) {
                closeRun(point.timestampMs)
                continue
            }

            if (runStart == null) {
                runStart = point
                runMax = point.accelMps2
                runBrake = point.brakePressed == true
            } else {
                if (abs(point.accelMps2) > abs(runMax)) runMax = point.accelMps2
                if (point.brakePressed == true) runBrake = true
            }
        }
        closeRun(segment.last().timestampMs)

        return events
    }

    private data class AccelPoint(
        val timestampMs: Long,
        val accelMps2: Float,
        val brakePressed: Boolean?,
    )
}
