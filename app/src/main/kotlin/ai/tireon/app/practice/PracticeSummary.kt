package ai.tireon.app.practice

import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.coach.CoachMessage
import ai.tireon.core.coach.CoachMessages
import ai.tireon.core.mission.MissionResult
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
    /** 미션 달성 여부. 판정 불가면 null이다. */
    val missionSuccess: Boolean? = null,
    /** 미션 판정 상세 문구. */
    val missionDetail: String? = null,
    /** 측정 근거가 붙은 코칭 문장. 종료 음성은 이 중 필요한 피드백 하나만 읽는다. */
    val coachMessages: List<CoachMessage> = emptyList(),
) {
    companion object {

        fun from(
            result: ScoreResult,
            events: List<DrivingEvent>,
            missionResult: MissionResult? = null,
        ): PracticeSummary {
            val success = when (missionResult) {
                is MissionResult.Success -> true
                is MissionResult.Failed -> false
                else -> null
            }
            val detail = when (missionResult) {
                is MissionResult.Success -> missionResult.detail
                is MissionResult.Failed -> missionResult.detail
                is MissionResult.Insufficient -> missionResult.reason
                null -> null
            }

            return when (result) {
                is ScoreResult.Scored -> PracticeSummary(
                    scoreText = result.score.value.toString(),
                    message = "연습이 끝났어요",
                    eventCount = events.size,
                    missionSuccess = success,
                    missionDetail = detail,
                    coachMessages = CoachMessages().build(events, result),
                )

                // 부족 사유는 message가 이미 안내하므로 코칭 문장을 겹쳐 싣지 않는다.
                is ScoreResult.Insufficient -> PracticeSummary(
                    scoreText = null,
                    message = messageOf(result.reason),
                    eventCount = events.size,
                    missionSuccess = success,
                    missionDetail = detail,
                )
            }
        }

        private fun messageOf(reason: InsufficientReason): String = when (reason) {
            InsufficientReason.DISTANCE_TOO_SHORT -> "조금 더 길게 연습하면 점수를 낼 수 있어요"
            InsufficientReason.DURATION_TOO_SHORT -> "1분 이상 연습하면 점수를 낼 수 있어요"
            InsufficientReason.NOT_ENOUGH_SAMPLES -> "차량 신호를 충분히 받지 못했어요"
        }
    }
}
