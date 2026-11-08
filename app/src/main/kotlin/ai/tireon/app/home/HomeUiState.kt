package ai.tireon.app.home

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult

/**
 * 홈 화면에 표시할 상태.
 *
 * 문구를 화면이 아니라 상태에서 만든다. 문구 규칙을 JVM에서 시험하기 위함이다.
 *
 * @param scoreText 표시할 점수. 없으면 null이고 화면은 [statusText]만 보여준다.
 * @param statusText 점수 유무와 무관하게 항상 표시하는 안내 문구.
 */
data class HomeUiState(
    val scoreText: String?,
    val statusText: String,
    val hasScore: Boolean,
    /** 추천 미션. 근거가 없으면 null이다. */
    val recommendedMission: ai.tireon.core.mission.PracticeMission? = null,
    /** 추천 이유 문구. */
    val recommendReason: String? = null,
) {
    companion object {

        fun from(score: DrivingScore): HomeUiState = HomeUiState(
            scoreText = score.value.toString(),
            statusText = "오늘의 운전 점수",
            hasScore = true,
        )

        fun from(result: ScoreResult): HomeUiState = when (result) {
            is ScoreResult.Scored -> from(result.score)
            is ScoreResult.Insufficient -> HomeUiState(
                scoreText = null,
                statusText = messageOf(result.reason),
                hasScore = false,
            )
        }

        fun empty(): HomeUiState = HomeUiState(
            scoreText = null,
            statusText = "아직 주행 기록이 없어요",
            hasScore = false,
        )

        private fun messageOf(reason: InsufficientReason): String = when (reason) {
            InsufficientReason.DISTANCE_TOO_SHORT -> "조금 더 길게 주행하면 점수를 낼 수 있어요"
            InsufficientReason.DURATION_TOO_SHORT -> "1분 이상 주행하면 점수를 낼 수 있어요"
            InsufficientReason.NOT_ENOUGH_SAMPLES -> "차량 신호를 충분히 받지 못했어요"
        }
    }
}
