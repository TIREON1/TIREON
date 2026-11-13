package ai.tireon.core.report

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.mission.MissionResult
import ai.tireon.core.mission.PracticeMission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MissionResultRecordTest {

    @Test
    fun `미션 성공을 기록에 담는다`() {
        val record = SessionRecordFactory.create(
            id = "s1",
            startedAtMs = 1_000L,
            durationSec = 60,
            distanceKm = 1f,
            result = ScoreResult.Scored(DrivingScore(90), emptyList()),
            events = emptyList(),
            mission = PracticeMission.SMOOTH_START,
            missionResult = MissionResult.Success("급가속 없이 출발했어요"),
        )

        assertEquals("SMOOTH_START", record.mission)
        assertEquals(true, record.missionSuccess)
        assertEquals("급가속 없이 출발했어요", record.missionDetail)
    }

    @Test
    fun `미션 실패를 기록에 담는다`() {
        val record = SessionRecordFactory.create(
            id = "s2",
            startedAtMs = 2_000L,
            durationSec = 60,
            distanceKm = 1f,
            result = ScoreResult.Scored(DrivingScore(70), emptyList()),
            events = emptyList(),
            mission = PracticeMission.SMOOTH_STOP,
            missionResult = MissionResult.Failed("급제동이 있었어요 (2 번)"),
        )

        assertEquals("SMOOTH_STOP", record.mission)
        assertEquals(false, record.missionSuccess)
        assertEquals("급제동이 있었어요 (2 번)", record.missionDetail)
    }

    @Test
    fun `미션 판정 불가는 달성 여부를 비운다`() {
        val record = SessionRecordFactory.create(
            id = "s3",
            startedAtMs = 3_000L,
            durationSec = 10,
            distanceKm = null,
            result = ScoreResult.Insufficient(InsufficientReason.DURATION_TOO_SHORT),
            events = emptyList(),
            mission = PracticeMission.STEADY_SPEED,
            missionResult = MissionResult.Insufficient("속도 신호를 받지 못했어요"),
        )

        assertEquals("STEADY_SPEED", record.mission)
        assertNull(record.missionSuccess)
        assertEquals("속도 신호를 받지 못했어요", record.missionDetail)
    }

    @Test
    fun `미션 정보가 없으면 모두 비운다`() {
        val record = SessionRecordFactory.create(
            id = "s4",
            startedAtMs = 4_000L,
            durationSec = 60,
            distanceKm = 1f,
            result = ScoreResult.Scored(DrivingScore(100), emptyList()),
            events = emptyList(),
        )

        assertNull(record.mission)
        assertNull(record.missionSuccess)
        assertNull(record.missionDetail)
    }

    @Test
    fun `스키마 버전이 올라간다`() {
        val record = SessionRecordFactory.create(
            id = "s5",
            startedAtMs = 5_000L,
            durationSec = 60,
            distanceKm = 1f,
            result = ScoreResult.Scored(DrivingScore(100), emptyList()),
            events = emptyList(),
        )

        assertEquals(3, record.schemaVersion)
    }
}
