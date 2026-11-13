package ai.tireon.core.coach

/**
 * 음성 재생 여부를 정한다.
 *
 * 차 안에서 문장이 연속으로 나오면 말이 겹치고 운전을 방해한다. 최소 간격을
 * 두고, 같은 문장은 [REPEAT_INTERVAL_MS] 안에서는 반복하지 않는다.
 */
object SpeechPolicy {

    /** 두 문장 사이 최소 간격. */
    const val MIN_INTERVAL_MS = 5_000L

    /**
     * 같은 문장을 다시 읽기까지의 간격.
     *
     * 같은 실수를 연달아 들으면 잔소리가 되지만, 한참 뒤에 같은 실수를 했는데
     * 아무 말이 없으면 코칭이 멈춘 것처럼 느껴진다.
     */
    const val REPEAT_INTERVAL_MS = 30_000L

    /**
     * @param nowMs 현재 시각
     * @param lastSpokenAtMs 마지막으로 재생한 시각. 재생 이력이 없으면 0
     * @param text 읽을 문장
     * @param lastText 마지막으로 읽은 문장. 없으면 null
     */
    fun shouldSpeak(
        nowMs: Long,
        lastSpokenAtMs: Long,
        text: String,
        lastText: String?,
    ): Boolean {
        if (text.isBlank()) return false
        // 재생 이력이 없으면 간격을 따지지 않는다.
        if (lastSpokenAtMs <= 0L) return true

        val elapsed = nowMs - lastSpokenAtMs
        if (text == lastText) return elapsed >= REPEAT_INTERVAL_MS
        return elapsed >= MIN_INTERVAL_MS
    }
}
