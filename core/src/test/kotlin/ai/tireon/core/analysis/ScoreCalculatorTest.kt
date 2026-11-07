package ai.tireon.core.analysis

import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ScoreCalculatorTest {

    private val calculator = ScoreCalculator()

    private fun event(type: DrivingEventType, atMs: Long = 0L) = DrivingEvent(
        type = type,
        atMs = atMs,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `이벤트가 없으면 100점이다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 5f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(100, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `거리 1km당 이벤트 비율로 감점한다`() {
        // 급제동 2회 / 2km = 1.0회/km * 8점 = 8점 감점
        val result = calculator.calculate(
            events = listOf(event(DrivingEventType.HARSH_BRAKE), event(DrivingEventType.HARSH_BRAKE)),
            distanceKm = 2f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(92, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `거리가 1km 미만이면 1km로 나눈다`() {
        // 급가속 1회 / max(0.8km, 1.0km) = 1.0 * 6점 = 6점 감점
        val result = calculator.calculate(
            events = listOf(event(DrivingEventType.HARSH_ACCEL)),
            distanceKm = 0.8f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(94, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `감점이 100을 넘어도 0점 아래로 내려가지 않는다`() {
        val events = List(50) { event(DrivingEventType.HARSH_BRAKE) }
        val result = calculator.calculate(
            events = events,
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(0, assertIs<ScoreResult.Scored>(result).score.value)
    }

    @Test
    fun `거리가 0_5km 미만이면 점수를 만들지 않는다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 0.4f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(
            InsufficientReason.DISTANCE_TOO_SHORT,
            assertIs<ScoreResult.Insufficient>(result).reason,
        )
    }

    @Test
    fun `주행 시간이 60초 미만이면 점수를 만들지 않는다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 5f,
            durationSec = 59,
            sampleCount = 500,
        )

        assertEquals(
            InsufficientReason.DURATION_TOO_SHORT,
            assertIs<ScoreResult.Insufficient>(result).reason,
        )
    }

    @Test
    fun `유효 샘플이 20개 미만이면 점수를 만들지 않는다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 5f,
            durationSec = 600,
            sampleCount = 19,
        )

        assertEquals(
            InsufficientReason.NOT_ENOUGH_SAMPLES,
            assertIs<ScoreResult.Insufficient>(result).reason,
        )
    }

    @Test
    fun `이벤트 종류별 횟수와 감점을 함께 돌려준다`() {
        val result = calculator.calculate(
            events = listOf(
                event(DrivingEventType.HARSH_BRAKE),
                event(DrivingEventType.HARSH_STEER),
                event(DrivingEventType.HARSH_STEER),
            ),
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 500,
        )

        val breakdown = assertIs<ScoreResult.Scored>(result).breakdown
        val brake = breakdown.first { it.type == DrivingEventType.HARSH_BRAKE }
        val steer = breakdown.first { it.type == DrivingEventType.HARSH_STEER }

        assertEquals(1, brake.count)
        assertEquals(8, brake.penalty)
        assertEquals(2, steer.count)
        assertEquals(10, steer.penalty)
    }

    @Test
    fun `발생하지 않은 이벤트는 breakdown에 넣지 않는다`() {
        val result = calculator.calculate(
            events = listOf(event(DrivingEventType.HARSH_BRAKE)),
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 500,
        )

        val breakdown = assertIs<ScoreResult.Scored>(result).breakdown
        assertTrue(breakdown.none { it.type == DrivingEventType.HARSH_STEER })
    }

    @Test
    fun `감점이 큰 항목을 먼저 정렬한다`() {
        val result = calculator.calculate(
            events = listOf(
                event(DrivingEventType.MISSING_TURN_SIGNAL),
                event(DrivingEventType.HARSH_BRAKE),
            ),
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 500,
        )

        val breakdown = assertIs<ScoreResult.Scored>(result).breakdown
        assertEquals(DrivingEventType.HARSH_BRAKE, breakdown.first().type)
    }
}
