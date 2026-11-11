package ai.tireon.app.report

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ai.tireon.app.R
import ai.tireon.app.driving.DrivingMissionSelection
import ai.tireon.app.practice.MissionSelection
import ai.tireon.core.mission.DrivingMission
import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.DrivingSessionRecord

/**
 * 기록이 연습인지 주행인지.
 *
 * 기록에는 종류 필드가 없어 저장된 미션 이름으로 구분한다. 두 미션 이름은 겹치지 않는다.
 */
sealed interface SessionKind {
    data class Practice(val mission: PracticeMission?) : SessionKind
    data class Driving(val mission: DrivingMission?) : SessionKind

    companion object {
        /** DrivingSessionViewModel이 주행 기록 ID에 붙이는 접두어. */
        private const val DRIVING_ID_PREFIX = "driving-"

        fun of(record: DrivingSessionRecord): SessionKind {
            val name = record.mission
            PracticeMission.entries.firstOrNull { it.name == name }?.let { return Practice(it) }
            DrivingMission.entries.firstOrNull { it.name == name }?.let { return Driving(it) }
            // 미션 없이 주행한 기록은 이름이 없으므로 ID로 구분한다.
            return if (record.id.startsWith(DRIVING_ID_PREFIX)) Driving(null) else Practice(null)
        }
    }
}

/** "연습 · 부드러운 정지", "주행 · 전체 점검", "주행" 같은 한 줄 표시. */
@Composable
internal fun SessionKind.label(): String {
    val (kind, missionName) = when (this) {
        is SessionKind.Practice -> stringResource(R.string.report_kind_practice) to
            mission?.let { stringResource(MissionSelection.of(it).nameResId) }
        is SessionKind.Driving -> stringResource(R.string.report_kind_driving) to
            mission?.let { stringResource(DrivingMissionSelection.of(it).nameResId) }
    }
    return if (missionName == null) kind else "$kind · $missionName"
}

internal val SessionKind.iconRes: Int
    get() = when (this) {
        is SessionKind.Practice -> R.drawable.ic_practice
        is SessionKind.Driving -> R.drawable.ic_car
    }
