package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.EventLine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 세션 기록을 상세 화면 표시용으로 바꾼다.
 *
 * 저장된 값과 표시 문구를 분리한다. 화면은 이 모델만 그리면 되고, 문구 규칙은
 * JVM에서 시험할 수 있다.
 */
data class ReportDetail(
    val scoreText: String?,
    val hasScore: Boolean,
    val scoreReason: String?,
    val dateText: String,
    val durationText: String,
    val distanceText: String,
    val events: List<EventLine>,
    val missionLabel: String?,
    val missionDetail: String?,
    /** 미션 달성 여부. [missionLabel]의 아이콘과 색을 고르는 데 쓴다. */
    val missionSuccess: Boolean? = null,
    /** 연습인지 주행인지와 고른 미션. */
    val kind: SessionKind? = null,
) {
    companion object {

        private val eventLabels = mapOf(
            "HARSH_BRAKE" to "급제동",
            "HARSH_ACCEL" to "급가속",
            "HARSH_STEER" to "급조향",
            "MISSING_TURN_SIGNAL" to "방향지시등 누락",
        )

        private const val UNAVAILABLE = "사용할 수 없음"
        private const val PENDING = "판정 보류"

        fun from(record: DrivingSessionRecord): ReportDetail = ReportDetail(
            scoreText = record.score?.toString(),
            hasScore = record.score != null,
            scoreReason = if (record.score == null) {
                reasonOf(record.insufficientReason)
            } else {
                null
            },
            dateText = formatDate(record.startedAtMs),
            durationText = formatDuration(record.durationSec),
            distanceText = record.distanceKm?.let { "%.1fkm".format(it) } ?: UNAVAILABLE,
            events = toEventLines(record.eventCounts),
            missionLabel = missionLabelOf(record),
            missionDetail = record.missionDetail,
            missionSuccess = record.missionSuccess,
            kind = SessionKind.of(record),
        )

        /**
         * 주행 미션은 성공·실패를 가르지 않아 [DrivingSessionRecord.missionSuccess]가 null이다.
         * 그래서 요약이 있으면 "주행 결과"로, 판정을 보류했으면 "판정 보류"로 나눠 보여준다.
         * 보류를 0회와 같은 정상 결과로 보이지 않게 하기 위해서다. 연습 미션은 판정했으면
         * 성공·실패가 있으므로, null인데 문구가 있으면 보류다. 버전 2 이하 기록은
         * [DrivingSessionRecord.missionInsufficient]가 없어 이 규칙으로 구분한다.
         */
        private fun missionLabelOf(record: DrivingSessionRecord): String? = when (record.missionSuccess) {
            true -> "목표 달성"
            false -> "다시 해볼까요"
            null -> when {
                record.missionDetail == null -> null
                record.missionInsufficient -> PENDING
                SessionKind.of(record) is SessionKind.Driving -> "주행 결과"
                else -> PENDING
            }
        }

        private fun reasonOf(reason: String?): String = when (reason) {
            "DISTANCE_TOO_SHORT" -> "조금 더 길게 주행하면 점수를 낼 수 있어요"
            "DURATION_TOO_SHORT" -> "1분 이상 주행하면 점수를 낼 수 있어요"
            "NOT_ENOUGH_SAMPLES" -> "차량 신호를 충분히 받지 못했어요"
            else -> "데이터가 부족해 점수를 내지 못했어요"
        }

        private fun toEventLines(counts: Map<String, Int>): List<EventLine> =
            counts.entries
                .mapNotNull { entry ->
                    eventLabels[entry.key]?.let { label -> EventLine(label, entry.value) }
                }
                .sortedByDescending { it.count }

        private fun formatDuration(totalSec: Int): String {
            val minutes = totalSec / 60
            val seconds = totalSec % 60
            return if (minutes == 0) "${seconds}초" else "${minutes}분 ${seconds}초"
        }

        private fun formatDate(timestampMs: Long): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN)
            return formatter.format(Date(timestampMs))
        }
    }
}
