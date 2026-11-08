package ai.tireon.pleos.driving

import ai.tireon.pleos.vehicle.OdometerClient
import ai.tireon.pleos.vehicle.VehicleResult
import ai.tireon.pleos.vehicle.VehicleSession
import android.content.Context
import android.util.Log

/**
 * Pleos Vehicle SDK로 주행 데이터를 조회한다.
 *
 * SDK 세션은 이 객체의 생명주기에 묶인다. 비동기 콜백이 끝나기 전에 release를
 * 호출하지 않도록 호출 측이 [release] 시점을 관리해야 한다.
 */
class PleosDrivingDataSource(context: Context) : DrivingDataSource {

    private val session = VehicleSession(context.applicationContext)
    private var initialized = false

    override fun read(onResult: (DrivingData) -> Unit) {
        when (val init = session.initialize()) {
            is VehicleResult.Success -> {
                initialized = true
                readDrivingData(onResult)
            }

            is VehicleResult.Failure -> {
                Log.e(TAG, "Vehicle SDK 초기화 실패: ${init.operation}", init.cause)
                onResult(DrivingData(message = MESSAGE_CONNECT_FAILED))
            }

            is VehicleResult.Unsupported -> {
                onResult(DrivingData(message = MESSAGE_UNAVAILABLE))
            }
        }
    }

    private fun readDrivingData(onResult: (DrivingData) -> Unit) {
        when (val odometer = session.odometer()) {
            is VehicleResult.Success -> {
                val client = OdometerClient(odometer.value)
                var distance: Float? = null
                var tripTime: Int? = null
                var message: String? = null
                var pending = 2

                fun deliver() {
                    pending--
                    if (pending == 0) {
                        onResult(
                            DrivingData(
                                distanceDriven = distance,
                                tripDrivingTime = tripTime,
                                message = message,
                            ),
                        )
                    }
                }

                client.readCurrentDistanceDriven { result ->
                    distance = (result as? VehicleResult.Success)?.value
                    result.failureMessage()?.let { message = it }
                    result.logIfFailed()
                    deliver()
                }

                client.readTripCurrentDrivingTime { result ->
                    tripTime = (result as? VehicleResult.Success)?.value
                    result.failureMessage()?.let { message = it }
                    result.logIfFailed()
                    deliver()
                }
            }

            is VehicleResult.Failure -> {
                Log.e(TAG, "Odometer 획득 실패: ${odometer.operation}", odometer.cause)
                onResult(DrivingData(message = MESSAGE_DATA_UNAVAILABLE))
            }

            is VehicleResult.Unsupported -> {
                onResult(DrivingData(message = MESSAGE_DATA_UNAVAILABLE))
            }
        }
    }

    override fun release() {
        if (!initialized) return
        session.release()
        initialized = false
    }

    private fun VehicleResult<*>.logIfFailed() {
        when (this) {
            is VehicleResult.Failure -> Log.e(
                TAG,
                "Vehicle API 실패: $operation${code?.let { " ($it)" } ?: ""}",
                cause,
            )

            is VehicleResult.Unsupported -> Log.i(TAG, "Vehicle API 미지원: $operation ($detail)")
            is VehicleResult.Success -> Unit
        }
    }

    /** 미지원은 오류가 아니므로 값만 비워 두고, 실제 실패만 안내한다. */
    private fun VehicleResult<*>.failureMessage(): String? = when (this) {
        is VehicleResult.Success, is VehicleResult.Unsupported -> null
        is VehicleResult.Failure -> MESSAGE_LOAD_FAILED
    }

    private companion object {
        const val TAG = "PleosDrivingDataSource"
        const val MESSAGE_CONNECT_FAILED = "차량 서비스에 연결하지 못했습니다."
        const val MESSAGE_UNAVAILABLE = "차량 서비스를 사용할 수 없습니다."
        const val MESSAGE_DATA_UNAVAILABLE = "주행 데이터를 사용할 수 없습니다."
        const val MESSAGE_LOAD_FAILED = "주행 데이터를 불러오지 못했습니다."
    }
}
