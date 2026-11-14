package ai.tireon.pleos.signal

import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal

/**
 * Pleos SDK 값을 도메인 타입으로 바꾼다.
 *
 * `internal`인 이유는 SDK 타입이 공개 API로 새지 않게 하기 위함이다.
 */
internal object SignalMapper {

    fun gear(value: VehicleGear?): Gear? = when (value) {
        null -> null
        VehicleGear.GEAR_PARK -> Gear.PARK
        VehicleGear.GEAR_REVERSE -> Gear.REVERSE
        VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
        VehicleGear.GEAR_DRIVE,
        VehicleGear.GEAR_1,
        VehicleGear.GEAR_2,
        VehicleGear.GEAR_3,
        VehicleGear.GEAR_4,
        VehicleGear.GEAR_5,
        VehicleGear.GEAR_6,
        VehicleGear.GEAR_7,
        VehicleGear.GEAR_8,
        VehicleGear.GEAR_9,
        -> Gear.DRIVE
        VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
    }

    fun turnSignal(value: TurnSignalValue?): TurnSignal? = when (value) {
        null -> null
        TurnSignalValue.OFF -> TurnSignal.OFF
        TurnSignalValue.LEFT -> TurnSignal.LEFT
        TurnSignalValue.RIGHT -> TurnSignal.RIGHT
    }

    /**
     * 조향각 부호를 정규화한다.
     *
     * Pleos SDK는 주입한 값의 부호를 반전해 전달한다(스파이크 실측).
     * 좌회전이 양수가 되도록 여기서 부호를 뒤집는다.
     */
    fun steering(rawDeg: Float?): Float? = rawDeg?.let { -it }
}
