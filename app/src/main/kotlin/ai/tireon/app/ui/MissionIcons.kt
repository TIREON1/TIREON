package ai.tireon.app.ui

import androidx.annotation.DrawableRes
import ai.tireon.app.R
import ai.tireon.core.mission.DrivingMission
import ai.tireon.core.mission.PracticeMission

/** 미션별 아이콘. 문구 매핑(MissionSelection)과 달리 화면 장식이라 UI 패키지에 둔다. */
internal object MissionIcons {

    @DrawableRes
    fun of(mission: PracticeMission): Int = when (mission) {
        PracticeMission.SMOOTH_START -> R.drawable.ic_trending_up
        PracticeMission.SMOOTH_STOP -> R.drawable.ic_trending_down
        PracticeMission.REVERSE_SENSE -> R.drawable.ic_reverse
        PracticeMission.TURN_SIGNAL -> R.drawable.ic_turn_signal
        PracticeMission.STEADY_SPEED -> R.drawable.ic_speed
    }

    @DrawableRes
    fun of(mission: DrivingMission?): Int = when (mission) {
        null -> R.drawable.ic_car
        DrivingMission.SMOOTH_DRIVING -> R.drawable.ic_trending_flat
        DrivingMission.SIGNAL_HABIT -> R.drawable.ic_turn_signal
        DrivingMission.STABLE_STEERING -> R.drawable.ic_steering
        DrivingMission.OVERALL -> R.drawable.ic_report
    }
}
