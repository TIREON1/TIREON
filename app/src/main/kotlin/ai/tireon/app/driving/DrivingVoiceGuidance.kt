package ai.tireon.app.driving

import ai.tireon.app.voice.SessionResultVoice
import ai.tireon.pleos.coach.CoachSpeaker

/** 주행 중 코칭 없이 기록 시작과 정차 후 결과만 안내한다. */
class DrivingVoiceGuidance(private val speaker: CoachSpeaker) {
    private val resultVoice = SessionResultVoice(speaker)

    fun start() {
        speaker.speak("주행 기록을 시작합니다.")
    }

    fun prepareFinish(result: DrivingSessionResult): (Result<Unit>) -> Unit {
        val detail = result.scoreText?.let { "이번 주행은 ${it}점이에요." }
            ?: result.message.trim().let { if (it.endsWith('.')) it else "$it." }
        return resultVoice.prepare("주행 기록을 마쳤어요. $detail")
    }

    fun onShown() = resultVoice.onShown()

    fun onHidden(collecting: Boolean) {
        // 시작 직후 내비게이션으로 넘어가도 짧은 시작 안내는 끝까지 재생한다.
        resultVoice.onHidden(stopCurrent = !collecting)
    }

    fun close() = resultVoice.close()
}
