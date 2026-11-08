package ai.tireon.app.practice

import ai.tireon.core.mission.MissionCatalog
import ai.tireon.core.mission.PracticeMission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MissionSelectionTest {

    @Test
    fun `카탈로그의 모든 미션에 문구가 있다`() {
        MissionCatalog.all().forEach { mission ->
            val display = MissionSelection.of(mission)
            assertEquals(mission, display.mission)
            assertTrue(display.nameResId != 0)
            assertTrue(display.descriptionResId != 0)
        }
    }

    @Test
    fun `화면 목록은 카탈로그 순서를 따른다`() {
        val missions = MissionSelection.all().map { it.mission }

        assertEquals(MissionCatalog.all(), missions)
    }

    @Test
    fun `미션마다 문구가 다르다`() {
        val names = MissionSelection.all().map { it.nameResId }

        assertEquals(names.size, names.distinct().size)
    }
}
