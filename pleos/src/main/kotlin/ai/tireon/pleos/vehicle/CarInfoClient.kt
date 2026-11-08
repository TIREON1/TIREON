package ai.tireon.pleos.vehicle

import ai.pleos.playground.vehicle.api.CarInfo

/**
 * 차량 식별 정보 조회 어댑터.
 *
 * VIN은 차량 고유 식별자다. 개인정보는 아니지만 기기 밖으로 내보내지 않는다.
 * 값이 없으면 null을 돌려주고, 0이나 빈 문자열로 대체하지 않는다.
 */
class CarInfoClient(private val carInfo: CarInfo) {

    /** @return VIN. 미지원이거나 실패하면 null. */
    fun readVin(onResult: (String?) -> Unit) {
        carInfo.checkVINInfoCapabilities(
            onComplete = { config ->
                if (!config.accessType.canRead()) {
                    onResult(null)
                    return@checkVINInfoCapabilities
                }
                carInfo.getVINInfo(
                    onComplete = { vin -> onResult(vin?.takeIf { it.isNotBlank() }) },
                    onFailed = { onResult(null) },
                )
            },
            onFailed = { onResult(null) },
        )
    }

    companion object {
        const val OPERATION_VIN_CAPABILITY = "checkVINInfoCapabilities"
        const val OPERATION_VIN_GET = "getVINInfo"
    }
}
