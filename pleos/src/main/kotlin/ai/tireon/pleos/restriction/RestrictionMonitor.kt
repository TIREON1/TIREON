package ai.tireon.pleos.restriction

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 플랫폼의 주행 제한 상태를 흘려보낸다.
 *
 * 제한을 확인하지 못하면 `null`을 흘린다. 호출 측은 이를 최소 UI로 해석해야 한다.
 *
 * `CarUxRestrictions`가 활성화되지 않은 환경(예: 제한이 꺼진 에뮬레이터)에서는
 * 콜백이 한 번만 오거나 오지 않을 수 있다. 그래서 구독 시작 시 현재 값을 한 번
 * 읽어 즉시 전달한다.
 */
class RestrictionMonitor(context: Context) {

    private val appContext = context.applicationContext

    fun observe(): Flow<DrivingRestriction> = callbackFlow {
        var car: Car? = null
        var manager: CarUxRestrictionsManager? = null

        try {
            val instance = Car.createCar(appContext)
            car = instance
            manager = instance.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE)
                as? CarUxRestrictionsManager

            val active = manager
            if (active == null) {
                trySend(DrivingRestriction.MINIMAL_UI)
            } else {
                trySend(toRestriction(active.currentCarUxRestrictions))
                active.registerListener { restrictions ->
                    trySend(toRestriction(restrictions))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "제한 상태를 읽지 못했습니다", e)
            trySend(DrivingRestriction.MINIMAL_UI)
        }

        awaitClose {
            runCatching { manager?.unregisterListener() }
            runCatching { car?.disconnect() }
        }
    }.distinctUntilChanged()

    private fun toRestriction(restrictions: CarUxRestrictions?): DrivingRestriction =
        RestrictionPolicy.of(restrictions?.isRequiresDistractionOptimization)

    private companion object {
        const val TAG = "RestrictionMonitor"
    }
}
