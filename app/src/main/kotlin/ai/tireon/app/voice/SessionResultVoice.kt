package ai.tireon.app.voice

import ai.tireon.pleos.coach.CoachSpeaker

/** 저장 결과를 확인한 뒤 결과와 실패 안내를 하나의 발화로 전달한다. */
class SessionResultVoice(private val speaker: CoachSpeaker) {
    private var visible = true
    private var visibilityVersion = 0L
    private var closed = false

    fun prepare(text: String): (Result<Unit>) -> Unit {
        val version = visibilityVersion
        val requestedWhileVisible = visible && !closed
        var completed = false
        return { saveResult ->
            if (!completed) {
                completed = true
                // 저장 중 화면을 떠났다면 돌아온 뒤에도 이전 결과를 뒤늦게 읽지 않는다.
                if (requestedWhileVisible && visible && !closed && version == visibilityVersion) {
                    val failure = if (saveResult.isFailure) " 기록을 저장하지 못했어요." else ""
                    speaker.speak(text + failure)
                }
            }
        }
    }

    fun onShown() {
        visible = true
    }

    fun onHidden(stopCurrent: Boolean = true) {
        visible = false
        visibilityVersion++
        if (stopCurrent) speaker.stop()
    }

    fun close() {
        closed = true
        onHidden()
    }
}
