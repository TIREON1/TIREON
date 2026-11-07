package ai.tireon.pleos.signal

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import ai.tireon.core.driving.Gear

/**
 * AAOS 표준 API로 기어 상태를 읽는다.
 *
 * Pleos SDK의 `Gear` 도메인은 deprecated이고, 공식 안내가 "Google
 * GEAR_SELECTION property를 쓰라"고 한다. Pleos 문서의 API 선택 기준도
 * 기어를 AAOS 표준 데이터로 분류한다.
 *
 * 등록에 실패해도 예외를 던지지 않고 false를 반환한다.
 */
class AaosGearReader(context: Context) {

    private val appContext = context.applicationContext
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var callback: CarPropertyManager.CarPropertyEventCallback? = null

    /** @return 등록에 성공하면 true. */
    // android.car.jar 1.0.0에는 대체 API가 없어 구 API를 쓴다.
    // minSdk 28과 호환되므로 교체 시점을 별도로 판단한다.
    @Suppress("DEPRECATION")
    fun start(onGear: (Gear?) -> Unit): Boolean {
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
                onGear(toGear(value.value as? Int))
            }

            override fun onErrorEvent(propertyId: Int, areaId: Int) {
                Log.e(TAG, "기어 구독 오류 property=0x%X area=%d".format(propertyId, areaId))
                onGear(null)
            }
        }
        callback = listener

        return try {
            manager.registerCallback(listener, VehiclePropertyIds.GEAR_SELECTION, SAMPLE_RATE_HZ)
            // 등록 직후 현재 값을 한 번 읽어 초기 상태를 채운다.
            runCatching {
                onGear(toGear(manager.getIntProperty(VehiclePropertyIds.GEAR_SELECTION, 0)))
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "기어 구독 등록 실패", e)
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

    private fun toGear(value: Int?): Gear? = when (value) {
        null -> null
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        VehicleGear.GEAR_DRIVE,
        VehicleGear.GEAR_FIRST,
        VehicleGear.GEAR_SECOND,
        VehicleGear.GEAR_THIRD,
        VehicleGear.GEAR_FOURTH,
        VehicleGear.GEAR_FIFTH,
        VehicleGear.GEAR_SIXTH,
        VehicleGear.GEAR_SEVENTH,
        VehicleGear.GEAR_EIGHTH,
        VehicleGear.GEAR_NINTH,
        -> Gear.DRIVE
        else -> Gear.UNKNOWN
    }

    private companion object {
        const val TAG = "AaosGearReader"
        const val SAMPLE_RATE_HZ = 1f
    }
}
