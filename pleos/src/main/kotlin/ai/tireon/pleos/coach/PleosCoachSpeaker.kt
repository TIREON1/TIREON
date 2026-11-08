package ai.tireon.pleos.coach

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import android.content.Context
import android.util.Log
import java.util.Locale

/**
 * Pleos TTS SDK로 코칭 문장을 읽는다.
 *
 * `Mode.ON_DEVICE`를 쓰므로 네트워크와 앱 등록이 필요 없다. 초기화나 재생에
 * 실패해도 예외를 던지지 않고 로그만 남긴다. 음성은 보조 수단이기 때문이다.
 */
class PleosCoachSpeaker(context: Context) : CoachSpeaker {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var released = false

    /** 사용 직전에 한 번 초기화한다. 실패하면 이후 호출은 조용히 무시된다. */
    private fun ensureInitialized(): TextToSpeech? {
        if (released) return null

        tts?.let { if (initialized) return it }

        return try {
            val engine = TextToSpeech(appContext, Mode.ON_DEVICE)
            engine.initialize()
            engine.setLocale(Locale.KOREA)
            tts = engine
            initialized = true
            engine
        } catch (e: Exception) {
            Log.e(TAG, "TTS 초기화 실패", e)
            initialized = false
            null
        }
    }

    override fun speak(text: String) {
        if (text.isBlank()) return
        val engine = ensureInitialized() ?: return

        try {
            engine.speak(text, true)
        } catch (e: Exception) {
            Log.e(TAG, "TTS 재생 실패", e)
        }
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "TTS 정지 실패", e)
        }
    }

    override fun release() {
        if (released) return
        released = true
        try {
            tts?.stop()
            tts?.release()
        } catch (e: Exception) {
            Log.e(TAG, "TTS 해제 실패", e)
        } finally {
            tts = null
            initialized = false
        }
    }

    private companion object {
        const val TAG = "PleosCoachSpeaker"
    }
}
