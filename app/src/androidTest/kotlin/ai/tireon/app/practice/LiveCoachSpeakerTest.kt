package ai.tireon.app.practice

import ai.tireon.core.coach.LiveCoach
import ai.tireon.core.coach.SpeechPolicy
import ai.tireon.core.driving.Confidence
import ai.tireon.core.driving.DrivingEvent
import ai.tireon.core.driving.DrivingEventType
import ai.tireon.pleos.coach.CoachSpeaker
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * 실시간 코칭이 음성 재생까지 이어지는지 확인한다.
 *
 * 단위 테스트는 이벤트 선별만 본다. 여기서는 스피커 호출까지 검증한다.
 */
@RunWith(AndroidJUnit4::class)
class LiveCoachSpeakerTest {

    private class RecordingSpeaker : CoachSpeaker {
        val spoken = mutableListOf<String>()
        var stopped = false
        var released = false

        override fun speak(text: String) {
            spoken += text
        }

        override fun stop() {
            stopped = true
        }

        override fun release() {
            released = true
        }
    }

    private fun event(type: DrivingEventType, atMs: Long) = DrivingEvent(
        type = type,
        atMs = atMs,
        value = 3f,
        confidence = Confidence.MEASURED,
    )

    @Test
    fun 새_이벤트를_안내하면_스피커가_호출된다() {
        val coach = LiveCoach()
        val speaker = RecordingSpeaker()
        val events = listOf(event(DrivingEventType.HARSH_BRAKE, 1_000L))

        val fresh = coach.newEvents(events)
        val text = fresh.joinToString(" ") { coach.messageFor(it) }
        speaker.speak(text)

        assertEquals(1, speaker.spoken.size)
        assertTrue(speaker.spoken.first().contains("급제동"))
    }

    @Test
    fun 같은_이벤트는_두_번_안내하지_않는다() {
        val coach = LiveCoach()
        val speaker = RecordingSpeaker()
        val events = listOf(event(DrivingEventType.HARSH_BRAKE, 1_000L))

        val first = coach.newEvents(events)
        if (first.isNotEmpty()) {
            speaker.speak(first.joinToString(" ") { coach.messageFor(it) })
        }
        val second = coach.newEvents(events)
        if (second.isNotEmpty()) {
            speaker.speak(second.joinToString(" ") { coach.messageFor(it) })
        }

        assertEquals(1, speaker.spoken.size)
    }

    @Test
    fun 재생_정책이_연속_안내를_막는다() {
        val now = 1_000L

        val allowed = SpeechPolicy.shouldSpeak(now, 0L, "급제동이 있었어요", null)
        val blocked = SpeechPolicy.shouldSpeak(now + 1_000L, now, "급가속이 있었어요", "급제동이 있었어요")

        assertTrue(allowed)
        assertFalse(blocked)
    }

    @Test
    fun 스피커_해제_상태를_확인한다() {
        val speaker = RecordingSpeaker()

        speaker.speak("안내")
        speaker.stop()
        speaker.release()

        assertTrue(speaker.stopped)
        assertTrue(speaker.released)
    }
}
