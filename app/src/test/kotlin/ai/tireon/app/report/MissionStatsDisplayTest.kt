package ai.tireon.app.report

import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.DrivingSessionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 미션별 성적 표시 문구.
 *
 * 화면 없이 검증한다. 문구가 잘못되면 사용자가 "무엇을 연습해야 하는지"를
 * 잘못 이해하게 된다.
 */
class MissionStatsDisplayTest {

    private fun record(
        id: String = "r1",
        mission: String? = "SMOOTH_START",
        success: Boolean? = true,
    ) = DrivingSessionRecord(
        id = id,
        startedAtMs = NOW - DAY,
        durationSec = 60,
        distanceKm = 1f,
        score = 85,
        insufficientReason = null,
        eventCounts = emptyMap(),
        mission = mission,
        missionSuccess = success,
    )

    @Test
    fun `기록이 없으면 안내 문구를 보여준다`() {
        val display = MissionStatsDisplay.from(emptyList())

        assertTrue(!display.hasStats)
        assertTrue(display.emptyMessage.contains("미션"))
        assertTrue(display.rows.isEmpty())
        assertNull(display.hardestText)
    }

    @Test
    fun `미션 성적이 있으면 표시한다`() {
        val display = MissionStatsDisplay.from(listOf(record(success = true)))

        assertTrue(display.hasStats)
        assertEquals(1, display.rows.size)
        assertEquals("부드러운 출발", display.rows.single().name)
        assertEquals("100%", display.rows.single().rateText)
        assertEquals("1번 성공 / 1번 시도", display.rows.single().countText)
    }

    @Test
    fun `실패가 있으면 성공률이 내려간다`() {
        val display = MissionStatsDisplay.from(
            listOf(
                record(id = "a", success = true),
                record(id = "b", success = false),
            ),
        )

        assertEquals("50%", display.rows.single().rateText)
        assertEquals("1번 성공 / 2번 시도", display.rows.single().countText)
    }

    @Test
    fun `가장 어려운 미션을 안내한다`() {
        val display = MissionStatsDisplay.from(
            listOf(
                record(id = "a", mission = "SMOOTH_START", success = true),
                record(id = "b", mission = "SMOOTH_STOP", success = false),
                record(id = "c", mission = "SMOOTH_STOP", success = false),
            ),
        )

        assertEquals("부드러운 정지 연습이 필요해요", display.hardestText)
    }

    @Test
    fun `모두 성공하면 어려운 미션 안내가 없다`() {
        val display = MissionStatsDisplay.from(
            listOf(
                record(id = "a", success = true),
                record(id = "b", success = true),
            ),
        )

        assertNull(display.hardestText)
    }

    @Test
    fun `시도가 한 번뿐이면 어려운 미션으로 보지 않는다`() {
        val display = MissionStatsDisplay.from(
            listOf(
                record(id = "a", mission = "SMOOTH_START", success = true),
                record(id = "b", mission = "SMOOTH_START", success = true),
                record(id = "c", mission = "SMOOTH_STOP", success = false),
            ),
        )

        assertNull(display.hardestText)
    }

    @Test
    fun `미션 이름이 모두 한국어다`() {
        PracticeMission.entries.forEach { mission ->
            val name = MissionStatsDisplay.nameOf(mission)

            assertTrue(name.isNotBlank(), "$mission 이름이 비어 있다")
            assertTrue(
                name.any { it in '가'..'힣' },
                "$mission 이름에 한글이 없다: $name",
            )
        }
    }

    @Test
    fun `판정 불가 기록만 있으면 안내 문구를 보여준다`() {
        val display = MissionStatsDisplay.from(listOf(record(success = null)))

        assertTrue(!display.hasStats)
        assertTrue(display.rows.isEmpty())
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
