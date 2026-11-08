package ai.tireon.pleos.vehicle

/**
 * 차량 정보를 조회하는 경계.
 *
 * SDK 타입을 앱 계층으로 노출하지 않기 위한 인터페이스다.
 */
interface VehicleInfoSource {
    /**
     * 차량 서비스 연결 상태와 VIN을 알려준다.
     *
     * 값이 없을 수 있다. 실패해도 예외를 던지지 않고 null을 전달한다.
     */
    fun readInfo(onResult: (connected: Boolean, vin: String?) -> Unit)

    fun release()
}

/**
 * Pleos SDK로 차량 정보를 조회한다.
 */
class PleosVehicleInfoSource(context: android.content.Context) : VehicleInfoSource {

    private val session = VehicleSession(context.applicationContext)

    override fun readInfo(onResult: (connected: Boolean, vin: String?) -> Unit) {
        when (val initialized = session.initialize()) {
            is VehicleResult.Success -> Unit

            is VehicleResult.Failure -> {
                android.util.Log.e(
                    TAG,
                    "차량 초기화 실패: ${initialized.operation}",
                    initialized.cause,
                )
                onResult(false, null)
                return
            }

            is VehicleResult.Unsupported -> {
                onResult(false, null)
                return
            }
        }

        when (val carInfo = session.carInfo()) {
            is VehicleResult.Success -> {
                CarInfoClient(carInfo.value).readVin { vin -> onResult(true, vin) }
            }

            is VehicleResult.Failure -> {
                android.util.Log.e(TAG, "CarInfo 획득 실패: ${carInfo.operation}", carInfo.cause)
                onResult(true, null)
            }

            is VehicleResult.Unsupported -> onResult(true, null)
        }
    }

    override fun release() {
        session.release()
    }

    private companion object {
        const val TAG = "PleosVehicleInfoSource"
    }
}
