package ai.tireon.app.practice

import androidx.annotation.StringRes
import ai.tireon.app.R
import ai.tireon.core.mission.MissionCatalog
import ai.tireon.core.mission.PracticeMission

/** 화면에 표시할 미션 문구. */
data class MissionDisplay(
    val mission: PracticeMission,
    @StringRes val nameResId: Int,
    @StringRes val descriptionResId: Int,
)

/**
 * 미션과 화면 문구를 연결한다.
 *
 * 미션 정의는 `:core`에 있고 문구는 앱 리소스에 있다. 이 매핑이 그 둘을 잇는다.
 */
object MissionSelection {

    fun all(): List<MissionDisplay> = MissionCatalog.all().map(::of)

    fun of(mission: PracticeMission): MissionDisplay = when (mission) {
        PracticeMission.SMOOTH_START -> MissionDisplay(
            mission = mission,
            nameResId = R.string.mission_smooth_start_name,
            descriptionResId = R.string.mission_smooth_start_description,
        )

        PracticeMission.SMOOTH_STOP -> MissionDisplay(
            mission = mission,
            nameResId = R.string.mission_smooth_stop_name,
            descriptionResId = R.string.mission_smooth_stop_description,
        )

        PracticeMission.REVERSE_SENSE -> MissionDisplay(
            mission = mission,
            nameResId = R.string.mission_reverse_sense_name,
            descriptionResId = R.string.mission_reverse_sense_description,
        )

        PracticeMission.TURN_SIGNAL -> MissionDisplay(
            mission = mission,
            nameResId = R.string.mission_turn_signal_name,
            descriptionResId = R.string.mission_turn_signal_description,
        )

        PracticeMission.STEADY_SPEED -> MissionDisplay(
            mission = mission,
            nameResId = R.string.mission_steady_speed_name,
            descriptionResId = R.string.mission_steady_speed_description,
        )

        PracticeMission.RIGHT_TURN_STOP -> MissionDisplay(
            mission = mission,
            nameResId = R.string.mission_right_turn_stop_name,
            descriptionResId = R.string.mission_right_turn_stop_description,
        )
    }
}
