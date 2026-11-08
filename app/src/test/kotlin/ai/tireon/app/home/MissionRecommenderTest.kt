package ai.tireon.app.home

import ai.tireon.core.mission.PracticeMission
import ai.tireon.core.report.DrivingSessionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 최근 기록에서 연습할 미션을 고른다.
 *
 * 가장 많이 발생한 이벤트를 줄일 수 있는 미션을 추천한다. 기록이 없거나
 * 이벤트가 없으면 추천하지 않는다.
 */
class MissionRecommenderTest {

    private fun record(
        id: String = "r1",
        startedAtMs: Long = 1_000L,
        events: Map<String, Int> = emptyMap(),
    ) = DrivingSessionRecord(
        id = id,
        startedAtMs = startedAtMs,
        durationSec = 60,
        distanceKm = 1f,
        score = 85,
        insufficientReason = null,
        eventCounts = events,
    )

    @Test
    fun `기록이 없으면 추천하지 않는다`() {
        assertNull(MissionRecommender.recommend(emptyList()))
    }

    @Test
    fun `이벤트가 없으면 추천하지 않는다`() {
        assertNull(MissionRecommender.recommend(listOf(record())))
    }

    @Test
    fun `급가속이 가장 많으면 부드러운 출발을 추천한다`() {
        val records = listOf(
            record(events = mapOf("HARSH_ACCEL" to 5, "HARSH_BRAKE" to 1)),
        )

        assertEquals(PracticeMission.SMOOTH_START, MissionRecommender.recommend(records))
    }

    @Test
    fun `급제동이 가장 많으면 부드러운 정지를 추천한다`() {
        val records = listOf(
            record(events = mapOf("HARSH_BRAKE" to 4, "HARSH_ACCEL" to 1)),
        )

        assertEquals(PracticeMission.SMOOTH_STOP, MissionRecommender.recommend(records))
    }

    @Test
    fun `급조향이 가장 많으면 깜빡이 습관을 추천한다`() {
        // 급조향은 방향지시등 습관과 함께 연습하는 편이 낫다
        val records = listOf(
            record(events = mapOf("HARSH_STEER" to 3)),
        )

        assertEquals(PracticeMission.TURN_SIGNAL, MissionRecommender.recommend(records))
    }

    @Test
    fun `방향지시등 누락이 가장 많으면 깜빡이 습관을 추천한다`() {
        val records = listOf(
            record(events = mapOf("MISSING_TURN_SIGNAL" to 6)),
        )

        assertEquals(PracticeMission.TURN_SIGNAL, MissionRecommender.recommend(records))
    }

    @Test
    fun `여러 기록을 합산한다`() {
        val records = listOf(
            record(id = "a", events = mapOf("HARSH_ACCEL" to 2)),
            record(id = "b", events = mapOf("HARSH_ACCEL" to 2)),
            record(id = "c", events = mapOf("HARSH_BRAKE" to 3)),
        )

        assertEquals(PracticeMission.SMOOTH_START, MissionRecommender.recommend(records))
    }

    @Test
    fun `오래된 기록은 제외한다`() {
        // 최근 10개만 본다. 11번째로 오래된 기록의 이벤트는 무시된다.
        val old = record(
            id = "old",
            startedAtMs = 0L,
            events = mapOf("HARSH_BRAKE" to 100),
        )
        val recent = (1..10).map { index ->
            record(
                id = "recent-$index",
                startedAtMs = index * 1_000L,
                events = mapOf("HARSH_ACCEL" to 1),
            )
        }

        assertEquals(
            PracticeMission.SMOOTH_START,
            MissionRecommender.recommend(listOf(old) + recent),
        )
    }

    @Test
    fun `정렬 순서와 무관하게 최신 기록을 본다`() {
        val old = record(id = "old", startedAtMs = 1_000L, events = mapOf("HARSH_BRAKE" to 3))
        val recent = record(id = "recent", startedAtMs = 9_000L, events = mapOf("HARSH_ACCEL" to 3))

        // 오래된 것이 앞에 있어도 합산 결과는 동일하다
        assertEquals(
            MissionRecommender.recommend(listOf(old, recent)),
            MissionRecommender.recommend(listOf(recent, old)),
        )
    }

    @Test
    fun `추천 이유 문구를 만든다`() {
        val records = listOf(record(events = mapOf("HARSH_BRAKE" to 3)))

        val reason = MissionRecommender.reasonFor(records)

        assertEquals(true, reason != null && reason.contains("급제동"))
        assertEquals(true, reason != null && reason.contains("3"))
    }

    @Test
    fun `추천이 없으면 이유도 없다`() {
        assertNull(MissionRecommender.reasonFor(emptyList()))
    }
}
