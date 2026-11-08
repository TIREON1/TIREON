package ai.tireon.app.practice

import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.DrivingEvent

/**
 * 연습 세션 요약.
 *
 * @param scoreText 표시할 점수. 데이터가 부족하면 null이고 화면은 [message]만 보여준다.
 */
data class PracticeSummary(
    val scoreText: String?,
    val message: String,
    val eventCount: Int,
) {
    companion object {

        fun from(result: ScoreResult, events: List<DrivingEvent>): PracticeSummary =
            when (result) {
                is ScoreResult.Scored -> PracticeSummary(
                    scoreText = result.score.value.toString(),
                    message = "연습이 끝났어요",
                    eventCount = events.size,
                )

                is ScoreResult.Insufficient -> PracticeSummary(
                    scoreText = null,
                    message = messageOf(result.reason),
                    eventCount = events.size,
                )
            }

        private fun messageOf(reason: InsufficientReason): String = when (reason) {
            InsufficientReason.DISTANCE_TOO_SHORT -> "조금 더 길게 연습하면 점수를 낼 수 있어요"
            InsufficientReason.DURATION_TOO_SHORT -> "1분 이상 연습하면 점수를 낼 수 있어요"
            InsufficientReason.NOT_ENOUGH_SAMPLES -> "차량 신호를 충분히 받지 못했어요"
        }
    }
}
