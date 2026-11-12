package ai.tireon.app.permission

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 기록 중 알림 권한 판정.
 *
 * 승인 없이도 기록은 계속되지만, 사용자가 기록 중임을 알 수 있는 유일한 표시가
 * 사라진다. 요청 여부를 API 레벨과 승인 상태로만 결정한다.
 */
class NotificationPermissionTest {

    @Test
    fun `Android 13 이상에서 승인 전이면 요청한다`() {
        assertTrue(NotificationPermission.shouldRequest(sdkInt = 33, granted = false))
        assertTrue(NotificationPermission.shouldRequest(sdkInt = 34, granted = false))
    }

    @Test
    fun `이미 승인했으면 요청하지 않는다`() {
        assertFalse(NotificationPermission.shouldRequest(sdkInt = 34, granted = true))
    }

    @Test
    fun `Android 13 미만에서는 요청하지 않는다`() {
        assertFalse(NotificationPermission.shouldRequest(sdkInt = 32, granted = false))
        assertFalse(NotificationPermission.shouldRequest(sdkInt = 28, granted = false))
    }
}
