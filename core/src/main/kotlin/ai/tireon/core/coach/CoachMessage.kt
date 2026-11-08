package ai.tireon.core.coach

enum class Severity { GOOD, WARN, CRITICAL }

/**
 * @param evidence 판정에 쓴 측정값. 화면에서 문장과 분리해 보여주기 위한 값으로,
 *   측정값과 해석을 섞지 않기 위해 문장에 넣지 않는다.
 */
data class CoachMessage(
    val text: String,
    val severity: Severity,
    val evidence: String?,
)
