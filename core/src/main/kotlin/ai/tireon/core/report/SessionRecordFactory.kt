package ai.tireon.core.report

import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.mission.MissionResult
import ai.tireon.core.mission.PracticeMission

/** 분석 결과를 저장 가능한 기록으로 바꾼다. */
object SessionRecordFactory {

    fun create(
        id: String,
        startedAtMs: Long,
        durationSec: Int,
        distanceKm: Float?,
        result: ScoreResult,
        events: List<DrivingEvent>,
        mission: PracticeMission? = null,
        missionResult: MissionResult? = null,
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
            null -> null
        }

        return DrivingSessionRecord(
            id = id,
            startedAtMs = startedAtMs,
            durationSec = durationSec,
            distanceKm = distanceKm,
            score = score,
            insufficientReason = insufficientReason,
            eventCounts = counts,
            mission = mission?.name,
            missionSuccess = success,
            missionDetail = detail,
        )
    }
}
