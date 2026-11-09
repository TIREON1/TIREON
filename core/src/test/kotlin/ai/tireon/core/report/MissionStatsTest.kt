package ai.tireon.core.report

import ai.tireon.core.mission.PracticeMission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 미션별 성공률을 계산한다.
 *
 * "어떤 미션을 자주 실패하는지"를 알아야 다음 연습을 제안할 수 있다.
 * 판정 불가(null)는 성패 어느 쪽에도 세지 않는다.
 */
class MissionStatsTest {

    private fun record(
        id: String = "r1",
        mission: String? = "SMOOTH_START",
        success: Boolean? = true,
        startedAtMs: Long = NOW - DAY,
    ) = DrivingSessionRecord(
        id = id,
        startedAtMs = startedAtMs,
        durationSec = 60,
        distanceKm = 1f,
        score = 85,
        insufficientReason = null,
        eventCounts = emptyMap(),
        mission = mission,
        missionSuccess = success,
    )

    @Test
    fun `기록이 없으면 빈 목록이다`() {
        assertTrue(MissionStats.byMission(emptyList()).isEmpty())
    }

    @Test
    fun `미션이 없는 기록은 제외한다`() {
        val records = listOf(record(mission = null, success = null))

        assertTrue(MissionStats.byMission(records).isEmpty())
    }

    @Test
    fun `판정 불가 기록은 제외한다`() {
        val records = listOf(record(mission = "SMOOTH_START", success = null))

        assertTrue(MissionStats.byMission(records).isEmpty())
    }

    @Test
    fun `성공과 실패를 센다`() {
        val records = listOf(
            record(id = "a", success = true),
            record(id = "b", success = true),
            record(id = "c", success = false),
        )

        val stats = MissionStats.byMission(records).single()

        assertEquals(PracticeMission.SMOOTH_START, stats.mission)
        assertEquals(2, stats.successCount)
        assertEquals(1, stats.failureCount)
        assertEquals(3, stats.total)
    }

    @Test
    fun `성공률을 퍼센트로 계산한다`() {
        val records = listOf(
            record(id = "a", success = true),
            record(id = "b", success = true),
            record(id = "c", success = true),
            record(id = "d", success = false),
        )

        val stats = MissionStats.byMission(records).single()

        assertEquals(75, stats.successRate)
    }

    @Test
    fun `미션별로 따로 집계한다`() {
        val records = listOf(
            record(id = "a", mission = "SMOOTH_START", success = true),
            record(id = "b", mission = "SMOOTH_STOP", success = false),
            record(id = "c", mission = "SMOOTH_STOP", success = false),
        )

        val stats = MissionStats.byMission(records)

        assertEquals(2, stats.size)
        assertEquals(PracticeMission.SMOOTH_STOP, stats[0].mission)
        assertEquals(PracticeMission.SMOOTH_START, stats[1].mission)
    }

    @Test
    fun `시도가 많은 순으로 정렬한다`() {
        val records = listOf(
            record(id = "a", mission = "SMOOTH_START", success = true),
            record(id = "b", mission = "SMOOTH_STOP", success = true),
            record(id = "c", mission = "SMOOTH_STOP", success = true),
            record(id = "d", mission = "SMOOTH_STOP", success = true),
        )

        val stats = MissionStats.byMission(records)

        assertEquals(PracticeMission.SMOOTH_STOP, stats.first().mission)
        assertEquals(3, stats.first().total)
    }

    @Test
    fun `가장 어려운 미션을 찾는다`() {
        val records = listOf(
            record(id = "a", mission = "SMOOTH_START", success = true),
            record(id = "b", mission = "SMOOTH_STOP", success = false),
            record(id = "c", mission = "SMOOTH_STOP", success = false),
        )

        val hardest = MissionStats.hardest(records)

        assertEquals(PracticeMission.SMOOTH_STOP, hardest?.mission)
    }

    @Test
    fun `시도가 한 번뿐인 미션은 가장 어려운 미션으로 보지 않는다`() {
        val records = listOf(
            record(id = "a", mission = "SMOOTH_START", success = true),
            record(id = "b", mission = "SMOOTH_START", success = true),
            record(id = "c", mission = "SMOOTH_START", success = true),
            record(id = "d", mission = "SMOOTH_STOP", success = false),
        )

        val hardest = MissionStats.hardest(records)

        assertNull(hardest)
    }

    @Test
    fun `모두 성공하면 어려운 미션이 없다`() {
        val records = listOf(
            record(id = "a", success = true),
            record(id = "b", success = true),
        )

        assertNull(MissionStats.hardest(records))
    }

    @Test
    fun `알 수 없는 미션 이름은 제외한다`() {
        val records = listOf(record(mission = "UNKNOWN_MISSION", success = true))

        assertTrue(MissionStats.byMission(records).isEmpty())
    }

    @Test
    fun `기간 내 기록만 센다`() {
        val records = listOf(
            record(id = "recent", success = true, startedAtMs = NOW - DAY),
            record(id = "old", success = false, startedAtMs = NOW - 30 * DAY),
        )

        val stats = MissionStats.byMission(records, nowMs = NOW, periodDays = 7).single()

        assertEquals(1, stats.successCount)
        assertEquals(0, stats.failureCount)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
