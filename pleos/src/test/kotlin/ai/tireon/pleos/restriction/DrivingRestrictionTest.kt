package ai.tireon.pleos.restriction

import kotlin.test.Test
import kotlin.test.assertEquals

class DrivingRestrictionTest {

    @Test
    fun `제한이 없으면 전체 UI를 허용한다`() {
        assertEquals(
            DrivingRestriction.NONE,
            RestrictionPolicy.of(requiresDistractionOptimization = false),
        )
    }

    @Test
    fun `제한이 걸리면 최소 UI로 축소한다`() {
        assertEquals(
            DrivingRestriction.MINIMAL_UI,
            RestrictionPolicy.of(requiresDistractionOptimization = true),
        )
    }

    @Test
    fun `상태를 모르면 최소 UI로 본다`() {
        // 제한 상태를 확인하지 못한 채 전체 UI를 띄우면 안전 규정을 위반할 수 있다.
        assertEquals(
            DrivingRestriction.MINIMAL_UI,
            RestrictionPolicy.of(requiresDistractionOptimization = null),
        )
    }
}
