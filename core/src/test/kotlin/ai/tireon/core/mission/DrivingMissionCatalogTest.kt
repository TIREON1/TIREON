package ai.tireon.core.mission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 주행 미션 목록.
 *
 * 연습 미션과 달리 판정에 필요한 신호가 미션마다 다르다. 어떤 신호를 요구하는지
 * 목록으로 드러내야 화면과 판정이 같은 기준을 쓴다.
 */
class DrivingMissionCatalogTest {

    @Test
    fun `미션 없이 주행하는 선택지가 있다`() {
        assertTrue(DrivingMissionCatalog.ALLOW_SKIP)
    }

    @Test
    fun `모든 주행 미션이 목록에 있다`() {
        val missions = DrivingMissionCatalog.all()

        assertEquals(DrivingMission.entries.size, missions.size)
        assertTrue(missions.map { it.mission }.containsAll(DrivingMission.entries))
    }

    @Test
    fun `중복된 미션이 없다`() {
        val missions = DrivingMissionCatalog.all().map { it.mission }

        assertEquals(missions.size, missions.distinct().size)
    }

    @Test
    fun `전체 점검은 모든 신호를 요구한다`() {
        val spec = DrivingMissionCatalog.of(DrivingMission.OVERALL)

        assertTrue(spec.requiresSpeed)
        assertTrue(spec.requiresSteering)
        assertTrue(spec.requiresTurnSignal)
    }

    @Test
    fun `방향지시등 미션은 방향지시등 신호를 요구한다`() {
        val spec = DrivingMissionCatalog.of(DrivingMission.SIGNAL_HABIT)

        assertTrue(spec.requiresTurnSignal)
    }

    @Test
    fun `안정 조향 미션은 조향 신호를 요구한다`() {
        val spec = DrivingMissionCatalog.of(DrivingMission.STABLE_STEERING)

        assertTrue(spec.requiresSteering)
    }

    @Test
    fun `모든 미션이 속도를 요구한다`() {
        // 급가속·급제동·조향 판정 모두 속도에서 나온다.
        DrivingMissionCatalog.all().forEach { spec ->
            assertTrue(spec.requiresSpeed, "${spec.mission} 이 속도를 요구하지 않는다")
        }
    }
}
