package ai.tireon.app.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 권한 재확인 판정.
 *
 * 화면이 다시 보일 때 권한 상태를 다시 읽어야 한다. 사용자가 설정에서 허용하고
 * 돌아왔거나, 거부했다가 다시 시도하는 경우를 같은 흐름으로 다루기 위함이다.
 */
class SpeedPermissionResumeTest {

    @Test
    fun `화면에 돌아왔을 때 권한이 생겼으면 통과한다`() {
        val decision = SpeedPermission.decideOnResume(
            grantedNow = true,
            shouldShowRationale = true,
        )

        assertEquals(SpeedPermission.Decision.NONE, decision)
    }

    @Test
    fun `화면에 돌아왔을 때 거부 이력이 있으면 이유를 안내한다`() {
        val decision = SpeedPermission.decideOnResume(
            grantedNow = false,
            shouldShowRationale = true,
        )

        assertEquals(SpeedPermission.Decision.REQUEST_WITH_RATIONALE, decision)
    }

    @Test
    fun `화면에 돌아왔을 때 처음 거부면 그냥 요청한다`() {
        val decision = SpeedPermission.decideOnResume(
            grantedNow = false,
            shouldShowRationale = false,
        )

        assertEquals(SpeedPermission.Decision.REQUEST, decision)
    }

    @Test
    fun `안내 후 다시 요청할 수 있다`() {
        val decision = SpeedPermission.decideAfterRationale()

        assertEquals(SpeedPermission.Decision.REQUEST, decision)
    }

    @Test
    fun `설정 이동이 필요한 상태를 구분한다`() {
        // 두 번 거부하면 시스템이 대화상자를 더 띄우지 않는다. 설정으로 보내야 한다.
        val decision = SpeedPermission.decideOnResume(
            grantedNow = false,
            shouldShowRationale = false,
            askedBefore = true,
        )

        assertEquals(SpeedPermission.Decision.OPEN_SETTINGS, decision)
    }

    @Test
    fun `처음 물어보는 것이면 설정이 아니라 대화상자를 띄운다`() {
        val decision = SpeedPermission.decideOnResume(
            grantedNow = false,
            shouldShowRationale = false,
            askedBefore = false,
        )

        assertEquals(SpeedPermission.Decision.REQUEST, decision)
    }

    @Test
    fun `승인한 적 있으면 직접 끈 것이므로 다시 요청한다`() {
        val decision = SpeedPermission.decideOnResume(
            grantedNow = false,
            shouldShowRationale = false,
            askedBefore = true,
            previouslyGranted = true,
        )

        assertEquals(SpeedPermission.Decision.REQUEST, decision)
    }

    @Test
    fun `영구 거부에는 설정 안내 문구가 있다`() {
        val message = SpeedPermission.rationaleFor(SpeedPermission.Decision.OPEN_SETTINGS)

        assertTrue(message != null && message.contains("설정"))
    }

    @Test
    fun `이전에 승인했는데 지금 없으면 다시 요청한다`() {
        val decision = SpeedPermission.decideOnResume(
            grantedNow = false,
            shouldShowRationale = false,
            previouslyGranted = true,
        )

        assertEquals(SpeedPermission.Decision.REQUEST, decision)
    }

    @Test
    fun `요청 중에는 중복 요청하지 않는다`() {
        assertTrue(SpeedPermission.shouldRequest(SpeedPermission.Decision.REQUEST, requestInFlight = false))
        assertFalse(SpeedPermission.shouldRequest(SpeedPermission.Decision.REQUEST, requestInFlight = true))
        assertFalse(SpeedPermission.shouldRequest(SpeedPermission.Decision.NONE, requestInFlight = false))
    }

    @Test
    fun `설정으로 보낼 때는 권한 요청을 하지 않는다`() {
        assertFalse(
            SpeedPermission.shouldRequest(SpeedPermission.Decision.OPEN_SETTINGS, requestInFlight = false),
        )
    }

    @Test
    fun `안내가 필요 없는 결정에는 문구가 없다`() {
        assertNull(SpeedPermission.rationaleFor(SpeedPermission.Decision.REQUEST))
        assertNull(SpeedPermission.rationaleFor(SpeedPermission.Decision.DENIED))
    }
}
