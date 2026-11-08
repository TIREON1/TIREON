package ai.tireon.pleos.coach

/**
 * 코칭 문장을 음성으로 읽어준다.
 *
 * 음성은 보조 수단이다. 재생에 실패해도 예외를 던지지 않아야 하며, 화면이
 * 주 경로로 남아야 한다.
 */
interface CoachSpeaker {
    fun speak(text: String)

    /** 재생을 즉시 멈춘다. 백그라운드 진입과 화면 전환에서 호출한다. */
    fun stop()

    /** 엔진을 해제한다. 호출 후에는 [speak]가 동작하지 않는다. */
    fun release()
}
