package ai.tireon.core.report

import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.DrivingEvent

/** 분석 결과를 저장 가능한 기록으로 바꾼다. */
object SessionRecordFactory {

    fun create(
        id: String,
        startedAtMs: Long,
        durationSec: Int,
        distanceKm: Float?,
        result: ScoreResult,
        events: List<DrivingEvent>,
    ): DrivingSessionRecord {
        val counts = events.groupingBy { it.type.name }.eachCount()

        return when (result) {
            is ScoreResult.Scored -> DrivingSessionRecord(
                id = id,
                startedAtMs = startedAtMs,
                durationSec = durationSec,
                distanceKm = distanceKm,
                score = result.score.value,
                insufficientReason = null,
                eventCounts = counts,
            )

            is ScoreResult.Insufficient -> DrivingSessionRecord(
                id = id,
                startedAtMs = startedAtMs,
                durationSec = durationSec,
                distanceKm = distanceKm,
                score = null,
                insufficientReason = result.reason.name,
                eventCounts = counts,
            )
        }
    }
}
