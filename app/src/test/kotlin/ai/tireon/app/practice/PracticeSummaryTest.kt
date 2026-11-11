package ai.tireon.app.practice

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PracticeSummaryTest {

    private fun event(type: DrivingEventType) = DrivingEvent(
        type = type,
        atMs = 0L,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun `점수가 있으면 숫자와 이벤트 수를 보여준다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Scored(DrivingScore(88), emptyList()),
            events = listOf(event(DrivingEventType.HARSH_BRAKE)),
        )

        assertEquals("88", summary.scoreText)
        assertEquals(1, summary.eventCount)
        assertEquals("연습이 끝났어요", summary.message)
    }

    @Test
    fun `점수가 나오면 측정 근거가 붙은 코칭 문장을 싣는다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Scored(DrivingScore(88), emptyList()),
            events = listOf(event(DrivingEventType.HARSH_BRAKE)),
        )

        val brake = summary.coachMessages.first()
        assertTrue(brake.text.startsWith("급제동이 1번"))
        assertTrue(brake.evidence.orEmpty().endsWith("(측정)"))
    }

    @Test
    fun `점수가 없으면 코칭 문장을 싣지 않는다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Insufficient(InsufficientReason.DURATION_TOO_SHORT),
            events = emptyList(),
        )

        assertTrue(summary.coachMessages.isEmpty())
    }

    @Test
    fun `데이터가 부족하면 점수 없이 이유를 안내한다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Insufficient(InsufficientReason.DURATION_TOO_SHORT),
            events = emptyList(),
        )

        assertNull(summary.scoreText)
        assertEquals("1분 이상 연습하면 점수를 낼 수 있어요", summary.message)
        assertEquals(0, summary.eventCount)
    }

    @Test
    fun `샘플 부족은 신호 문제로 안내한다`() {
        val summary = PracticeSummary.from(
            result = ScoreResult.Insufficient(InsufficientReason.NOT_ENOUGH_SAMPLES),
            events = emptyList(),
        )

        assertEquals("차량 신호를 충분히 받지 못했어요", summary.message)
    }
}
