package ai.tireon.core.driving

/**
 * 한 시점의 차량 신호 묶음.
 *
 * 각 값은 nullable이다. Pleos SDK가 값을 주지 못하는 경우가 실제로 있으며,
 * 측정 불가를 0으로 대체하면 유효한 측정값과 구분되지 않기 때문이다.
 */
data class DrivingSample(
    val timestampMs: Long,
    val speedMps: Float?,
    /**
     * 조향각. 양수는 좌회전, 음수는 우회전이다.
     *
     * 수집 계층이 SDK 부호를 이 규약에 맞춰 정규화한다.
     */
    val steeringDeg: Float?,
    val brakePressed: Boolean?,
    val turnSignal: TurnSignal?,
    val gear: Gear?,
)

enum class TurnSignal { OFF, LEFT, RIGHT }

enum class Gear { PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN }
