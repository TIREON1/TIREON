package ai.tireon.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class WindowLayoutTest {

    @Test
    fun `좁은 창은 단일 열이다`() {
        assertEquals(WindowLayout.COMPACT, WindowLayoutPolicy.of(360))
        assertEquals(WindowLayout.COMPACT, WindowLayoutPolicy.of(599))
    }

    @Test
    fun `넓은 창은 두 열이다`() {
        assertEquals(WindowLayout.EXPANDED, WindowLayoutPolicy.of(600))
        assertEquals(WindowLayout.EXPANDED, WindowLayoutPolicy.of(1280))
    }

    @Test
    fun `경계값은 확장으로 본다`() {
        assertEquals(WindowLayout.EXPANDED, WindowLayoutPolicy.of(600))
        assertEquals(WindowLayout.COMPACT, WindowLayoutPolicy.of(599))
    }
}
