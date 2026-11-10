package ai.tireon.app.driving

import ai.tireon.core.mission.DrivingMission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 주행 미션 선택 문구.
 *
 * 미션 정의는 `:core`에 있고 문구는 앱 리소스에 있다. 이 매핑이 그 둘을 잇는다.
 * 매핑이 빠지면 화면에서 미션을 고를 수 없다.
 */
class DrivingMissionSelectionTest {

    @Test
    fun `모든 주행 미션에 문구가 있다`() {
        val displays = DrivingMissionSelection.all()

        assertEquals(DrivingMission.entries.size, displays.size)
    }

    @Test
    fun `미션마다 이름과 설명이 다르다`() {
        val displays = DrivingMissionSelection.all()

        assertEquals(
            displays.size,
            displays.map { it.nameResId }.distinct().size,
            "미션 이름이 중복된다",
        )
        assertEquals(
            displays.size,
            displays.map { it.descriptionResId }.distinct().size,
            "미션 설명이 중복된다",
        )
    }

    @Test
    fun `미션 없이 주행 선택지의 이름이 있다`() {
        val nameResId = DrivingMissionSelection.skipNameResId()

        assertTrue(nameResId != 0)
    }

    @Test
    fun `미션 없이 주행은 미션을 돌려주지 않는다`() {
        assertNull(DrivingMissionSelection.forSkip())
    }

    @Test
    fun `미션 이름으로 화면 문구를 찾는다`() {
        val display = DrivingMissionSelection.of(DrivingMission.SMOOTH_DRIVING)

        assertEquals(DrivingMission.SMOOTH_DRIVING, display.mission)
        assertTrue(display.nameResId != 0)
        assertTrue(display.descriptionResId != 0)
    }
}
