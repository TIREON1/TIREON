package ai.tireon.core.coach

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.EventContribution
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoachMessagesTest {

    private val coach = CoachMessages()

    private fun event(type: DrivingEventType, value: Float, atMs: Long = 1_000L) = DrivingEvent(
        type = type,
        atMs = atMs,
        value = value,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `급제동 횟수와 측정 근거를 함께 전달한다`() {
        val events = listOf(
            event(DrivingEventType.HARSH_BRAKE, -3.2f),
            event(DrivingEventType.HARSH_BRAKE, -4.1f),
        )
        val result = ScoreResult.Scored(
            score = DrivingScore(84),
            breakdown = listOf(EventContribution(DrivingEventType.HARSH_BRAKE, 2, 16)),
        )

        val message = coach.build(events, result).first { it.text.contains("급제동") }

        assertTrue(message.text.contains("2"))
        assertTrue(message.evidence!!.contains("4.1"))
    }

    @Test
    fun `발생하지 않은 항목은 칭찬 문장으로 만든다`() {
        val result = ScoreResult.Scored(
            score = DrivingScore(100),
            breakdown = emptyList(),
        )

        val messages = coach.build(emptyList(), result)

        assertTrue(messages.any { it.text.contains("부드러") || it.text.contains("좋") })
    }

    @Test
    fun `점수와 다음 목표를 한 문장으로 만든다`() {
        val result = ScoreResult.Scored(
            score = DrivingScore(85),
            breakdown = listOf(EventContribution(DrivingEventType.HARSH_BRAKE, 1, 8)),
        )

        val messages = coach.build(emptyList(), result)

        assertTrue(messages.any { it.text.contains("85") })
    }

    @Test
    fun `데이터 부족은 점수 문장을 만들지 않는다`() {
        val result = ScoreResult.Insufficient(InsufficientReason.DISTANCE_TOO_SHORT)

        val messages = coach.build(emptyList(), result)

        assertTrue(messages.none { it.text.contains("점이에요") })
        assertTrue(messages.any { it.text.contains("주행") })
    }

    @Test
    fun `모든 메시지의 문장이 비어 있지 않다`() {
        val events = listOf(event(DrivingEventType.HARSH_STEER, 250f))
        val result = ScoreResult.Scored(
            score = DrivingScore(95),
            breakdown = listOf(EventContribution(DrivingEventType.HARSH_STEER, 1, 5)),
        )

        val messages = coach.build(events, result)

        assertTrue(messages.isNotEmpty())
        assertTrue(messages.all { it.text.isNotBlank() })
    }

    @Test
    fun `위험도가 높은 항목을 먼저 보여준다`() {
        val events = listOf(
            event(DrivingEventType.MISSING_TURN_SIGNAL, 45f),
            event(DrivingEventType.HARSH_BRAKE, -5f),
        )
        val result = ScoreResult.Scored(
            score = DrivingScore(80),
            breakdown = listOf(
                EventContribution(DrivingEventType.HARSH_BRAKE, 1, 8),
                EventContribution(DrivingEventType.MISSING_TURN_SIGNAL, 1, 4),
            ),
        )

        val messages = coach.build(events, result)
        val firstIssue = messages.first { it.severity != Severity.GOOD }

        assertEquals(Severity.CRITICAL, firstIssue.severity)
        assertTrue(firstIssue.text.contains("급제동"))
    }

    @Test
    fun `점수 문장은 항상 마지막에 온다`() {
        val events = listOf(event(DrivingEventType.HARSH_BRAKE, -3.5f))
        val result = ScoreResult.Scored(
            score = DrivingScore(90),
            breakdown = listOf(EventContribution(DrivingEventType.HARSH_BRAKE, 1, 8)),
        )

        val messages = coach.build(events, result)

        assertTrue(messages.last().text.contains("90"))
    }
}
