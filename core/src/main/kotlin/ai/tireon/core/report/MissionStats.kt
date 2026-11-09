package ai.tireon.core.report

import ai.tireon.core.mission.PracticeMission

/** 미션 하나의 성적. */
data class MissionScore(
    val mission: PracticeMission,
    val successCount: Int,
    val failureCount: Int,
) {
    val total: Int
        get() = successCount + failureCount

    /** 성공률(%). 시도가 없으면 0이다. */
    val successRate: Int
        get() = if (total == 0) 0 else successCount * 100 / total
}

/**
 * 미션별 성공률 계산.
 *
 * "어떤 미션을 자주 실패하는지"를 알아야 다음 연습을 제안할 수 있다.
 *
 * 판정 불가(`missionSuccess == null`)는 성공도 실패도 아니므로 제외한다.
 * 이걸 실패로 세면 신호가 없었을 뿐인 세션이 실패로 기록된다.
 */
object MissionStats {

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

    /** 가장 어려운 미션으로 판단할 최소 시도 횟수. */
    private const val MIN_ATTEMPTS = 2

    /**
     * @param records 세션 기록. 정렬 순서와 무관하다.
     * @param nowMs 기준 시각
     * @param periodDays 집계 기간
     * @return 시도가 많은 순으로 정렬된 목록.
     */
    fun byMission(
        records: List<DrivingSessionRecord>,
        nowMs: Long? = null,
        periodDays: Int = 7,
    ): List<MissionScore> {
        val filtered = filterByPeriod(records, nowMs, periodDays)

        val grouped = filtered
            .mapNotNull { record ->
                val mission = record.mission?.let { name ->
                    runCatching { PracticeMission.valueOf(name) }.getOrNull()
                } ?: return@mapNotNull null
                val success = record.missionSuccess ?: return@mapNotNull null
                mission to success
            }
            .groupBy({ it.first }, { it.second })

        return grouped.map { (mission, results) ->
            MissionScore(
                mission = mission,
                successCount = results.count { it },
                failureCount = results.count { !it },
            )
        }.sortedByDescending { it.total }
    }

    /**
     * 가장 어려운 미션.
     *
     * 시도가 [MIN_ATTEMPTS] 미만인 미션은 제외한다. 한 번 실패한 것만으로
     * "가장 어렵다"고 말하면 우연을 근거로 삼는 셈이다.
     *
     * @return 성공률이 가장 낮은 미션. 해당하는 미션이 없으면 null.
     */
    fun hardest(records: List<DrivingSessionRecord>): MissionScore? =
        byMission(records)
            .filter { it.total >= MIN_ATTEMPTS && it.failureCount > 0 }
            .minByOrNull { it.successRate }

    private fun filterByPeriod(
        records: List<DrivingSessionRecord>,
        nowMs: Long?,
        periodDays: Int,
    ): List<DrivingSessionRecord> {
        if (nowMs == null) return records
        val start = nowMs - periodDays * MILLIS_PER_DAY
        return records.filter { it.startedAtMs in start..nowMs }
    }
}
