package ai.tireon.core.report

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionRecordFactoryTest {

    private fun event(type: DrivingEventType) = DrivingEvent(
        type = type,
        atMs = 0L,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `점수가 있으면 기록에 숫자를 담는다`() {
        val record = SessionRecordFactory.create(
            id = "s1",
            startedAtMs = 1_000L,
            durationSec = 120,
            distanceKm = 1.5f,
            result = ScoreResult.Scored(DrivingScore(88), emptyList()),
            events = listOf(event(DrivingEventType.HARSH_BRAKE)),
        )

        assertEquals(88, record.score)
        assertNull(record.insufficientReason)
        assertEquals(1, record.eventCounts["HARSH_BRAKE"])
        assertEquals(1, record.schemaVersion)
    }

    @Test
    fun `데이터 부족은 점수 없이 사유를 남긴다`() {
        val record = SessionRecordFactory.create(
            id = "s2",
            startedAtMs = 2_000L,
            durationSec = 10,
            distanceKm = null,
            result = ScoreResult.Insufficient(InsufficientReason.DURATION_TOO_SHORT),
            events = emptyList(),
        )

        assertNull(record.score)
        assertEquals("DURATION_TOO_SHORT", record.insufficientReason)
        assertNull(record.distanceKm)
    }

    @Test
    fun `이벤트가 없으면 빈 맵이다`() {
        val record = SessionRecordFactory.create(
            id = "s3",
            startedAtMs = 3_000L,
            durationSec = 60,
            distanceKm = 1f,
            result = ScoreResult.Scored(DrivingScore(100), emptyList()),
            events = emptyList(),
        )

        assertEquals(emptyMap(), record.eventCounts)
    }
}
