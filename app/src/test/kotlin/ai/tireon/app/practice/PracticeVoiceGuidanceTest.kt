package ai.tireon.app.practice

import ai.tireon.core.DrivingScore
import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.analysis.ScoreResult
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.core.mission.MissionResult
import ai.tireon.core.mission.PracticeMission
import ai.tireon.pleos.coach.CoachSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeVoiceGuidanceTest {
    private class RecordingSpeaker : CoachSpeaker {
        val spoken = mutableListOf<String>()
        var stopped = false
        override fun speak(text: String) { spoken += text }
        override fun stop() { stopped = true }
        override fun release() = Unit
    }

    private val speaker = RecordingSpeaker()
    private var now = 10_000L
    private val guidance = PracticeVoiceGuidance(speaker) { now }

    @Test
    fun `미션 여섯 종류를 서로 다른 짧은 시작 안내로 확인한다`() {
        PracticeMission.entries.forEach(guidance::start)

        assertEquals(6, speaker.spoken.distinct().size)
        speaker.spoken.forEach { text ->
            assertTrue(text.contains("연습을 시작합니다."))
            assertEquals(2, text.count { it == '.' })
            assertTrue(text.length <= 70)
        }
        assertTrue(speaker.spoken.any { it.startsWith("우회전 일시정지 연습") })
        assertTrue(speaker.spoken.any { it.startsWith("천천히 후진 연습") })
    }

    @Test
    fun `시작 안내 직후에도 첫 급제동을 안내한다`() {
        guidance.start(PracticeMission.SMOOTH_STOP)
        now += 1_000
        guidance.announceEvent("급제동이 있었어요.")

        assertEquals(2, speaker.spoken.size)
        assertEquals("급제동이 있었어요.", speaker.spoken.last())
    }

    @Test
    fun `실시간 이벤트는 여전히 5초 간격과 같은 문장 중복을 제한한다`() {
        guidance.announceEvent("급제동이 있었어요.")
        now += 1_000
        guidance.announceEvent("급가속이 있었어요.")
        now += 4_000
        guidance.announceEvent("급제동이 있었어요.")
        guidance.announceEvent("급가속이 있었어요.")

        assertEquals(listOf("급제동이 있었어요.", "급가속이 있었어요."), speaker.spoken)
    }

    @Test
    fun `빈 이벤트는 이후 안내 간격을 소비하지 않는다`() {
        guidance.announceEvent("   ")
        now += 100
        guidance.announceEvent("급제동이 있었어요.")

        assertEquals(listOf("급제동이 있었어요."), speaker.spoken)
    }

    @Test
    fun `이벤트 직후 종료해도 결과 안내를 생략하지 않는다`() {
        guidance.announceEvent("급제동이 있었어요.")
        now += 100
        guidance.finish(summary(MissionResult.Failed("급제동이 있었어요 (1번)")))

        assertEquals(2, speaker.spoken.size)
        assertTrue(speaker.spoken.last().startsWith("연습을 마쳤어요. 목표를 아직 달성하지 못했어요."))
        assertTrue(speaker.spoken.last().contains("오늘은 92점이에요."))
        assertFalse(speaker.spoken.last().contains("저장"))
    }

    @Test
    fun `빠르게 마친 성공 미션은 목표 달성을 읽고 점수는 만들어내지 않는다`() {
        guidance.start(PracticeMission.RIGHT_TURN_STOP)
        now += 100
        guidance.finish(
            PracticeSummary.from(
                ScoreResult.Insufficient(InsufficientReason.DURATION_TOO_SHORT),
                emptyList(),
                MissionResult.Success("멈춘 뒤에 우회전했어요"),
            ),
        )

        val text = speaker.spoken.last()
        assertTrue(text.contains("목표를 달성했어요."))
        assertTrue(text.contains("1분 이상 연습하면 점수를 낼 수 있어요."))
        assertFalse(text.contains("오늘은"))
    }

    @Test
    fun `판정 불가를 실패나 성공으로 바꾸지 않고 실제 사유를 안내한다`() {
        guidance.finish(
            PracticeSummary.from(
                ScoreResult.Insufficient(InsufficientReason.NOT_ENOUGH_SAMPLES),
                emptyList(),
                MissionResult.Insufficient("방향지시등 신호를 받지 못했어요"),
            ),
        )

        val text = speaker.spoken.single()
        assertTrue(text.contains("이번 미션은 아직 판정할 수 없어요."))
        assertTrue(text.contains("방향지시등 신호를 받지 못했어요."))
        assertFalse(text.contains("목표를 달성"))
        assertFalse(text.contains("달성하지 못"))
        assertFalse(text.contains("점이에요"))
    }

    @Test
    fun `여러 이벤트를 한꺼번에 나열하지 않고 피드백 하나와 점수만 읽는다`() {
        val events = DrivingEventType.entries.map { type ->
            DrivingEvent(type, 0L, 3f, Confidence.MEASURED)
        }
        guidance.finish(summary(MissionResult.Failed("급제동이 있었어요 (1번)"), events))

        val text = speaker.spoken.single()
        assertTrue(text.contains("급제동이 있었어요 (1번)."))
        assertFalse(text.contains("급제동이 1번 있었어요."))
        assertFalse(text.contains("급가속"))
        assertFalse(text.contains("방향지시등"))
        assertFalse(text.contains("핸들"))
        assertEquals(1, Regex("92점").findAll(text).count())
    }

    @Test
    fun `다른 이벤트가 있어도 선택한 미션의 실패 원인을 먼저 읽는다`() {
        guidance.finish(
            summary(
                MissionResult.Failed("우회전 전에 2초 이상 완전히 멈춰 보세요"),
                listOf(DrivingEvent(DrivingEventType.HARSH_BRAKE, 0L, 3f, Confidence.MEASURED)),
            ),
        )

        val text = speaker.spoken.single()
        assertTrue(text.contains("우회전 전에 2초 이상 완전히 멈춰 보세요."))
        assertFalse(text.contains("급제동"))
    }

    @Test
    fun `화면을 떠날 때 현재 음성을 중단한다`() {
        guidance.start(PracticeMission.SMOOTH_START)
        guidance.stop()

        assertTrue(speaker.stopped)
    }

    private fun summary(
        missionResult: MissionResult,
        events: List<DrivingEvent> = emptyList(),
    ) = PracticeSummary.from(
        ScoreResult.Scored(DrivingScore(92), emptyList()),
        events,
        missionResult,
    )
}
