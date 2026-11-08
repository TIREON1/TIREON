package ai.tireon.pleos.signal

import ai.pleos.playground.vehicle.api.Brake
import ai.pleos.playground.vehicle.api.SteeringWheel
import ai.pleos.playground.vehicle.api.TurnSignal
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import ai.tireon.pleos.vehicle.VehicleResult
import ai.tireon.pleos.vehicle.VehicleSession
import android.content.Context

/**
 * Pleos 리스너 등록과 해제를 한곳에서 관리한다.
 *
 * 리스너는 세션이 활성일 때만 등록하고, 종료할 때 반드시 해제한다.
 * 재등록을 막아 이벤트가 배로 계산되는 것을 방지한다.
 *
 * 기어와 속도는 AAOS 표준 프로퍼티를 쓴다. Pleos SDK의 기어 도메인은
 * deprecated이고 공식 안내가 AAOS `GEAR_SELECTION`을 가리킨다.
 */
internal class PleosSignalBinding(
    context: Context,
    private val aggregator: SignalAggregator,
) {
    private val gearReader = AaosGearReader(context)
    private val speedReader = AaosSpeedReader(context)

    private var registered = false

    private var brakeDomain: Brake? = null
    private var steeringDomain: SteeringWheel? = null
    private var turnSignalDomain: TurnSignal? = null

    private var brakeListener: BrakePedalStatusListener? = null
    private var steeringListener: SteeringWheelAngleListener? = null
    private var turnSignalListener: TurnSignalStateListener? = null

    /** @return 하나라도 등록에 성공하면 true. */
    fun register(session: VehicleSession): Boolean {
        if (registered) return true

        var anyRegistered = false

        if (gearReader.start { gear -> aggregator.updateGear(gear) }) {
            anyRegistered = true
        }

        if (speedReader.start { mps -> aggregator.updateSpeed(mps) }) {
            anyRegistered = true
        }

        (session.brake() as? VehicleResult.Success)?.let { result ->
            brakeDomain = result.value
            val listener = object : BrakePedalStatusListener {
                override fun onBrakePedalStatusUpdated(status: Boolean?) {
                    aggregator.updateBrake(status)
                }

                override fun onFailed(e: Exception) {
                    aggregator.updateBrake(null)
                }
            }
            brakeListener = listener
            runCatching { result.value.registerBrakePedalStatus(listener) }
                .onSuccess { anyRegistered = true }
        }

        (session.steeringWheel() as? VehicleResult.Success)?.let { result ->
            steeringDomain = result.value
            val listener = object : SteeringWheelAngleListener {
                override fun onSteeringWheelAngleUpdated(angle: Float?) {
                    aggregator.updateSteering(SignalMapper.steering(angle))
                }

                override fun onFailed(e: Exception) {
                    aggregator.updateSteering(null)
                }
            }
            steeringListener = listener
            runCatching { result.value.registerSteeringWheelAngle(listener) }
                .onSuccess { anyRegistered = true }
        }

        (session.turnSignal() as? VehicleResult.Success)?.let { result ->
            turnSignalDomain = result.value
            val listener = object : TurnSignalStateListener {
                override fun onTurnSignalStateUpdated(
                    state: ai.pleos.playground.vehicle.constant.area.TurnSignalValue?,
                ) {
                    aggregator.updateTurnSignal(SignalMapper.turnSignal(state))
                }

                override fun onFailed(e: Exception) {
                    aggregator.updateTurnSignal(null)
                }
            }
            turnSignalListener = listener
            runCatching { result.value.registerTurnSignalState(listener) }
                .onSuccess { anyRegistered = true }
        }

        registered = anyRegistered
        return anyRegistered
    }

    fun unregister() {
        brakeListener?.let { listener ->
            runCatching { brakeDomain?.unregisterBrakePedalStatus(listener) }
        }
        steeringListener?.let { listener ->
            runCatching { steeringDomain?.unregisterSteeringWheelAngle(listener) }
        }
        turnSignalListener?.let { listener ->
            runCatching { turnSignalDomain?.unregisterTurnSignalState(listener) }
        }
        brakeListener = null
        steeringListener = null
        turnSignalListener = null
        brakeDomain = null
        steeringDomain = null
        turnSignalDomain = null

        gearReader.stop()
        speedReader.stop()
        registered = false
    }
}
