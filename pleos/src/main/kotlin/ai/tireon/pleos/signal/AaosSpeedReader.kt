package ai.tireon.pleos.signal

import android.car.Car
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log

/**
 * AAOS 표준 API로 차속을 읽는다.
 *
 * Pleos SDK에는 속도 API가 없다. Pleos는 AAOS Alignment 정책에 따라 표준
 * 데이터를 AAOS Car API로 읽게 설계되어 있다(Vehicle SDK 문서).
 *
 * 단위는 m/s다. 등록에 실패해도 예외를 던지지 않고 false를 반환한다.
 * 속도가 없다고 앱 전체가 멈추면 안 되기 때문이다.
 */
class AaosSpeedReader(context: Context) {

    private val appContext = context.applicationContext
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var callback: CarPropertyManager.CarPropertyEventCallback? = null

    /** @return 등록에 성공하면 true. 실패하면 false를 반환하고 로그만 남긴다. */
    // android.car.jar 1.0.0에는 대체 API가 없어 구 API를 쓴다.
    // minSdk 28과 호환되므로 교체 시점을 별도로 판단한다.
    @Suppress("DEPRECATION")
    fun start(onSpeed: (Float?) -> Unit): Boolean {
        if (callback != null) return true

        val instance = try {
            Car.createCar(appContext)
        } catch (e: Exception) {
            Log.e(TAG, "Car 생성 실패", e)
            return false
        }
        car = instance

        val manager = try {
            instance.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
        } catch (e: Exception) {
            Log.e(TAG, "CarPropertyManager 획득 실패", e)
            null
        }
        if (manager == null) {
            stop()
            return false
        }
        propertyManager = manager

        val listener = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                onSpeed((value.value as? Float))
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.e(TAG, "차속 구독 오류 property=0x%X area=%d".format(propertyId, areaId))
                onSpeed(null)
            }
        }
        callback = listener

        return try {
            manager.registerCallback(listener, PERF_VEHICLE_SPEED, SAMPLE_RATE_HZ)
            true
        } catch (e: Exception) {
            Log.e(TAG, "차속 구독 등록 실패", e)
            callback = null
            stop()
            false
        }
    }

    @Suppress("DEPRECATION")
    fun stop() {
        callback?.let { listener ->
            runCatching { propertyManager?.unregisterCallback(listener) }
        }
        callback = null
        propertyManager = null
        runCatching { car?.disconnect() }
        car = null
    }

    private companion object {
        const val TAG = "AaosSpeedReader"

        /** VehiclePropertyIds.PERF_VEHICLE_SPEED */
        const val PERF_VEHICLE_SPEED = 0x11600207

        /**
         * 요청 주파수는 1Hz로 둔다.
         *
         * 에뮬레이터에서 getCarPropertyConfig가 null을 반환해 지원 범위를 알 수 없다.
         * 실제 갱신 간격은 수신 타임스탬프로 다시 계산해야 한다.
         */
        const val SAMPLE_RATE_HZ = 1f
    }
}
