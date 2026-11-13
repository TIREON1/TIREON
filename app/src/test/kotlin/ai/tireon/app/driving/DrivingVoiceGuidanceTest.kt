package ai.tireon.app.driving

import ai.tireon.pleos.coach.CoachSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DrivingVoiceGuidanceTest {
    private class RecordingSpeaker : CoachSpeaker {
        val spoken = mutableListOf<String>()
        var stopped = false
        override fun speak(text: String) { spoken += text }
        override fun stop() { stopped = true }
        override fun release() = Unit
    }

    private val speaker = RecordingSpeaker()
    private val guidance = DrivingVoiceGuidance(speaker)

    @Test
    fun `내비로 전환되어도 짧은 시작 안내를 중단하지 않는다`() {
        guidance.start()
        guidance.onHidden(collecting = true)

        assertEquals(listOf("주행 기록을 시작합니다."), speaker.spoken)
        assertFalse(speaker.stopped)
    }

    @Test
    fun `정차 후 돌아와 종료하면 실제 점수를 읽는다`() {
        guidance.start()
        guidance.onHidden(collecting = true)
        guidance.onShown()
        guidance.prepareFinish(result("88"))(Result.success(Unit))

        assertEquals("주행 기록을 마쳤어요. 이번 주행은 88점이에요.", speaker.spoken.last())
        assertEquals(2, speaker.spoken.size)
    }

    @Test
    fun `점수 부족일 때 점수를 만들지 않고 실제 이유를 읽는다`() {
        guidance.prepareFinish(result(null))(Result.success(Unit))

        val text = speaker.spoken.single()
        assertEquals("주행 기록을 마쳤어요. 차량 신호를 충분히 받지 못했어요.", text)
        assertFalse(text.contains("점이에요"))
    }

    @Test
    fun `저장이 실패하면 결과와 실패 안내를 합쳐 한 번 읽는다`() {
        val onSaved = guidance.prepareFinish(result("88"))
        assertTrue(speaker.spoken.isEmpty())
        onSaved(Result.failure(IllegalStateException("disk full")))

        assertEquals(
            "주행 기록을 마쳤어요. 이번 주행은 88점이에요. 기록을 저장하지 못했어요.",
            speaker.spoken.single(),
        )
    }

    @Test
    fun `결과를 닫으면 현재 재생과 늦은 저장 실패 안내를 모두 중단한다`() {
        val onSaved = guidance.prepareFinish(result("88"))
        guidance.onHidden(collecting = false)
        onSaved(Result.failure(IllegalStateException("disk full")))

        assertTrue(speaker.stopped)
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun `화면 재진입은 시작이나 결과를 다시 읽지 않는다`() {
        guidance.start()
        guidance.onHidden(collecting = true)
        guidance.onShown()
        guidance.onShown()

        assertEquals(listOf("주행 기록을 시작합니다."), speaker.spoken)
    }

    private fun result(score: String?) = DrivingSessionResult(
        scoreText = score,
        message = if (score != null) "주행이 끝났어요" else "차량 신호를 충분히 받지 못했어요",
        mission = null,
        missionDetail = null,
        eventCount = 0,
    )
}
