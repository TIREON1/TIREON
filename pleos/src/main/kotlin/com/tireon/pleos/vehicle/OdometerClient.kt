package com.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.api.Odometer

/**
 * 주행 거리·주행 시간 조회 어댑터.
 *
 * 공식 가이드의 Capability → Get 순서를 따른다.
 * `checkXxxCapabilities`로 읽기 지원 여부를 확인한 뒤에만 값을 요청한다.
 *
 * 주의
 * - Odometer API는 거리 단위(km/mile)를 함께 반환하지 않고, 단위 조회 API도
 *   확인되지 않았다. 따라서 값만 전달하며 호출 측에서 단위를 임의로 붙이지 않는다.
 * - 기능 미지원은 `onFailed`에 `ERROR_NOT_SUPPORTED_PROPERTY`로 전달될 수 있다.
 *   이 오류는 문서상 `VehicleException.errorCode`로 노출되지만 실제 배포된
 *   Vehicle 2.3.2 아티팩트에는 해당 클래스가 없어 코드로 분기할 수 없다.
 *   따라서 미지원 판정은 Capability 결과만 신뢰한다.
 */
class OdometerClient(private val odometer: Odometer) {

    /** 누적 주행 거리를 조회한다. */
    fun readCurrentDistanceDriven(
        onResult: (VehicleResult<Float>) -> Unit,
    ) {
        odometer.checkCurrentDistanceDrivenCapabilities(
            onComplete = { config ->
                if (!config.accessType.canRead()) {
                    onResult(VehicleResult.unsupported(OPERATION_DISTANCE_CAPABILITY, config.accessType))
                    return@checkCurrentDistanceDrivenCapabilities
                }
                odometer.getCurrentDistanceDriven(
                    onComplete = { value -> onResult(value.resultOrEmpty(OPERATION_DISTANCE_GET)) },
                    onFailed = { onResult(VehicleResult.Failure(OPERATION_DISTANCE_GET, it)) },
                )
            },
            onFailed = { onResult(VehicleResult.capabilityUnavailable(OPERATION_DISTANCE_CAPABILITY, it)) },
        )
    }

    /** 현재 트립의 주행 시간을 조회한다. 단위는 API에서 제공되지 않는다. */
    fun readTripCurrentDrivingTime(
        onResult: (VehicleResult<Int>) -> Unit,
    ) {
        odometer.checkTripCurrentDrivingTimeCapabilities(
            onComplete = { config ->
                if (!config.accessType.canRead()) {
                    onResult(VehicleResult.unsupported(OPERATION_TIME_CAPABILITY, config.accessType))
                    return@checkTripCurrentDrivingTimeCapabilities
                }
                odometer.getTripCurrentDrivingTime(
                    onComplete = { value -> onResult(value.resultOrEmpty(OPERATION_TIME_GET)) },
                    onFailed = { onResult(VehicleResult.Failure(OPERATION_TIME_GET, it)) },
                )
            },
            onFailed = { onResult(VehicleResult.capabilityUnavailable(OPERATION_TIME_CAPABILITY, it)) },
        )
    }

    companion object {
        const val OPERATION_DISTANCE_CAPABILITY = "checkCurrentDistanceDrivenCapabilities"
        const val OPERATION_DISTANCE_GET = "getCurrentDistanceDriven"
        const val OPERATION_TIME_CAPABILITY = "checkTripCurrentDrivingTimeCapabilities"
        const val OPERATION_TIME_GET = "getTripCurrentDrivingTime"
    }
}

/**
 * SDK 콜백이 null을 전달하면 "값 없음"으로 처리한다.
 *
 * 0으로 대체하면 측정값과 구분되지 않으므로 실패로 전달한다.
 */
private fun <T : Any> T?.resultOrEmpty(operation: String): VehicleResult<T> =
    if (this == null) {
        VehicleResult.Failure(
            operation,
            IllegalStateException("차량이 값을 반환하지 않았습니다: $operation"),
        )
    } else {
        VehicleResult.Success(this)
    }
