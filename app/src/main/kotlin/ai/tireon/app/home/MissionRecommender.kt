package ai.tireon.app.home

import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.DrivingSessionRecord

/**
 * 최근 기록에서 연습할 미션을 고른다.
 *
 * 가장 많이 발생한 이벤트를 줄일 수 있는 미션을 추천한다. 기록이 없거나 이벤트가
 * 없으면 추천하지 않는다. 근거 없이 아무 미션이나 권하지 않기 위함이다.
 */
object MissionRecommender {

    /** 추천에 사용할 최근 기록 수. */
    private const val RECENT_LIMIT = 10

    private val labelOf = mapOf(
        "HARSH_ACCEL" to "급가속",
        "HARSH_BRAKE" to "급제동",
        "HARSH_STEER" to "급조향",
        "MISSING_TURN_SIGNAL" to "방향지시등 누락",
    )

    /**
     * @param records 세션 기록. 정렬 순서와 무관하게 최신 [RECENT_LIMIT]개만 본다.
     * @return 추천 미션. 근거가 없으면 null.
     */
    fun recommend(records: List<DrivingSessionRecord>): PracticeMission? {
        val top = topEvent(records) ?: return null
        return missionFor(top.first)
    }

    /** @return 추천 이유 문구. 근거가 없으면 null. */
    fun reasonFor(records: List<DrivingSessionRecord>): String? {
        val top = topEvent(records) ?: return null
        val label = labelOf[top.first] ?: return null
        return "최근 ${label}이 ${top.second}번 있었어요"
    }

    private fun topEvent(records: List<DrivingSessionRecord>): Pair<String, Int>? {
        val counts = mutableMapOf<String, Int>()
        records
            .sortedByDescending { it.startedAtMs }
            .take(RECENT_LIMIT)
            .forEach { record ->
            record.eventCounts.forEach { (type, count) ->
                counts[type] = (counts[type] ?: 0) + count
            }
        }
        return counts.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.toPair()
    }

    /**
     * 이벤트를 줄이는 데 가장 직접적인 미션을 고른다.
     *
     * 급조향은 방향지시등 습관과 함께 연습하는 편이 효과적이라 깜빡이 미션으로
     * 묶는다.
     */
    private fun missionFor(eventType: String): PracticeMission = when (eventType) {
        "HARSH_ACCEL" -> PracticeMission.SMOOTH_START
        "HARSH_BRAKE" -> PracticeMission.SMOOTH_STOP
        "HARSH_STEER" -> PracticeMission.TURN_SIGNAL
        "MISSING_TURN_SIGNAL" -> PracticeMission.TURN_SIGNAL
        else -> PracticeMission.SMOOTH_START
    }
}
