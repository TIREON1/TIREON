package ai.tireon.core.coach

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechPolicyTest {

    @Test
    fun `처음 문장은 재생한다`() {
        assertTrue(SpeechPolicy.shouldSpeak(1_000L, 0L, "출발 좋아요", null))
    }

    @Test
    fun `최소 간격이 지나지 않으면 재생하지 않는다`() {
        assertFalse(
            SpeechPolicy.shouldSpeak(3_000L, 1_000L, "급제동이 있었어요", "출발 좋아요"),
        )
    }

    @Test
    fun `최소 간격이 지나면 재생한다`() {
        assertTrue(
            SpeechPolicy.shouldSpeak(6_000L, 1_000L, "급제동이 있었어요", "출발 좋아요"),
        )
    }

    @Test
    fun `같은 문장은 간격이 지나도 다시 읽지 않는다`() {
        assertFalse(
            SpeechPolicy.shouldSpeak(10_000L, 1_000L, "출발 좋아요", "출발 좋아요"),
        )
    }

    @Test
    fun `빈 문장은 재생하지 않는다`() {
        assertFalse(SpeechPolicy.shouldSpeak(10_000L, 0L, "   ", null))
    }
}
