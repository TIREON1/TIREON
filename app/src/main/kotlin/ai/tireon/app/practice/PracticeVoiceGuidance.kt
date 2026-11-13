package ai.tireon.app.practice

import ai.tireon.core.coach.SpeechPolicy
import ai.tireon.core.mission.PracticeMission
import ai.tireon.pleos.coach.CoachSpeaker
import ai.tireon.app.voice.SessionResultVoice

/** 시작·종료 확인은 실시간 코칭의 재생 간격과 별개로 한 번씩 안내한다. */
class PracticeVoiceGuidance(
    private val speaker: CoachSpeaker,
    private val nowMs: () -> Long,
) {
    private var lastEventAtMs = 0L
    private var lastEventText: String? = null
    private val resultVoice = SessionResultVoice(speaker)

    fun start(mission: PracticeMission) {
        // 시작 안내가 첫 급가속·급제동 안내를 막지 않도록 이벤트 시각은 갱신하지 않는다.
        speaker.speak(startMessage(mission))
    }

    fun announceEvent(text: String) {
        val now = nowMs()
        if (!SpeechPolicy.shouldSpeak(now, lastEventAtMs, text, lastEventText)) return
        lastEventAtMs = now
        lastEventText = text
        speaker.speak(text)
    }

    fun prepareFinish(summary: PracticeSummary): (Result<Unit>) -> Unit {
        // 정차 후 종료가 확정된 안내다. 직전 이벤트 때문에 결과가 생략되면 안 된다.
        return resultVoice.prepare(finishMessage(summary))
    }

    fun onShown() = resultVoice.onShown()

    fun stop() = resultVoice.onHidden()

    fun close() = resultVoice.close()

    private fun startMessage(mission: PracticeMission): String = when (mission) {
        PracticeMission.SMOOTH_START ->
            "부드러운 출발 연습을 시작합니다. 주변을 확인하고 천천히 출발해 주세요."
        PracticeMission.SMOOTH_STOP ->
            "부드러운 정지 연습을 시작합니다. 천천히 감속해 멈춰 보세요."
        PracticeMission.REVERSE_SENSE ->
            "천천히 후진 연습을 시작합니다. 뒤쪽을 확인하고 천천히 움직여 주세요."
        PracticeMission.TURN_SIGNAL ->
            "깜빡이 습관 연습을 시작합니다. 방향지시등을 켠 뒤 회전해 주세요."
        PracticeMission.STEADY_SPEED ->
            "정속 유지 연습을 시작합니다. 일정한 속도를 부드럽게 유지해 보세요."
        PracticeMission.RIGHT_TURN_STOP ->
            "우회전 일시정지 연습을 시작합니다. 오른쪽 깜빡이를 켜고 멈춘 뒤 회전해 주세요."
    }

    private fun finishMessage(summary: PracticeSummary): String {
        val messages = mutableListOf("연습을 마쳤어요.")
        messages += when (summary.missionSuccess) {
            true -> "목표를 달성했어요."
            false -> "목표를 아직 달성하지 못했어요."
            null -> "이번 미션은 아직 판정할 수 없어요."
        }

        summary.scoreText?.let { messages += "오늘은 ${it}점이에요." }

        // 점수와 미션 판정은 서로 다르다. 실패·판정 불가 사유 또는 코칭 하나만 읽는다.
        val feedback = when {
            summary.missionSuccess == null -> summary.missionDetail ?: summary.message
            summary.missionSuccess == false ->
                summary.missionDetail
                    ?: summary.coachMessages.firstOrNull { it.evidence != null }?.text
                    ?: summary.message
            summary.scoreText == null -> summary.message
            else -> summary.coachMessages.firstOrNull { it.evidence != null }?.text
        }
        feedback?.trim()?.takeIf { it.isNotEmpty() }?.let {
            messages += if (it.endsWith('.') || it.endsWith('!') || it.endsWith('?')) it else "$it."
        }
        return messages.joinToString(" ")
    }
}
