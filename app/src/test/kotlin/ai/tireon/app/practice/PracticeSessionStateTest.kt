package ai.tireon.app.practice

import ai.tireon.pleos.restriction.DrivingRestriction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeSessionStateTest {

    @Test
    fun `처음에는 준비 상태다`() {
        val state = PracticeSessionState.initial()

        assertEquals(PracticePhase.READY, state.phase)
        assertEquals(DrivingRestriction.NONE, state.restriction)
        assertFalse(state.showMinimalUi)
    }

    @Test
    fun `시작하면 진행 상태가 된다`() {
        val state = PracticeSessionState.initial().start(DrivingRestriction.NONE)

        assertEquals(PracticePhase.RUNNING, state.phase)
    }

    @Test
    fun `제한이 걸리면 최소 UI로 바뀐다`() {
        val state = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.MINIMAL_UI, sampleCount = 5)

        assertTrue(state.showMinimalUi)
        assertEquals(5, state.sampleCount)
    }

    @Test
    fun `제한 상태를 모르면 최소 UI로 본다`() {
        val state = PracticeSessionState.initial().start(null)

        assertTrue(state.showMinimalUi)
    }

    @Test
    fun `경과 시간은 샘플 수가 아니라 넘겨받은 시간을 쓴다`() {
        val state = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 15, elapsedSec = 3)

        assertEquals(3, state.elapsedSec)
        assertEquals(15, state.sampleCount)
    }

    @Test
    fun `시간을 넘기지 않으면 경과 시간을 유지한다`() {
        val state = PracticeSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 5, elapsedSec = 4)
            .tick(DrivingRestriction.MINIMAL_UI, sampleCount = 5)

        assertEquals(4, state.elapsedSec)
    }

    @Test
    fun `제한이 남아 있으면 종료할 수 없다`() {
        val running = PracticeSessionState.initial().start(DrivingRestriction.NONE)

        val state = running.finish(DrivingRestriction.MINIMAL_UI)

        assertEquals(PracticePhase.RUNNING, state.phase)
    }

    @Test
    fun `제한이 풀리면 종료된다`() {
        val running = PracticeSessionState.initial().start(DrivingRestriction.MINIMAL_UI)

        val state = running.finish(DrivingRestriction.NONE)

        assertEquals(PracticePhase.FINISHED, state.phase)
    }

    @Test
    fun `준비 상태에서는 종료되지 않는다`() {
        val state = PracticeSessionState.initial().finish(DrivingRestriction.NONE)

        assertEquals(PracticePhase.READY, state.phase)
    }
}
