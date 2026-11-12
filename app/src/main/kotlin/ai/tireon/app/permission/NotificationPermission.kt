package ai.tireon.app.permission

/**
 * 기록 중 알림 권한 판정.
 *
 * Android 13부터 포그라운드 서비스 알림을 보려면 `POST_NOTIFICATIONS` 승인이
 * 필요하다. targetSdk가 33 이상이면 자동으로 부여되지 않으므로 직접 요청한다.
 * 승인 없이도 서비스와 수집은 정상 동작하지만, 기록 중이라는 사실을 알릴 방법이
 * 사라진다.
 *
 * 이 클래스는 Android 타입에 의존하지 않는다. 판정 규칙을 JVM에서 시험하기 위함이다.
 */
object NotificationPermission {

    /** 요청할 권한. */
    const val REQUIRED = "android.permission.POST_NOTIFICATIONS"

    /** 런타임 권한이 생기기 전 API 레벨. */
    const val RUNTIME_PERMISSION_API = 33

    /**
     * 알림 권한을 요청해야 하는지.
     *
     * @param sdkInt 기기의 API 레벨
     * @param granted 지금 승인됐는지
     */
    fun shouldRequest(sdkInt: Int, granted: Boolean): Boolean =
        sdkInt >= RUNTIME_PERMISSION_API && !granted
}
