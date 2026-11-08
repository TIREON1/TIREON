package ai.tireon.app.report

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ai.tireon.core.report.DrivingSessionRecord
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

/**
 * 실제 안드로이드 파일 시스템에서 저장이 동작하는지 검증한다.
 *
 * JVM 임시 디렉터리와 달리 앱 전용 저장소의 권한과 경로를 확인할 수 있다.
 */
@RunWith(AndroidJUnit4::class)
class SessionStorageInstrumentedTest {

    private lateinit var directory: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.filesDir, "sessions-test")
        directory.deleteRecursively()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun record(id: String, startedAtMs: Long, score: Int? = 80) = DrivingSessionRecord(
        id = id,
        startedAtMs = startedAtMs,
        durationSec = 60,
        distanceKm = 1f,
        score = score,
        insufficientReason = if (score == null) "DURATION_TOO_SHORT" else null,
        eventCounts = mapOf("HARSH_BRAKE" to 2),
    )

    @Test
    fun 앱_전용_저장소에_기록을_저장하고_읽는다() = runBlocking {
        val repository = FileSessionRepository(directory)

        val saved = repository.save(record("a", 1_000L))
        val latest = repository.latest()

        assertTrue(saved.isSuccess)
        assertEquals("a", latest?.id)
        assertEquals(80, latest?.score)
        assertEquals(2, latest?.eventCounts?.get("HARSH_BRAKE"))
    }

    @Test
    fun 여러_기록이_최신순으로_유지된다() = runBlocking {
        val repository = FileSessionRepository(directory)
        repository.save(record("old", 1_000L))
        repository.save(record("middle", 2_000L))
        repository.save(record("new", 3_000L))

        val recent = repository.recent(limit = 10)

        assertEquals(listOf("new", "middle", "old"), recent.map { it.id })
    }

    @Test
    fun 앱을_다시_띄워도_기록이_남는다() = runBlocking {
        FileSessionRepository(directory).save(record("persisted", 5_000L))

        // 새 인스턴스가 같은 디렉터리를 읽는다. 프로세스 재시작을 흉내낸다.
        val reloaded = FileSessionRepository(directory).recent(limit = 10)

        assertEquals(1, reloaded.size)
        assertEquals("persisted", reloaded.first().id)
    }

    @Test
    fun 기록이_없으면_빈_목록이다() = runBlocking {
        val repository = FileSessionRepository(directory)

        assertTrue(repository.recent(limit = 10).isEmpty())
        assertNull(repository.latest())
    }

    @Test
    fun 파일이_깨져도_앱은_빈_목록으로_시작한다() = runBlocking {
        directory.mkdirs()
        File(directory, "sessions.json").writeText("{ corrupted")

        val repository = FileSessionRepository(directory)

        assertTrue(repository.recent(limit = 10).isEmpty())
    }
}
