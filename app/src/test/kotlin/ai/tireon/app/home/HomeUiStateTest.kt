package ai.tireon.app.home

import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeUiStateTest {

    @Test
    fun `점수가 있으면 숫자를 보여준다`() {
        val state = HomeUiState.from(ai.tireon.core.DrivingScore(85))

        assertEquals("85", state.scoreText)
        assertTrue(state.hasScore)
        assertEquals("오늘의 운전 점수", state.statusText)
    }

    @Test
    fun `데이터가 부족하면 점수 대신 이유를 보여준다`() {
        val state = HomeUiState.from(
            ScoreResult.Insufficient(InsufficientReason.DISTANCE_TOO_SHORT),
        )

        assertNull(state.scoreText)
        assertFalse(state.hasScore)
        assertEquals("조금 더 길게 주행하면 점수를 낼 수 있어요", state.statusText)
    }

    @Test
    fun `샘플 부족은 신호 문제로 안내한다`() {
        val state = HomeUiState.from(
            ScoreResult.Insufficient(InsufficientReason.NOT_ENOUGH_SAMPLES),
        )

        assertEquals("차량 신호를 충분히 받지 못했어요", state.statusText)
    }

    @Test
    fun `주행 기록이 없으면 시작을 안내한다`() {
        val state = HomeUiState.empty()

        assertNull(state.scoreText)
        assertFalse(state.hasScore)
        assertEquals("아직 주행 기록이 없어요", state.statusText)
    }
}
