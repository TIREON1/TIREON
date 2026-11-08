package ai.tireon.core.analysis

import ai.tireon.core.DrivingScore
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.math.roundToInt

/** 이벤트 종류별 발생 횟수와 감점. */
data class EventContribution(
    val type: DrivingEventType,
    val count: Int,
    val penalty: Int,
)

sealed interface ScoreResult {
    data class Scored(
        val score: DrivingScore,
        val breakdown: List<EventContribution>,
    ) : ScoreResult

    data class Insufficient(val reason: InsufficientReason) : ScoreResult
}

/**
 * 점수를 만들지 못한 이유.
 *
 * 사유마다 사용자가 취할 행동이 다르므로 하나로 합치지 않는다.
 */
enum class InsufficientReason {
    DISTANCE_TOO_SHORT,
    DURATION_TOO_SHORT,
    NOT_ENOUGH_SAMPLES,
}

/**
 * 운전 습관 이벤트를 점수로 환산한다.
 *
 * 거리 1km당 이벤트 비율로 감점한다. 짧은 주행에서 이벤트 하나가 과도하게
 * 반영되지 않도록 거리의 하한을 1km로 둔다.
 */
class ScoreCalculator {

    fun calculate(
        events: List<DrivingEvent>,
        distanceKm: Float,
        durationSec: Int,
        sampleCount: Int,
    ): ScoreResult {
        // 차량 데이터는 신뢰할 수 없는 입력이다. NaN·무한대가 들어오면
        // 거리 비교가 통과해 NaN 감점으로 예외가 나거나 100점이 만들어질 수 있다.
        val usableDistance = distanceKm.isFinite() && distanceKm >= 0f
        if (!usableDistance || distanceKm < Thresholds.VALID_MIN_DISTANCE_KM) {
            return ScoreResult.Insufficient(InsufficientReason.DISTANCE_TOO_SHORT)
        }
        if (durationSec < Thresholds.VALID_MIN_DURATION_SEC) {
            return ScoreResult.Insufficient(InsufficientReason.DURATION_TOO_SHORT)
        }
        if (sampleCount < Thresholds.VALID_MIN_SAMPLES) {
            return ScoreResult.Insufficient(InsufficientReason.NOT_ENOUGH_SAMPLES)
        }

        val effectiveDistance = maxOf(distanceKm, Thresholds.SCORE_DISTANCE_FLOOR_KM)
        val counts = events.groupingBy { it.type }.eachCount()

        val breakdown = counts
            .map { (type, count) ->
                val rate = count / effectiveDistance
                EventContribution(
                    type = type,
                    count = count,
                    penalty = (weightOf(type) * rate).roundToInt(),
                )
            }
            .filter { it.count > 0 }
            .sortedWith(compareByDescending<EventContribution> { it.penalty }.thenBy { it.type })

        val totalPenalty = breakdown.sumOf { it.penalty }
        val value = (MAX_SCORE - totalPenalty).coerceIn(DrivingScore.MIN, DrivingScore.MAX)

        return ScoreResult.Scored(
            score = DrivingScore(value),
            breakdown = breakdown,
        )
    }

    private fun weightOf(type: DrivingEventType): Int = when (type) {
        DrivingEventType.HARSH_BRAKE -> Thresholds.W_HARSH_BRAKE
        DrivingEventType.HARSH_ACCEL -> Thresholds.W_HARSH_ACCEL
        DrivingEventType.HARSH_STEER -> Thresholds.W_HARSH_STEER
        DrivingEventType.MISSING_TURN_SIGNAL -> Thresholds.W_MISSING_SIGNAL
    }

    private companion object {
        const val MAX_SCORE = DrivingScore.MAX
    }
}
