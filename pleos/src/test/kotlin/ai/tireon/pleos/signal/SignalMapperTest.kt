package ai.tireon.pleos.signal

import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SignalMapperTest {

    @Test
    fun `기어 값을 도메인 기어로 바꾼다`() {
        assertEquals(Gear.PARK, SignalMapper.gear(VehicleGear.GEAR_PARK))
        assertEquals(Gear.REVERSE, SignalMapper.gear(VehicleGear.GEAR_REVERSE))
        assertEquals(Gear.NEUTRAL, SignalMapper.gear(VehicleGear.GEAR_NEUTRAL))
        assertEquals(Gear.DRIVE, SignalMapper.gear(VehicleGear.GEAR_DRIVE))
    }

    @Test
    fun `숫자 기어는 드라이브로 본다`() {
        assertEquals(Gear.DRIVE, SignalMapper.gear(VehicleGear.GEAR_1))
        assertEquals(Gear.DRIVE, SignalMapper.gear(VehicleGear.GEAR_6))
    }

    @Test
    fun `알 수 없는 기어는 UNKNOWN이다`() {
        assertEquals(Gear.UNKNOWN, SignalMapper.gear(VehicleGear.GEAR_UNKNOWN))
        assertNull(SignalMapper.gear(null))
    }

    @Test
    fun `방향지시등 값을 도메인 값으로 바꾼다`() {
        assertEquals(TurnSignal.OFF, SignalMapper.turnSignal(TurnSignalValue.OFF))
        assertEquals(TurnSignal.LEFT, SignalMapper.turnSignal(TurnSignalValue.LEFT))
        assertEquals(TurnSignal.RIGHT, SignalMapper.turnSignal(TurnSignalValue.RIGHT))
        assertNull(SignalMapper.turnSignal(null))
    }

    @Test
    fun `조향각 부호를 정규화한다`() {
        // Pleos SDK는 주입값의 부호를 반전해 전달한다. 좌회전이 양수가 되도록 맞춘다.
        assertEquals(120f, SignalMapper.steering(-120f))
        assertEquals(-200f, SignalMapper.steering(200f))
    }

    @Test
    fun `조향각이 없으면 null이다`() {
        assertNull(SignalMapper.steering(null))
    }
}
