package com.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.api.model.capability.AccessType

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
     */
    data class Failure(
        val operation: String,
        val cause: Exception,
    ) : VehicleResult<Nothing>

    companion object {
        /** Capability가 읽기를 허용하지 않는 경우. */
        fun unsupported(operation: String, accessType: AccessType): Unsupported =
            Unsupported(
                operation = operation,
                detail = "accessType=$accessType",
            )

        /**
         * Capability 조회 자체가 실패한 경우.
         *
         * SDK는 미지원 기능을 onFailed로 알리므로 미지원으로 분류한다.
         * 오류 코드로 분기할 수 없어(문서의 VehicleException 미제공) 예외 메시지를 보존한다.
         */
        fun capabilityUnavailable(operation: String, cause: Exception): Unsupported =
            Unsupported(
                operation = operation,
                detail = cause.message ?: cause::class.java.simpleName,
            )
    }
}
