package ai.tireon.core.report

// EventLine은 같은 패키지에 있다
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 기간별 주행 통계를 계산한다.
 *
 * "TIREON을 쓰면 운전이 좋아진다"를 증명하려면 변화 추이가 보여야 한다.
 * 점수가 없는 기록은 평균에서 제외한다. 0점으로 넣으면 평균이 왜곡된다.
 */
class DrivingStatsTest {

    private fun record(
        id: String = "r1",
        startedAtMs: Long,
        score: Int? = 80,
        eventCounts: Map<String, Int> = emptyMap(),
        distanceKm: Float? = 1f,
    ) = DrivingSessionRecord(
        id = id,
        startedAtMs = startedAtMs,
        durationSec = 60,
        distanceKm = distanceKm,
        score = score,
        insufficientReason = if (score == null) "DURATION_TOO_SHORT" else null,
        eventCounts = eventCounts,
    )

    @Test
    fun `기록이 없으면 통계를 만들지 않는다`() {
        assertNull(DrivingStats.from(emptyList(), nowMs = NOW))
    }

    @Test
    fun `점수가 없는 기록만 있으면 통계를 만들지 않는다`() {
        val records = listOf(record(startedAtMs = NOW - DAY, score = null))

        assertNull(DrivingStats.from(records, nowMs = NOW))
    }

    @Test
    fun `평균 점수를 계산한다`() {
        val records = listOf(
            record(id = "a", startedAtMs = NOW - DAY, score = 80),
            record(id = "b", startedAtMs = NOW - 2 * DAY, score = 90),
        )

        val stats = DrivingStats.from(records, nowMs = NOW)!!

        assertEquals(85, stats.averageScore)
        assertEquals(2, stats.sessionCount)
    }

    @Test
    fun `점수 없는 기록은 평균에서 제외한다`() {
        val records = listOf(
            record(id = "a", startedAtMs = NOW - DAY, score = 80),
            record(id = "b", startedAtMs = NOW - 2 * DAY, score = null),
        )

        val stats = DrivingStats.from(records, nowMs = NOW)!!

        assertEquals(80, stats.averageScore)
        assertEquals(2, stats.sessionCount)
    }

    @Test
    fun `기간 내 기록만 센다`() {
        val records = listOf(
            record(id = "recent", startedAtMs = NOW - DAY),
            record(id = "old", startedAtMs = NOW - 30 * DAY),
        )

        val stats = DrivingStats.from(records, nowMs = NOW, periodDays = 7)!!

        assertEquals(1, stats.sessionCount)
    }

    @Test
    fun `기간 내 기록이 없으면 통계를 만들지 않는다`() {
        val records = listOf(record(startedAtMs = NOW - 30 * DAY))

        assertNull(DrivingStats.from(records, nowMs = NOW, periodDays = 7))
    }

    @Test
    fun `이벤트 총 횟수를 센다`() {
        val records = listOf(
            record(id = "a", startedAtMs = NOW - DAY, eventCounts = mapOf("HARSH_BRAKE" to 3)),
            record(id = "b", startedAtMs = NOW - 2 * DAY, eventCounts = mapOf("HARSH_BRAKE" to 2)),
        )

        val stats = DrivingStats.from(records, nowMs = NOW)!!

        assertEquals(5, stats.totalEvents)
    }

    @Test
    fun `이벤트를 많은 순으로 정렬한다`() {
        val records = listOf(
            record(
                startedAtMs = NOW - DAY,
                eventCounts = mapOf("HARSH_ACCEL" to 1, "HARSH_BRAKE" to 4),
            ),
        )

        val stats = DrivingStats.from(records, nowMs = NOW)!!

        assertEquals("급제동", stats.topEvents.first().label)
        assertEquals(4, stats.topEvents.first().count)
    }

    @Test
    fun `이벤트가 없으면 빈 목록이다`() {
        val records = listOf(record(startedAtMs = NOW - DAY))

        val stats = DrivingStats.from(records, nowMs = NOW)!!

        assertTrue(stats.topEvents.isEmpty())
        assertEquals(0, stats.totalEvents)
    }

    @Test
    fun `이전 기간보다 나아졌는지 판단한다`() {
        val records = listOf(
            // 최근 7일: 평균 90
            record(id = "n1", startedAtMs = NOW - DAY, score = 90),
            record(id = "n2", startedAtMs = NOW - 2 * DAY, score = 90),
            // 이전 7일: 평균 70
            record(id = "o1", startedAtMs = NOW - 10 * DAY, score = 70),
            record(id = "o2", startedAtMs = NOW - 11 * DAY, score = 70),
        )

        val stats = DrivingStats.from(records, nowMs = NOW, periodDays = 7)!!

        assertEquals(20, stats.scoreChange)
        assertEquals(true, stats.improved)
    }

    @Test
    fun `이전 기간 기록이 없으면 변화를 알 수 없다`() {
        val records = listOf(record(startedAtMs = NOW - DAY, score = 90))

        val stats = DrivingStats.from(records, nowMs = NOW, periodDays = 7)!!

        assertNull(stats.scoreChange)
        assertNull(stats.improved)
    }

    @Test
    fun `점수가 나빠지면 개선되지 않은 것으로 본다`() {
        val records = listOf(
            record(id = "n1", startedAtMs = NOW - DAY, score = 60),
            record(id = "o1", startedAtMs = NOW - 10 * DAY, score = 90),
        )

        val stats = DrivingStats.from(records, nowMs = NOW, periodDays = 7)!!

        assertEquals(-30, stats.scoreChange)
        assertEquals(false, stats.improved)
    }

    @Test
    fun `총 주행 거리를 합산한다`() {
        val records = listOf(
            record(id = "a", startedAtMs = NOW - DAY, distanceKm = 1.5f),
            record(id = "b", startedAtMs = NOW - 2 * DAY, distanceKm = 2.5f),
        )

        val stats = DrivingStats.from(records, nowMs = NOW)!!

        assertEquals(4.0f, stats.totalDistanceKm, 0.01f)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
