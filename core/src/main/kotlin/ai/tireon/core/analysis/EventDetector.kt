package ai.tireon.core.analysis

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
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

        val accelerationEvents = detectAcceleration(samples)
        return accelerationEvents.sortedBy { it.atMs }
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
