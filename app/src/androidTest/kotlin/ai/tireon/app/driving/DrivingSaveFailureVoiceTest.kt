package ai.tireon.app.driving

import ai.tireon.app.report.FileSessionRepository
import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.pleos.coach.PleosCoachSpeaker
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 캐시 안의 잘못된 저장 경로로 실제 저장 실패를 만들고 Pleos TTS로 안내한다.
 *
 * 자동 검증은 저장 실패까지 확인한다. 실제 발화는 이 테스트 실행 중 녹음해 별도로
 * 검수한다. 사용자 기록 저장소나 앱 설정은 변경하지 않는다.
 */
@RunWith(AndroidJUnit4::class)
class DrivingSaveFailureVoiceTest {

    @Test
    fun actualSaveFailureAnnouncesAfterDrivingResult() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // 디렉터리 자리에 일반 파일을 주어 sessions.json.tmp 생성을 확실히 실패시킨다.
        val blockingFile = File.createTempFile("driving-save-failure-", ".tmp", context.cacheDir)
        val speaker = withContext(Dispatchers.Main) { PleosCoachSpeaker(context) }
        val guidance = withContext(Dispatchers.Main) { DrivingVoiceGuidance(speaker) }

        try {
            val onSaved = withContext(Dispatchers.Main) {
                guidance.prepareFinish(
                    DrivingSessionResult(
                        scoreText = null,
                        message = "차량 신호를 충분히 받지 못했어요",
                        mission = null,
                        missionDetail = null,
                        eventCount = 0,
                    ),
                )
            }
            val result = FileSessionRepository(blockingFile).save(
                DrivingSessionRecord(
                    id = "save-failure-voice-fixture",
                    startedAtMs = 0L,
                    durationSec = 0,
                    distanceKm = null,
                    score = null,
                    insufficientReason = "NOT_ENOUGH_SAMPLES",
                    eventCounts = emptyMap(),
                ),
            )

            assertTrue("일반 파일 아래에는 기록 파일을 저장할 수 없어야 한다", result.isFailure)
            assertTrue("실패 fixture가 일반 파일로 유지되어야 한다", blockingFile.isFile)
            withContext(Dispatchers.Main) { onSaved(result) }

            // 실제 SDK 음성을 녹음하는 smoke fixture 전용 대기다. 제품 코드에는 지연을 넣지 않는다.
            delay(10_000L)
        } finally {
            withContext(NonCancellable + Dispatchers.Main) {
                guidance.close()
                speaker.release()
            }
            assertTrue("이 테스트가 만든 캐시 fixture를 정리해야 한다", blockingFile.delete())
        }
    }
}
