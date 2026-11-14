package ai.tireon.app.permission

/**
 * 차속 권한 요청 판정.
 *
 * `android.car.permission.CAR_SPEED`는 protectionLevel이 dangerous라 사용자 승인이
 * 필요하다. 승인 없이는 속도가 null이 되어 급가속·급제동 판정이 불가능하다.
 *
 * 이 클래스는 Android 타입에 의존하지 않는다. 판정 규칙을 JVM에서 시험하기 위함이다.
 */
object SpeedPermission {

    /** 차속 권한 이름. */
    const val PERMISSION = "android.car.permission.CAR_SPEED"

    /** 요청할 권한. */
    val REQUIRED: List<String> = listOf(PERMISSION)

    /** 거부됐을 때의 안내 문구. */
    const val DENIED_MESSAGE = "속도 권한이 없어 급가속·급제동을 분석할 수 없어요"

    private const val RATIONALE = "급가속과 급제동을 분석하려면 차량 속도 권한이 필요해요"

    private const val SETTINGS_RATIONALE =
        "속도 권한이 꺼져 있어요. 설정에서 권한을 켜면 분석할 수 있어요"

    /** 요청 전 판정. */
    enum class Decision {
        /** 요청할 필요가 없다. */
        NONE,

        /** 바로 요청한다. */
        REQUEST,

        /** 이전에 거부했으므로 이유를 먼저 설명한다. */
        REQUEST_WITH_RATIONALE,

        /** 요청 후 거부됐다. */
        DENIED,

        /** 시스템이 더 이상 대화상자를 띄우지 않는다. 설정으로 보내야 한다. */
        OPEN_SETTINGS,
    }

    /**
     * @param granted 이미 승인됐는지
     * @param shouldShowRationale 이전에 거부해서 이유 설명이 필요한지
     */
    fun decide(granted: Boolean, shouldShowRationale: Boolean): Decision = when {
        granted -> Decision.NONE
        shouldShowRationale -> Decision.REQUEST_WITH_RATIONALE
        else -> Decision.REQUEST
    }

    /**
     * 화면이 다시 보일 때의 판정.
     *
     * ViewModel이나 Compose 상태는 화면 전환에도 살아있다. 사용자가 설정에서 권한을
     * 켜고 돌아오는 경우를 놓치지 않으려면 복귀 시점에 시스템 값으로 다시 판정해야 한다.
     *
     * @param grantedNow 지금 승인됐는지
     * @param shouldShowRationale 시스템이 이유 설명을 요구하는지
     * @param askedBefore 이 앱이 이전에 권한을 요청한 적이 있는지
     * @param previouslyGranted 이전에 승인한 적이 있는지
     */
    fun decideOnResume(
        grantedNow: Boolean,
        shouldShowRationale: Boolean,
        askedBefore: Boolean = false,
        previouslyGranted: Boolean = false,
    ): Decision = when {
        grantedNow -> Decision.NONE
        shouldShowRationale -> Decision.REQUEST_WITH_RATIONALE
        // 사용자가 직접 끈 경우에는 대화상자가 다시 뜬다.
        previouslyGranted -> Decision.REQUEST
        // 물어본 적이 있는데 안내도 요구하지 않으면 두 번 거부한 것이다.
        // 이때는 다시 물어봐도 아무 일도 일어나지 않는다.
        askedBefore -> Decision.OPEN_SETTINGS
        else -> Decision.REQUEST
    }

    /** 이유를 설명한 뒤 다시 요청한다. */
    fun decideAfterRationale(): Decision = Decision.REQUEST

    /** 요청 직후 결과 판정. */
    fun decideAfterRequest(granted: Boolean): Decision =
        if (granted) Decision.NONE else Decision.DENIED

    /**
     * 지금 권한을 요청해야 하는지.
     *
     * 요청 중에 또 요청하면 시스템이 무시하거나 두 번 뜬다. 진행 중이면 건드리지 않는다.
     */
    fun shouldRequest(decision: Decision, requestInFlight: Boolean): Boolean =
        decision == Decision.REQUEST && !requestInFlight

    /** @return 안내 문구. 안내가 필요 없으면 null. */
    fun rationaleFor(decision: Decision): String? = when (decision) {
        Decision.REQUEST_WITH_RATIONALE -> RATIONALE
        Decision.OPEN_SETTINGS -> SETTINGS_RATIONALE
        else -> null
    }
}
