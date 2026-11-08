package ai.tireon.app.report

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 저장 결과를 화면 상태로 바꾼다.
 *
 * 저장이 실패하면 사용자가 알아야 한다. 성공했거나 아직 저장 전이면 안내하지 않는다.
 */
class SaveStatusTest {

    @Test
    fun `저장 전에는 안내가 없다`() {
        val status = SaveStatus.idle()

        assertNull(status.message)
    }

    @Test
    fun `저장 성공에는 안내가 없다`() {
        val status = SaveStatus.from(Result.success(Unit))

        assertNull(status.message)
    }

    @Test
    fun `저장 실패에는 안내 문구가 있다`() {
        val status = SaveStatus.from(Result.failure(IllegalStateException("disk full")))

        assertEquals(true, status.message != null)
        assertTrue(status.message!!.contains("저장"))
    }

    @Test
    fun `안내 문구는 원인을 추측하지 않는다`() {
        val status = SaveStatus.from(Result.failure(IllegalStateException("disk full")))

        assertEquals(false, status.message!!.contains("용량"))
    }
}
