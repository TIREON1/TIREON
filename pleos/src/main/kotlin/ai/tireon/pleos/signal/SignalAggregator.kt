package ai.tireon.pleos.signal

import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal

/**
 * 서로 다른 스레드에서 들어오는 차량 신호를 하나의 샘플로 묶는다.
 *
 * Pleos 리스너들은 각자 다른 스레드에서 콜백을 부른다. 매 콜백마다 샘플을
 * 만들면 시점이 어긋나므로, 마지막 값만 보관하고 요청 시점에 한 번에 읽는다.
 *
 * 초기값은 모두 null이다. 값이 오지 않은 신호를 0으로 채우면 정지 상태나
 * 정상 조향과 구분되지 않는다.
 */
class SignalAggregator(
    private val clock: () -> Long,
) {
    @Volatile
    private var speedMps: Float? = null

    @Volatile
    private var steeringDeg: Float? = null

    @Volatile
    private var brakePressed: Boolean? = null

    @Volatile
    private var turnSignal: TurnSignal? = null

    @Volatile
    private var gear: Gear? = null

    fun updateSpeed(mps: Float?) {
        speedMps = mps
    }

    fun updateSteering(deg: Float?) {
        steeringDeg = deg
    }

    fun updateBrake(pressed: Boolean?) {
        brakePressed = pressed
    }

    fun updateTurnSignal(signal: TurnSignal?) {
        turnSignal = signal
    }

    fun updateGear(value: Gear?) {
        gear = value
    }

    fun snapshot(): DrivingSample = DrivingSample(
        timestampMs = clock(),
        speedMps = speedMps,
        steeringDeg = steeringDeg,
        brakePressed = brakePressed,
        turnSignal = turnSignal,
        gear = gear,
    )

    fun reset() {
        speedMps = null
        steeringDeg = null
        brakePressed = null
        turnSignal = null
        gear = null
    }
}
