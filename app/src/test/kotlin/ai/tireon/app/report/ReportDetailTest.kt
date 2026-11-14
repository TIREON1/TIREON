package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 세션 기록을 상세 화면 표시용으로 바꾼다.
 *
 * 저장된 값과 표시 문구를 분리한다. 화면은 이 모델만 그리면 된다.
 */
class ReportDetailTest {

    private fun record(
        score: Int? = 85,
        insufficientReason: String? = null,
        eventCounts: Map<String, Int> = emptyMap(),
        mission: String? = null,
        missionSuccess: Boolean? = null,
        missionDetail: String? = null,
        missionInsufficient: Boolean = false,
        id: String = "r1",
        distanceKm: Float? = 1.5f,
        durationSec: Int = 120,
    ) = DrivingSessionRecord(
        id = id,
        startedAtMs = 1_700_000_000_000L,
        durationSec = durationSec,
        distanceKm = distanceKm,
        score = score,
        insufficientReason = insufficientReason,
        eventCounts = eventCounts,
        mission = mission,
        missionSuccess = missionSuccess,
        missionDetail = missionDetail,
        missionInsufficient = missionInsufficient,
    )

    @Test
    fun `점수를 표시한다`() {
        val detail = ReportDetail.from(record(score = 92))

        assertEquals("92", detail.scoreText)
        assertTrue(detail.hasScore)
    }

    @Test
    fun `점수가 없으면 사유를 표시한다`() {
        val detail = ReportDetail.from(
            record(score = null, insufficientReason = "DURATION_TOO_SHORT"),
        )

        assertNull(detail.scoreText)
        assertEquals(false, detail.hasScore)
        assertTrue(detail.scoreReason!!.contains("1분"))
    }

    @Test
    fun `알 수 없는 사유는 일반 문구로 표시한다`() {
        val detail = ReportDetail.from(
            record(score = null, insufficientReason = "SOMETHING_NEW"),
        )

        assertTrue(detail.scoreReason!!.contains("부족"))
    }

    @Test
    fun `주행 시간을 분과 초로 표시한다`() {
        val detail = ReportDetail.from(record(durationSec = 125))

        assertEquals("2분 5초", detail.durationText)
    }

    @Test
    fun `1분 미만은 초만 표시한다`() {
        val detail = ReportDetail.from(record(durationSec = 45))

        assertEquals("45초", detail.durationText)
    }

    @Test
    fun `거리를 표시한다`() {
        val detail = ReportDetail.from(record(distanceKm = 1.54f))

        assertEquals("1.5km", detail.distanceText)
    }

    @Test
    fun `거리가 없으면 안내 문구를 쓴다`() {
        val detail = ReportDetail.from(record(distanceKm = null))

        assertEquals("사용할 수 없음", detail.distanceText)
    }

    @Test
    fun `이벤트를 많은 순으로 정렬해 보여준다`() {
        val detail = ReportDetail.from(
            record(
                eventCounts = mapOf(
                    "HARSH_ACCEL" to 1,
                    "HARSH_BRAKE" to 5,
                    "HARSH_STEER" to 3,
                ),
            ),
        )

        assertEquals(listOf("급제동", "급조향", "급가속"), detail.events.map { it.label })
        assertEquals(listOf(5, 3, 1), detail.events.map { it.count })
    }

    @Test
    fun `이벤트가 없으면 빈 목록이다`() {
        val detail = ReportDetail.from(record())

        assertTrue(detail.events.isEmpty())
    }

    @Test
    fun `알 수 없는 이벤트는 제외한다`() {
        val detail = ReportDetail.from(
            record(eventCounts = mapOf("HARSH_BRAKE" to 2, "UNKNOWN_TYPE" to 9)),
        )

        assertEquals(1, detail.events.size)
        assertEquals("급제동", detail.events.first().label)
    }

    @Test
    fun `미션 달성 여부를 표시한다`() {
        val detail = ReportDetail.from(
            record(mission = "SMOOTH_START", missionSuccess = true, missionDetail = "잘했어요"),
        )

        assertEquals("목표 달성", detail.missionLabel)
        assertEquals("잘했어요", detail.missionDetail)
    }

    @Test
    fun `미션 실패를 표시한다`() {
        val detail = ReportDetail.from(
            record(mission = "SMOOTH_STOP", missionSuccess = false, missionDetail = "급제동 2회"),
        )

        assertEquals("다시 해볼까요", detail.missionLabel)
    }

    @Test
    fun `미션이 없으면 라벨도 없다`() {
        val detail = ReportDetail.from(record())

        assertNull(detail.missionLabel)
    }

    @Test
    fun `연습 미션 판정 보류는 보류로 표시한다`() {
        val detail = ReportDetail.from(
            record(
                mission = "STEADY_SPEED",
                missionSuccess = null,
                missionDetail = "신호 부족",
                missionInsufficient = true,
            ),
        )

        assertEquals("판정 보류", detail.missionLabel)
        assertEquals("신호 부족", detail.missionDetail)
    }

    @Test
    fun `보류 여부가 없는 이전 연습 기록도 보류로 표시한다`() {
        val detail = ReportDetail.from(
            record(mission = "STEADY_SPEED", missionSuccess = null, missionDetail = "신호 부족"),
        )

        assertEquals("판정 보류", detail.missionLabel)
    }

    @Test
    fun `주행 미션 요약을 표시한다`() {
        val detail = ReportDetail.from(
            record(
                id = "driving-1",
                mission = "SMOOTH_DRIVING",
                missionDetail = "급가속 1회, 급제동 0회",
            ),
        )

        assertEquals("주행 결과", detail.missionLabel)
        assertEquals("급가속 1회, 급제동 0회", detail.missionDetail)
        assertNull(detail.missionSuccess)
    }

    @Test
    fun `주행 미션 판정 보류는 요약과 구분해 표시한다`() {
        val detail = ReportDetail.from(
            record(
                id = "driving-1",
                mission = "SMOOTH_DRIVING",
                missionDetail = "브레이크 신호를 받지 못했어요",
                missionInsufficient = true,
            ),
        )

        assertEquals("판정 보류", detail.missionLabel)
        assertEquals("브레이크 신호를 받지 못했어요", detail.missionDetail)
    }

    @Test
    fun `주행 미션 결과가 없으면 라벨도 없다`() {
        val detail = ReportDetail.from(record(id = "driving-1", mission = "SMOOTH_DRIVING"))

        assertNull(detail.missionLabel)
    }
}
