package ai.tireon.core.analysis

import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.driving.DrivingSample
import ai.tireon.core.driving.Gear
import ai.tireon.core.driving.TurnSignal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 입력 경계에서의 방어를 고정한다.
 *
 * 차량 데이터는 신뢰할 수 없는 입력으로 취급한다. 물리적으로 불가능하거나
 * 형식이 깨진 값이 들어와도 예외로 앱이 멈추거나, 없는 점수를 만들어내면 안 된다.
 */
class ScoreCalculatorInputTest {

    private val calculator = ScoreCalculator()

    @Test
    fun `거리가 NaN이면 예외 없이 데이터 부족으로 처리한다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = Float.NaN,
            durationSec = 600,
            sampleCount = 500,
        )

        assertIs<ScoreResult.Insufficient>(result)
    }

    @Test
    fun `거리가 무한대이면 예외 없이 데이터 부족으로 처리한다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = Float.POSITIVE_INFINITY,
            durationSec = 600,
            sampleCount = 500,
        )

        assertIs<ScoreResult.Insufficient>(result)
    }

    @Test
    fun `거리가 음수이면 데이터 부족으로 처리한다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = -3f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertEquals(
            InsufficientReason.DISTANCE_TOO_SHORT,
            assertIs<ScoreResult.Insufficient>(result).reason,
        )
    }

    @Test
    fun `거리 하한과 정확히 같으면 점수를 낸다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 0.5f,
            durationSec = 600,
            sampleCount = 500,
        )

        assertIs<ScoreResult.Scored>(result)
    }

    @Test
    fun `주행 시간 하한과 정확히 같으면 점수를 낸다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 1f,
            durationSec = 60,
            sampleCount = 500,
        )

        assertIs<ScoreResult.Scored>(result)
    }

    @Test
    fun `샘플 수 하한과 정확히 같으면 점수를 낸다`() {
        val result = calculator.calculate(
            events = emptyList(),
            distanceKm = 1f,
            durationSec = 600,
            sampleCount = 20,
        )

        assertIs<ScoreResult.Scored>(result)
    }

    @Test
    fun `아주 먼 거리에서도 점수가 범위를 벗어나지 않는다`() {
        val events = List(100) {
            ai.tireon.core.driving.DrivingEvent(
                type = DrivingEventType.HARSH_BRAKE,
                atMs = it.toLong(),
                value = 3f,
                confidence = ai.tireon.core.driving.Confidence.MEASURED,
            )
        }

        val result = calculator.calculate(
            events = events,
            distanceKm = 1_000_000f,
            durationSec = 100_000,
            sampleCount = 100_000,
        )

        val score = assertIs<ScoreResult.Scored>(result).score.value
        assertTrue(score in 0..100)
    }

    @Test
    fun `감점이 0인 항목은 점수 문장의 개선 제안에서 제외한다`() {
        // 30km 주행에서 급제동 1회는 penalty가 0으로 반올림된다.
        val coach = ai.tireon.core.coach.CoachMessages()
        val events = listOf(
            ai.tireon.core.driving.DrivingEvent(
                type = DrivingEventType.HARSH_BRAKE,
                atMs = 0L,
                value = 3f,
                confidence = ai.tireon.core.driving.Confidence.MEASURED,
            ),
        )
        val result = calculator.calculate(
            events = events,
            distanceKm = 30f,
            durationSec = 2_000,
            sampleCount = 10_000,
        )

        val scoreMessage = coach.build(events, result).last()

        assertTrue(
            !scoreMessage.text.contains("줄이면"),
            "감점이 없는 항목을 개선하라고 하면 안 된다: ${scoreMessage.text}",
        )
    }
}
