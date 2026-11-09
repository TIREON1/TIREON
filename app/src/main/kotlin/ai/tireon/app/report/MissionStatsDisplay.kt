package ai.tireon.app.report

import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.MissionStats

/**
 * 미션별 성적 표시 문구.
 *
 * 문구를 화면이 아니라 여기서 만든다. JVM에서 시험할 수 있고, 문구가 바뀌어도
 * 화면 코드를 건드릴 필요가 없다.
 */
data class MissionStatsDisplay(
    val hasStats: Boolean,
    val emptyMessage: String,
    val hardestText: String?,
    val rows: List<MissionRow>,
) {

    /** 미션 한 줄. */
    data class MissionRow(
        val name: String,
        val rateText: String,
        val countText: String,
    )

    companion object {

        /** 가장 어려운 미션을 알려주는 문구. */
        fun from(records: List<ai.tireon.core.report.DrivingSessionRecord>): MissionStatsDisplay {
            val scores = MissionStats.byMission(records)
            if (scores.isEmpty()) {
                return MissionStatsDisplay(
                    hasStats = false,
                    emptyMessage = "아직 미션 기록이 없어요",
                    hardestText = null,
                    rows = emptyList(),
                )
            }

            val hardest = MissionStats.hardest(records)
            return MissionStatsDisplay(
                hasStats = true,
                emptyMessage = "",
                hardestText = hardest?.let { score ->
                    "${nameOf(score.mission)} 연습이 필요해요"
                },
                rows = scores.map { score ->
                    MissionRow(
                        name = nameOf(score.mission),
                        rateText = "${score.successRate}%",
                        countText = "${score.successCount}번 성공 / ${score.total}번 시도",
                    )
                },
            )
        }

        /** 미션 이름. 화면에 그대로 쓰는 말이다. */
        fun nameOf(mission: PracticeMission): String = when (mission) {
            PracticeMission.SMOOTH_START -> "부드러운 출발"
            PracticeMission.SMOOTH_STOP -> "부드러운 정지"
            PracticeMission.REVERSE_SENSE -> "후진 감각"
            PracticeMission.TURN_SIGNAL -> "깜빡이 습관"
            PracticeMission.STEADY_SPEED -> "정속 유지"
        }
    }
}
