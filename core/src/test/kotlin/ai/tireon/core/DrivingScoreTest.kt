package ai.tireon.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DrivingScoreTest {
    @Test
    fun `범위 안의 점수를 생성한다`() {
        assertEquals(80, DrivingScore(80).value)
    }

    @Test
    fun `범위를 벗어난 점수는 거부한다`() {
        assertFailsWith<IllegalArgumentException> { DrivingScore(-1) }
        assertFailsWith<IllegalArgumentException> { DrivingScore(101) }
    }
}
