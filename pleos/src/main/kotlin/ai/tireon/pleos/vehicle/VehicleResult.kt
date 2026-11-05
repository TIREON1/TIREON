package ai.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.ErrorCode

/**
 * Pleos Vehicle SDK 사용 결과.
 *
 * SDK의 Capability·Get 호출은 비동기 콜백으로 결과를 전달하므로,
 * 도메인 계층에서는 성공·미지원·실패를 명시적으로 구분한다.
 */
sealed interface VehicleResult<out T> {
    data class Success<T>(val value: T) : VehicleResult<T>

    /**
     * 차량 또는 기능이 해당 데이터를 제공하지 않는 경우.
     *
     * 사용자 오류가 아니므로 화면에는 값 없음으로만 표시하고,
     * 오류 안내 문구는 띄우지 않는다.
     */
    data class Unsupported(
        val operation: String,
        val detail: String,
    ) : VehicleResult<Nothing>

    /**
     * @param operation 실패한 작업 이름 (로그·진단용)
     * @param cause SDK가 onFailed로 전달한 예외
     * @param code 예외에서 추출한 오류 코드 (확인 불가 시 null)
     */
    data class Failure(
        val operation: String,
        val cause: Exception,
        val code: ErrorCode? = null,
    ) : VehicleResult<Nothing>

    companion object {
        /** Capability가 읽기를 허용하지 않는 경우. */
        fun unsupported(operation: String, accessType: AccessType): Unsupported =
            Unsupported(
                operation = operation,
                detail = "accessType=$accessType",
            )
    }
}

/**
 * SDK 예외를 결과로 변환한다.
 *
 * 오류 코드로 미지원 여부를 판별할 수 있으면 [VehicleResult.Unsupported]로,
 * 그 외는 [VehicleResult.Failure]로 전달한다.
 */
internal fun failed(
    operation: String,
    cause: Exception,
): VehicleResult<Nothing> {
    val code = VehicleErrorCodes.from(cause)
    return when (code) {
        ErrorCode.ERROR_NOT_SUPPORTED_PROPERTY,
        ErrorCode.ERROR_NOT_SUPPORTED_AREA,
        -> VehicleResult.Unsupported(operation = operation, detail = code.name)

        else -> VehicleResult.Failure(operation = operation, cause = cause, code = code)
    }
}
