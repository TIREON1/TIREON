package ai.tireon.app.permission

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 런타임 권한 요청 결과 해석.
 *
 * 차속 권한은 dangerous라 사용자 승인이 필요하다. 거부되면 속도가 null이 되어
 * 급가속·급제동 판정이 불가능해진다. 그 사실을 사용자에게 알려야 한다.
 */
class SpeedPermissionTest {

    @Test
    fun `권한이 있으면 요청하지 않는다`() {
        val decision = SpeedPermission.decide(granted = true, shouldShowRationale = false)

        assertEquals(SpeedPermission.Decision.NONE, decision)
    }

    @Test
    fun `권한이 없으면 요청한다`() {
        val decision = SpeedPermission.decide(granted = false, shouldShowRationale = false)

        assertEquals(SpeedPermission.Decision.REQUEST, decision)
    }

    @Test
    fun `거부 후에는 이유를 안내한다`() {
        val decision = SpeedPermission.decide(granted = false, shouldShowRationale = true)

        assertEquals(SpeedPermission.Decision.REQUEST_WITH_RATIONALE, decision)
    }

    @Test
    fun `요청 없이 판정할 수 있다`() {
        assertEquals(
            SpeedPermission.Decision.NONE,
            SpeedPermission.decideAfterRequest(granted = true),
        )
        assertEquals(
            SpeedPermission.Decision.DENIED,
            SpeedPermission.decideAfterRequest(granted = false),
        )
    }

    @Test
    fun `거부 안내 문구는 기능 제한을 설명한다`() {
        val message = SpeedPermission.DENIED_MESSAGE

        assertTrue(message.contains("속도"))
        assertTrue(message.contains("권한"))
    }

    @Test
    fun `필요 권한 목록이 비어 있지 않다`() {
        val permissions = SpeedPermission.REQUIRED

        assertTrue(permissions.isNotEmpty())
        assertTrue(permissions.all { it.startsWith("android.car.permission.") })
    }

    @Test
    fun `권한 이름이 차속이다`() {
        assertEquals(
            "android.car.permission.CAR_SPEED",
            SpeedPermission.REQUIRED.first(),
        )
    }

    @Test
    fun `안내가 필요 없는 경우 문구가 없다`() {
        assertNull(SpeedPermission.rationaleFor(SpeedPermission.Decision.NONE))
    }

    @Test
    fun `근거 안내가 필요한 경우 문구가 있다`() {
        val message = SpeedPermission.rationaleFor(SpeedPermission.Decision.REQUEST_WITH_RATIONALE)

        assertTrue(message != null && message.contains("속도"))
    }
}
