package ai.tireon.app.stats

import ai.tireon.core.report.DrivingStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 통계 화면 표시 문구.
 *
 * 문구를 화면이 아니라 상태에서 만든다. JVM에서 시험할 수 있다.
 */
class StatsDisplayTest {

    private fun stats(
        averageScore: Int? = 85,
        scoreChange: Int? = 5,
        improved: Boolean? = true,
        sessionCount: Int = 3,
        totalEvents: Int = 5,
        totalDistanceKm: Float = 12.5f,
    ) = DrivingStats(
        sessionCount = sessionCount,
        averageScore = averageScore,
        totalEvents = totalEvents,
        topEvents = emptyList(),
        totalDistanceKm = totalDistanceKm,
        scoreChange = scoreChange,
        improved = improved,
    )

    @Test
    fun `통계가 없으면 안내 문구를 보여준다`() {
        val display = StatsDisplay.from(null, periodDays = 7)

        assertEquals("최근 7일 기록이 없어요", display.emptyMessage)
        assertEquals(false, display.hasStats)
    }

    @Test
    fun `기록은 있지만 점수가 없으면 기록 없음과 구분한다`() {
        val display = StatsDisplay.from(null, periodDays = 7, hasUnscoredRecords = true)

        assertEquals("최근 7일 기록은 있지만 점수를 낼 수 있는 주행이 없어요", display.emptyMessage)
        assertEquals(false, display.hasStats)
    }

    @Test
    fun `평균 점수를 표시한다`() {
        val display = StatsDisplay.from(stats(), periodDays = 7)

        assertEquals("85", display.averageText)
        assertEquals(true, display.hasStats)
    }

    @Test
    fun `개선되면 긍정 문구를 보여준다`() {
        val display = StatsDisplay.from(stats(scoreChange = 8, improved = true), 7)

        assertEquals("지난주보다 8점 올랐어요", display.changeText)
    }

    @Test
    fun `나빠지면 부정 문구를 보여준다`() {
        val display = StatsDisplay.from(stats(scoreChange = -5, improved = false), 7)

        assertEquals("지난주보다 5점 내렸어요", display.changeText)
    }

    @Test
    fun `변화가 없으면 유지 문구를 보여준다`() {
        val display = StatsDisplay.from(stats(scoreChange = 0, improved = false), 7)

        assertEquals("지난주와 같아요", display.changeText)
    }

    @Test
    fun `비교할 기록이 없으면 변화 문구가 없다`() {
        val display = StatsDisplay.from(stats(scoreChange = null, improved = null), 7)

        assertNull(display.changeText)
    }

    @Test
    fun `주행 횟수를 표시한다`() {
        val display = StatsDisplay.from(stats(sessionCount = 5), 7)

        assertEquals("5회", display.sessionText)
    }

    @Test
    fun `이벤트 횟수를 표시한다`() {
        val display = StatsDisplay.from(stats(totalEvents = 12), 7)

        assertEquals("12회", display.eventText)
    }

    @Test
    fun `거리를 소수 첫째 자리로 표시한다`() {
        val display = StatsDisplay.from(stats(totalDistanceKm = 12.567f), 7)

        assertEquals("12.6km", display.distanceText)
    }
}
