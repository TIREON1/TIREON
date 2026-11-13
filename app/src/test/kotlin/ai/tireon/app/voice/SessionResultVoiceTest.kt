package ai.tireon.app.voice

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import ai.tireon.pleos.coach.CoachSpeaker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SessionResultVoiceTest {
    private class RecordingSpeaker : CoachSpeaker {
        val spoken = mutableListOf<String>()
        var stopped = false
        override fun speak(text: String) { spoken += text }
        override fun stop() { stopped = true }
        override fun release() = Unit
    }

    private class DeferredRepository : SessionRepository {
        val saved = CompletableDeferred<Result<Unit>>()
        override suspend fun save(record: DrivingSessionRecord): Result<Unit> = saved.await()
        override suspend fun recent(limit: Int) = emptyList<DrivingSessionRecord>()
        override suspend fun latest(): DrivingSessionRecord? = null
    }

    private val speaker = RecordingSpeaker()
    private val voice = SessionResultVoice(speaker)

    @Test
    fun `저장 실패가 확정되기 전에는 실패를 말하지 않고 확정 후 한 번 읽는다`() = runTest {
        val repository = DeferredRepository()
        val onSaved = voice.prepare("주행 기록을 마쳤어요.")
        launch { onSaved(repository.save(record())) }
        runCurrent()
        assertTrue(speaker.spoken.isEmpty())

        repository.saved.complete(Result.failure(IllegalStateException("disk full")))
        runCurrent()
        assertEquals(listOf("주행 기록을 마쳤어요. 기록을 저장하지 못했어요."), speaker.spoken)
    }

    @Test
    fun `저장 성공은 실패 문구 없이 결과만 읽는다`() {
        voice.prepare("연습을 마쳤어요.")(Result.success(Unit))

        assertEquals(listOf("연습을 마쳤어요."), speaker.spoken)
    }

    @Test
    fun `중복 완료 콜백에도 같은 결과를 한 번만 읽는다`() {
        val onSaved = voice.prepare("연습을 마쳤어요.")
        onSaved(Result.success(Unit))
        onSaved(Result.failure(IllegalStateException("late callback")))

        assertEquals(listOf("연습을 마쳤어요."), speaker.spoken)
    }

    @Test
    fun `화면을 떠났다가 돌아와도 이전에 대기하던 저장 결과를 읽지 않는다`() = runTest {
        val repository = DeferredRepository()
        val onSaved = voice.prepare("주행 기록을 마쳤어요.")
        launch { onSaved(repository.save(record())) }
        runCurrent()

        voice.onHidden()
        voice.onShown()
        repository.saved.complete(Result.failure(IllegalStateException("disk full")))
        runCurrent()

        assertTrue(speaker.stopped)
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun `숨겨진 화면에서 준비한 결과도 돌아온 뒤 뒤늦게 읽지 않는다`() {
        voice.onHidden()
        val onSaved = voice.prepare("연습을 마쳤어요.")
        voice.onShown()
        onSaved(Result.success(Unit))

        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun `돌아온 화면에서 새로 준비한 결과는 읽는다`() {
        voice.onHidden()
        voice.onShown()
        voice.prepare("연습을 마쳤어요.")(Result.success(Unit))

        assertEquals(listOf("연습을 마쳤어요."), speaker.spoken)
    }

    @Test
    fun `ViewModel 해제 후에는 화면 표시 호출과 저장 완료가 와도 읽지 않는다`() {
        val onSaved = voice.prepare("연습을 마쳤어요.")
        voice.close()
        voice.onShown()
        onSaved(Result.success(Unit))
        voice.prepare("주행 기록을 마쳤어요.")(Result.success(Unit))

        assertTrue(speaker.stopped)
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun `내비 전환은 현재 안내를 중단하지 않아도 지연 결과는 무효화한다`() {
        val onSaved = voice.prepare("주행 기록을 마쳤어요.")
        voice.onHidden(stopCurrent = false)
        onSaved(Result.success(Unit))

        assertFalse(speaker.stopped)
        assertTrue(speaker.spoken.isEmpty())
    }

    @Test
    fun `저장 코루틴 취소를 저장 실패 안내로 바꾸지 않는다`() = runTest {
        val repository = DeferredRepository()
        val onSaved = voice.prepare("연습을 마쳤어요.")
        val job = launch { onSaved(repository.save(record())) }
        runCurrent()
        job.cancel()
        runCurrent()

        assertTrue(speaker.spoken.isEmpty())
    }

    private fun record() = DrivingSessionRecord(
        id = "voice-test", startedAtMs = 0L, durationSec = 60,
        distanceKm = 0.5f, score = 88, insufficientReason = null, eventCounts = emptyMap(),
    )
}
