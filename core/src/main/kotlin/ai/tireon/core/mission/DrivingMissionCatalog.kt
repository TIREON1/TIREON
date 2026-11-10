package ai.tireon.core.mission

/** 주행 미션이 판정에 요구하는 신호. */
data class DrivingMissionSpec(
    val mission: DrivingMission,
    val requiresSpeed: Boolean,
    val requiresSteering: Boolean,
    val requiresTurnSignal: Boolean,
)

/**
 * 주행 미션 정의.
 *
 * 화면 문구는 여기 두지 않는다. `:core`는 Android 리소스에 의존하지 않으므로,
 * 표시 문구는 앱 계층에서 미션별로 매핑한다.
 */
object DrivingMissionCatalog {

    /** 미션을 고르지 않고 주행할 수 있는지. */
    const val ALLOW_SKIP: Boolean = true

    private val specs = listOf(
        DrivingMissionSpec(
            mission = DrivingMission.SMOOTH_DRIVING,
            requiresSpeed = true,
            requiresSteering = false,
            requiresTurnSignal = false,
        ),
        DrivingMissionSpec(
            mission = DrivingMission.SIGNAL_HABIT,
            requiresSpeed = true,
            requiresSteering = false,
            requiresTurnSignal = true,
        ),
        DrivingMissionSpec(
            mission = DrivingMission.STABLE_STEERING,
            requiresSpeed = true,
            requiresSteering = true,
            requiresTurnSignal = false,
        ),
        DrivingMissionSpec(
            mission = DrivingMission.OVERALL,
            requiresSpeed = true,
            requiresSteering = true,
            requiresTurnSignal = true,
        ),
    )

    /** @return 모든 주행 미션. */
    fun all(): List<DrivingMissionSpec> = specs

    fun of(mission: DrivingMission): DrivingMissionSpec =
        specs.first { it.mission == mission }
}
