package ai.tireon.app.stats

import ai.tireon.core.report.DrivingStats

/**
 * 통계 화면 표시 문구.
 *
 * 문구를 화면이 아니라 여기서 만든다. JVM에서 시험할 수 있고, 문구가 바뀌어도
 * 화면 코드를 건드릴 필요가 없다.
 */
data class StatsDisplay(
    val hasStats: Boolean,
    val emptyMessage: String,
    val averageText: String?,
    val changeText: String?,
    val sessionText: String?,
    val eventText: String?,
    val distanceText: String?,
    val topEvents: List<ai.tireon.core.report.EventLine>,
) {
    companion object {

        fun from(
            stats: DrivingStats?,
            periodDays: Int,
            hasUnscoredRecords: Boolean = false,
        ): StatsDisplay {
            if (stats == null) {
                return StatsDisplay(
                    hasStats = false,
                    emptyMessage = if (hasUnscoredRecords) {
                        "최근 ${periodDays}일 기록은 있지만 점수를 낼 수 있는 주행이 없어요"
                    } else {
                        "최근 ${periodDays}일 기록이 없어요"
                    },
                    averageText = null,
                    changeText = null,
                    sessionText = null,
                    eventText = null,
                    distanceText = null,
                    topEvents = emptyList(),
                )
            }

            return StatsDisplay(
                hasStats = true,
                emptyMessage = "",
                averageText = stats.averageScore?.toString(),
                changeText = changeTextOf(stats.scoreChange),
                sessionText = "${stats.sessionCount}회",
                eventText = "${stats.totalEvents}회",
                distanceText = "%.1fkm".format(stats.totalDistanceKm),
                topEvents = stats.topEvents,
            )
        }

        private fun changeTextOf(change: Int?): String? = when {
            change == null -> null
            change > 0 -> "지난주보다 ${change}점 올랐어요"
            change < 0 -> "지난주보다 ${-change}점 내렸어요"
            else -> "지난주와 같아요"
        }
    }
}
