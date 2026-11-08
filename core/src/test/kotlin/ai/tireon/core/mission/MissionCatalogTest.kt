package ai.tireon.core.mission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MissionCatalogTest {

    @Test
    fun `연습 미션은 다섯 종류다`() {
        assertEquals(5, MissionCatalog.all().size)
    }

    @Test
    fun `모든 미션에 스펙이 있다`() {
        MissionCatalog.all().forEach { mission ->
            val spec = MissionCatalog.spec(mission)
            assertEquals(mission, spec.mission)
        }
    }

    @Test
    fun `부드러운 출발은 속도만 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.SMOOTH_START)

        assertTrue(spec.requiresSpeed)
        assertFalse(spec.requiresSteering)
        assertFalse(spec.requiresGear)
    }

    @Test
    fun `후진 감각은 기어와 조향이 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.REVERSE_SENSE)

        assertTrue(spec.requiresGear)
        assertTrue(spec.requiresSteering)
    }

    @Test
    fun `깜빡이 습관은 방향지시등과 조향이 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.TURN_SIGNAL)

        assertTrue(spec.requiresTurnSignal)
        assertTrue(spec.requiresSteering)
    }

    @Test
    fun `정속 유지는 속도만 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.STEADY_SPEED)

        assertTrue(spec.requiresSpeed)
        assertFalse(spec.requiresSteering)
    }
}
