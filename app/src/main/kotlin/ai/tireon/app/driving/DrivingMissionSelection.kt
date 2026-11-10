package ai.tireon.app.driving

import ai.tireon.app.R
import ai.tireon.core.mission.DrivingMission
import ai.tireon.core.mission.DrivingMissionCatalog
import androidx.annotation.StringRes

/** 화면에 표시할 주행 미션 문구. */
data class DrivingMissionDisplay(
    val mission: DrivingMission,
    @StringRes val nameResId: Int,
    @StringRes val descriptionResId: Int,
)

/**
 * 주행 미션과 화면 문구를 연결한다.
 *
 * 미션 정의는 `:core`에 있고 문구는 앱 리소스에 있다. 이 매핑이 그 둘을 잇는다.
 * 연습 미션의 [ai.tireon.app.practice.MissionSelection]과 같은 역할이다.
 */
object DrivingMissionSelection {

    fun all(): List<DrivingMissionDisplay> = DrivingMissionCatalog.all().map { of(it.mission) }

    fun of(mission: DrivingMission): DrivingMissionDisplay = when (mission) {
        DrivingMission.SMOOTH_DRIVING -> DrivingMissionDisplay(
            mission = mission,
            nameResId = R.string.driving_mission_smooth_driving_name,
            descriptionResId = R.string.driving_mission_smooth_driving_description,
        )

        DrivingMission.SIGNAL_HABIT -> DrivingMissionDisplay(
            mission = mission,
            nameResId = R.string.driving_mission_signal_habit_name,
            descriptionResId = R.string.driving_mission_signal_habit_description,
        )

        DrivingMission.STABLE_STEERING -> DrivingMissionDisplay(
            mission = mission,
            nameResId = R.string.driving_mission_stable_steering_name,
            descriptionResId = R.string.driving_mission_stable_steering_description,
        )

        DrivingMission.OVERALL -> DrivingMissionDisplay(
            mission = mission,
            nameResId = R.string.driving_mission_overall_name,
            descriptionResId = R.string.driving_mission_overall_description,
        )
    }

    /** 미션을 고르지 않고 주행하는 선택지의 이름. */
    @StringRes
    fun skipNameResId(): Int = R.string.driving_mission_skip_name

    /** 미션을 고르지 않고 주행하는 선택지의 설명. */
    @StringRes
    fun skipDescriptionResId(): Int = R.string.driving_mission_skip_description

    /** @return 항상 null. 미션을 고르지 않았다는 뜻이다. */
    fun forSkip(): DrivingMission? = null
}
