package ai.tireon.core.mission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MissionCatalogTest {

    @Test
    fun `연습 미션은 여섯 종류다`() {
        assertEquals(6, MissionCatalog.all().size)
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
    fun `천천히 후진은 기어와 속도가 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.REVERSE_SENSE)

        assertTrue(spec.requiresGear)
        assertTrue(spec.requiresSpeed)
        assertFalse(spec.requiresSteering)
    }

    @Test
    fun `깜빡이 습관은 방향지시등과 조향이 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.TURN_SIGNAL)

        assertTrue(spec.requiresTurnSignal)
        assertTrue(spec.requiresSteering)
    }

    @Test
    fun `우회전 일시정지는 속도와 방향지시등과 조향이 필요하다`() {
        val spec = MissionCatalog.spec(PracticeMission.RIGHT_TURN_STOP)

        assertTrue(spec.requiresSpeed)
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
