package ai.tireon.core.mission

/**
 * 연습 미션 정의.
 *
 * 화면 문구는 여기 두지 않는다. `:core`는 Android 리소스에 의존하지 않으므로,
 * 표시 문구는 앱 계층에서 미션별로 매핑한다.
 */
object MissionCatalog {

    private val specs = listOf(
        MissionSpec(
            mission = PracticeMission.SMOOTH_START,
            requiresSpeed = true,
            requiresSteering = false,
            requiresGear = false,
            requiresTurnSignal = false,
        ),
        MissionSpec(
            mission = PracticeMission.SMOOTH_STOP,
            requiresSpeed = true,
            requiresSteering = false,
            requiresGear = false,
            requiresTurnSignal = false,
            requiresBrake = true,
        ),
        MissionSpec(
            mission = PracticeMission.REVERSE_SENSE,
            requiresSpeed = true,
            requiresSteering = false,
            requiresGear = true,
            requiresTurnSignal = false,
        ),
        MissionSpec(
            mission = PracticeMission.TURN_SIGNAL,
            // 회전 여부는 속도로 도로 주행을 확인해야 판정할 수 있다. 속도가 없으면
            // 회전을 못 했다고 단정하지 않고 판정 불가로 돌려준다.
            requiresSpeed = true,
            requiresSteering = true,
            requiresGear = false,
            requiresTurnSignal = true,
        ),
        MissionSpec(
            mission = PracticeMission.STEADY_SPEED,
            requiresSpeed = true,
            requiresSteering = false,
            requiresGear = false,
            requiresTurnSignal = false,
        ),
        MissionSpec(
            mission = PracticeMission.RIGHT_TURN_STOP,
            requiresSpeed = true,
            requiresSteering = true,
            requiresGear = false,
            requiresTurnSignal = true,
        ),
    )

    /** 화면에 보여줄 순서. */
    fun all(): List<PracticeMission> = specs.map { it.mission }

    fun spec(mission: PracticeMission): MissionSpec =
        specs.first { it.mission == mission }
}
