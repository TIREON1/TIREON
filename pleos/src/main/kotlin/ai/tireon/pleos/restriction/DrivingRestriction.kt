package ai.tireon.pleos.restriction

/**
 * 주행 중 UI 노출 수준.
 *
 * [MINIMAL_UI]는 숫자 하나만 보여주고 터치 요소를 두지 않는 상태다.
 */
enum class DrivingRestriction {
    NONE,
    MINIMAL_UI,
}

/**
 * 플랫폼의 주행 제한 상태를 UI 정책으로 바꾼다.
 *
 * AAOS `CarUxRestrictions`를 직접 받지 않고 boolean만 받는 이유는
 * Android 타입 없이 JVM에서 검증하기 위함이다.
 *
 * 제한 상태를 확인하지 못했을 때(null) 전체 UI를 띄우면 안전 규정을 위반할 수
 * 있으므로, 모르는 상태는 제한이 걸린 것으로 본다.
 */
object RestrictionPolicy {

    fun of(requiresDistractionOptimization: Boolean?): DrivingRestriction =
        if (requiresDistractionOptimization == false) {
            DrivingRestriction.NONE
        } else {
            DrivingRestriction.MINIMAL_UI
        }
}
