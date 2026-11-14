package ai.tireon.core.report

import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.mission.DrivingMission
import ai.tireon.core.mission.DrivingMissionResult
import ai.tireon.core.mission.MissionResult
import ai.tireon.core.mission.PracticeMission

/** 분석 결과를 저장 가능한 기록으로 바꾼다. */
object SessionRecordFactory {

    /**
     * @param mission 연습 미션. 연습 세션이 아니면 null.
     * @param missionResult 연습 미션 판정 결과.
     * @param drivingMission 주행 미션. 고르지 않았으면 null.
     *
     * 주행 미션은 성공·실패를 가르지 않는다. 실도로는 통제할 수 없는 변수가 많아
     * 합격·불합격보다 "무엇이 몇 번 있었는지"를 보여주는 편이 정직하다. 그래서
     * [drivingMission]은 판정 요약과 보류 여부를 남기고 `missionSuccess`는 null로 둔다.
     */
    fun create(
        id: String,
        startedAtMs: Long,
        durationSec: Int,
        distanceKm: Float?,
        result: ScoreResult,
        events: List<DrivingEvent>,
        mission: PracticeMission? = null,
        missionResult: MissionResult? = null,
        drivingMission: DrivingMission? = null,
        drivingMissionResult: DrivingMissionResult? = null,
    ): DrivingSessionRecord {
        val counts = events.groupingBy { it.type.name }.eachCount()

        val (score, insufficientReason) = when (result) {
            is ScoreResult.Scored -> result.score.value to null
            is ScoreResult.Insufficient -> null to result.reason.name
        }

        val success = when (missionResult) {
            is MissionResult.Success -> true
            is MissionResult.Failed -> false
            else -> null
        }

        val detail = when (missionResult) {
            is MissionResult.Success -> missionResult.detail
            is MissionResult.Failed -> missionResult.detail
            is MissionResult.Insufficient -> missionResult.reason
            null -> when (drivingMissionResult) {
                is DrivingMissionResult.Scored -> drivingMissionResult.detail
                is DrivingMissionResult.Insufficient -> drivingMissionResult.reason
                null -> null
            }
        }
        val evaluated = when {
            missionResult != null -> missionResult !is MissionResult.Insufficient
            drivingMissionResult != null -> drivingMissionResult is DrivingMissionResult.Scored
            else -> null
        }

        return DrivingSessionRecord(
            id = id,
            startedAtMs = startedAtMs,
            durationSec = durationSec,
            distanceKm = distanceKm,
            score = score,
            insufficientReason = insufficientReason,
            eventCounts = counts,
            mission = mission?.name ?: drivingMission?.name,
            missionSuccess = success,
            missionDetail = detail,
            missionEvaluated = evaluated,
        )
    }
}
