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
            requiresSpeed = false,
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
