package ai.tireon.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DrivingDisplayTest {

    @Test
    fun `측정값이 있으면 그대로 표시한다`() {
        assertEquals("12.5", DrivingDisplay.distance(12.5f))
        assertEquals("0.0", DrivingDisplay.distance(0.0f))
        assertEquals("30", DrivingDisplay.drivingTime(30))
    }

    @Test
    fun `측정값이 없으면 null을 반환한다`() {
        // 0으로 대체하면 실제 측정값 0과 구분되지 않으므로 null을 유지한다.
        assertNull(DrivingDisplay.distance(null))
        assertNull(DrivingDisplay.drivingTime(null))
    }

    @Test
    fun `실제 측정값 0과 값 없음은 구분된다`() {
        assertEquals("0.0", DrivingDisplay.distance(0.0f))
        assertNull(DrivingDisplay.distance(null))
    }
}
