package ai.tireon.core.report

/**
 * 기간별 주행 통계.
 *
 * "TIREON을 쓰면 운전이 좋아진다"를 보여주려면 변화 추이가 필요하다. 최근 기간과
 * 직전 기간을 비교해 개선 여부를 판단한다.
 *
 * 점수가 없는 기록은 평균에서 제외한다. 0점으로 넣으면 평균이 왜곡된다.
 */
data class DrivingStats(
    /** 기간 내 세션 수. 점수 유무와 무관하게 센다. */
    val sessionCount: Int,
    /** 점수가 있는 세션의 평균. 점수가 하나도 없으면 null이다. */
    val averageScore: Int?,
    /** 이벤트 총 발생 횟수. */
    val totalEvents: Int,
    /** 이벤트 종류별 횟수. 많은 순으로 정렬한다. */
    val topEvents: List<EventLine>,
    /** 총 주행 거리(km). 거리 기록이 없으면 0이다. */
    val totalDistanceKm: Float,
    /** 직전 기간 대비 평균 점수 변화. 비교할 기록이 없으면 null이다. */
    val scoreChange: Int?,
    /** 개선 여부. [scoreChange]가 null이면 판단하지 않는다. */
    val improved: Boolean?,
) {
    companion object {
        private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        private val eventLabels = mapOf(
            "HARSH_BRAKE" to "급제동",
            "HARSH_ACCEL" to "급가속",
            "HARSH_STEER" to "급조향",
            "MISSING_TURN_SIGNAL" to "방향지시등 누락",
        )

        /**
         * @param records 세션 기록. 정렬 순서와 무관하다.
         * @param nowMs 기준 시각
         * @param periodDays 최근 기간. 이 기간과 직전 같은 길이의 기간을 비교한다.
         * @return 기간 내 점수 기록이 하나도 없으면 null.
         */
        fun from(
            records: List<DrivingSessionRecord>,
            nowMs: Long,
            periodDays: Int = 7,
        ): DrivingStats? {
            val periodMs = periodDays * MILLIS_PER_DAY
            val currentStart = nowMs - periodMs
            val previousStart = currentStart - periodMs

            val current = records.filter { it.startedAtMs in currentStart..nowMs }
            if (current.isEmpty()) return null

            val currentScores = current.mapNotNull { it.score }
            if (currentScores.isEmpty()) return null

            val previousScores = records
                .filter { it.startedAtMs in previousStart until currentStart }
                .mapNotNull { it.score }

            val average = currentScores.average().toInt()
            val previousAverage = previousScores.takeIf { it.isNotEmpty() }?.average()?.toInt()
            val change = previousAverage?.let { average - it }

            return DrivingStats(
                sessionCount = current.size,
                averageScore = average,
                totalEvents = current.sumOf { it.eventCounts.values.sum() },
                topEvents = toEventLines(current),
                totalDistanceKm = current.mapNotNull { it.distanceKm }.sum(),
                scoreChange = change,
                improved = change?.let { it > 0 },
            )
        }

        private fun toEventLines(records: List<DrivingSessionRecord>): List<EventLine> {
            val counts = mutableMapOf<String, Int>()
            records.forEach { record ->
                record.eventCounts.forEach { (type, count) ->
                    counts[type] = (counts[type] ?: 0) + count
                }
            }
            return counts.entries
                .mapNotNull { entry ->
                    eventLabels[entry.key]?.let { label -> EventLine(label, entry.value) }
                }
                .sortedByDescending { it.count }
        }
    }
}

/** 통계 화면에 표시할 이벤트 한 줄. */
data class EventLine(
    val label: String,
    val count: Int,
)
