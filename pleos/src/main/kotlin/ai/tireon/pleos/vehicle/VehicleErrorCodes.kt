package ai.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.constant.ErrorCode

/**
 * Vehicle SDK가 `onFailed`로 전달한 예외에서 오류 코드를 안전하게 추출한다.
 *
 * 배경
 * - 공식 문서는 `VehicleException.errorCode` 사용을 안내한다.
 * - 그러나 배포된 Vehicle 2.3.2 AAR은 내부 클래스가 난독화되어 있어
 *   `ai.pleos.playground.vehicle.callback.exception.VehicleException` 타입이 없다.
 *   실제 예외는 같은 패키지의 `a`이며 `a()`가 `ErrorCode`를 반환한다.
 *
 * 설계
 * - 난독화된 이름에 직접 의존하지 않고 리플렉션으로 조회한다.
 * - 조회에 실패하면 null을 반환해 호출 측이 Capability 판정으로 폴백하게 한다.
 *   즉 SDK가 재난독화되어도 크래시하지 않는다.
 */
object VehicleErrorCodes {

    private const val EXCEPTION_CLASS = "ai.pleos.playground.vehicle.callback.exception.VehicleException"
    private const val OBFUSCATED_EXCEPTION_CLASS = "ai.pleos.playground.vehicle.callback.exception.a"
    private const val ERROR_CODE_GETTER = "getErrorCode"
    private const val OBFUSCATED_ERROR_CODE_GETTER = "a"

    /**
     * 문서상 정식 타입인 `VehicleException`이 배포본에 추가되면
     * 이 목록의 첫 항목에서 바로 처리된다.
     */
    private val candidates: List<Pair<Class<*>, java.lang.reflect.Method>> by lazy {
        listOf(
            EXCEPTION_CLASS to ERROR_CODE_GETTER,
            OBFUSCATED_EXCEPTION_CLASS to OBFUSCATED_ERROR_CODE_GETTER,
        ).mapNotNull { (className, methodName) ->
            val type = runCatching { Class.forName(className) }.getOrNull() ?: return@mapNotNull null
            val method = runCatching { type.getMethod(methodName) }.getOrNull() ?: return@mapNotNull null
            type to method
        }
    }

    /** 예외에서 오류 코드를 추출한다. 확인할 수 없으면 null. */
    fun from(exception: Exception): ErrorCode? {
        for ((type, method) in candidates) {
            if (!type.isInstance(exception)) continue
            val code = runCatching { method.invoke(exception) as? ErrorCode }.getOrNull()
            if (code != null) return code
        }
        return null
    }
}
