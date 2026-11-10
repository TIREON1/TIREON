package ai.tireon.core.report

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.mission.DrivingMission
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
        assertEquals(DrivingSessionRecord.CURRENT_SCHEMA_VERSION, record.schemaVersion)
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

    @Test
    fun `주행 미션 이름을 기록에 담는다`() {
        val record = SessionRecordFactory.create(
            id = "s4",
            startedAtMs = 4_000L,
            durationSec = 300,
            distanceKm = 5f,
            result = ScoreResult.Scored(DrivingScore(80), emptyList()),
            events = emptyList(),
            drivingMission = DrivingMission.SMOOTH_DRIVING,
        )

        assertEquals("SMOOTH_DRIVING", record.mission)
    }

    @Test
    fun `주행 미션은 성공 여부를 판정하지 않는다`() {
        val record = SessionRecordFactory.create(
            id = "s5",
            startedAtMs = 5_000L,
            durationSec = 300,
            distanceKm = 5f,
            result = ScoreResult.Scored(DrivingScore(80), emptyList()),
            events = emptyList(),
            drivingMission = DrivingMission.OVERALL,
        )

        // 실도로는 통제할 수 없는 변수가 많아 합격·불합격을 가르지 않는다.
        assertNull(record.missionSuccess)
        assertEquals("OVERALL", record.mission)
    }

    @Test
    fun `주행 미션을 고르지 않으면 미션을 남기지 않는다`() {
        val record = SessionRecordFactory.create(
            id = "s6",
            startedAtMs = 6_000L,
            durationSec = 300,
            distanceKm = 5f,
            result = ScoreResult.Scored(DrivingScore(80), emptyList()),
            events = emptyList(),
            drivingMission = null,
        )

        assertNull(record.mission)
        assertNull(record.missionSuccess)
    }
}
