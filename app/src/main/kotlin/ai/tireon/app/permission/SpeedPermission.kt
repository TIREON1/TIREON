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

    /** 요청할 권한. */
    val REQUIRED: List<String> = listOf("android.car.permission.CAR_SPEED")

    /** 거부됐을 때의 안내 문구. */
    const val DENIED_MESSAGE = "속도 권한이 없어 급가속·급제동을 분석할 수 없어요"

    private const val RATIONALE = "급가속과 급제동을 분석하려면 차량 속도 권한이 필요해요"

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

    /** 요청 직후 결과 판정. */
    fun decideAfterRequest(granted: Boolean): Decision =
        if (granted) Decision.NONE else Decision.DENIED

    /** @return 안내 문구. 안내가 필요 없으면 null. */
    fun rationaleFor(decision: Decision): String? = when (decision) {
        Decision.REQUEST_WITH_RATIONALE -> RATIONALE
        else -> null
    }
}
