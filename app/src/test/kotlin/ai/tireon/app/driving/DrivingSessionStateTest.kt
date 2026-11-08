package ai.tireon.app.driving

import ai.tireon.pleos.restriction.DrivingRestriction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 주행 세션 상태.
 *
 * 연습과 달리 주행 중에는 화면을 띄우지 않는다. 수집만 하고, 정차 후에 결과를 본다.
 */
class DrivingSessionStateTest {

    @Test
    fun `처음에는 준비 상태다`() {
        val state = DrivingSessionState.initial()

        assertEquals(DrivingSessionPhase.READY, state.phase)
        assertFalse(state.collecting)
    }

    @Test
    fun `시작하면 수집을 시작한다`() {
        val state = DrivingSessionState.initial().start(DrivingRestriction.NONE)

        assertEquals(DrivingSessionPhase.COLLECTING, state.phase)
        assertTrue(state.collecting)
    }

    @Test
    fun `주행 중에는 화면을 숨긴다`() {
        val state = DrivingSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.MINIMAL_UI, sampleCount = 10)

        assertTrue(state.hideUi)
    }

    @Test
    fun `제한 상태를 모르면 화면을 숨긴다`() {
        val state = DrivingSessionState.initial().start(null)

        assertTrue(state.hideUi)
    }

    @Test
    fun `정차하면 결과를 볼 수 있다`() {
        val state = DrivingSessionState.initial()
            .start(DrivingRestriction.MINIMAL_UI)
            .tick(DrivingRestriction.MINIMAL_UI, sampleCount = 50)
            .stop(DrivingRestriction.NONE)

        assertEquals(DrivingSessionPhase.FINISHED, state.phase)
        assertFalse(state.collecting)
        assertFalse(state.hideUi)
    }

    @Test
    fun `주행 중에는 종료할 수 없다`() {
        val state = DrivingSessionState.initial()
            .start(DrivingRestriction.MINIMAL_UI)
            .stop(DrivingRestriction.MINIMAL_UI)

        assertEquals(DrivingSessionPhase.COLLECTING, state.phase)
    }

    @Test
    fun `샘플 수를 기록한다`() {
        val state = DrivingSessionState.initial()
            .start(DrivingRestriction.NONE)
            .tick(DrivingRestriction.NONE, sampleCount = 42)

        assertEquals(42, state.sampleCount)
    }
}
