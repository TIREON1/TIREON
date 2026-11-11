package ai.tireon.app.report

import ai.tireon.core.mission.DrivingMission
import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.DrivingSessionRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionKindTest {

    private fun record(id: String, mission: String?) = DrivingSessionRecord(
        id = id,
        startedAtMs = 0L,
        durationSec = 0,
        distanceKm = null,
        score = null,
        insufficientReason = null,
        eventCounts = emptyMap(),
        mission = mission,
    )

    @Test
    fun `연습 미션 이름이면 연습 기록이다`() {
        assertEquals(
            SessionKind.Practice(PracticeMission.SMOOTH_STOP),
            SessionKind.of(record("session-1", "SMOOTH_STOP")),
        )
    }

    @Test
    fun `주행 미션 이름이면 주행 기록이다`() {
        assertEquals(
            SessionKind.Driving(DrivingMission.OVERALL),
            SessionKind.of(record("driving-1", "OVERALL")),
        )
    }

    @Test
    fun `미션 없이 주행한 기록은 ID로 구분한다`() {
        assertEquals(SessionKind.Driving(null), SessionKind.of(record("driving-1", null)))
    }

    @Test
    fun `알 수 없는 미션 이름은 미션 없는 연습으로 본다`() {
        assertEquals(SessionKind.Practice(null), SessionKind.of(record("session-1", "REMOVED")))
    }
}
