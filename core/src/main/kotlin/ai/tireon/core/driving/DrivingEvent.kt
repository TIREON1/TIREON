package ai.tireon.core.driving

enum class DrivingEventType {
    HARSH_ACCEL,
    HARSH_BRAKE,
    HARSH_STEER,
    MISSING_TURN_SIGNAL,
}

/** 이벤트의 근거가 실제 측정값인지 추정값인지 구분한다. */
enum class Confidence { MEASURED, ESTIMATED }

/**
 * @param value 판정에 사용한 실측값. 단위는 이벤트 종류마다 다르다.
 *   가속도는 m/s², 급조향은 °/s, 방향지시등 누락은 조향 변화량(°).
 */
data class DrivingEvent(
    val type: DrivingEventType,
    val atMs: Long,
    val value: Float,
    val confidence: Confidence,
)
