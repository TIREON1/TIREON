package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileSessionRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun record(id: String, startedAtMs: Long, score: Int? = 80) = DrivingSessionRecord(
        id = id,
        startedAtMs = startedAtMs,
        durationSec = 60,
        distanceKm = 1f,
        score = score,
        insufficientReason = if (score == null) "DURATION_TOO_SHORT" else null,
        eventCounts = emptyMap(),
    )

    @Test
    fun `저장한 기록을 다시 읽는다`() = runTest {
        val repository = FileSessionRepository(folder.root)

        val saved = repository.save(record("a", 1_000L))
        val latest = repository.latest()

        assertTrue(saved.isSuccess)
        assertEquals("a", latest?.id)
    }

    @Test
    fun `최근 기록은 최신 순으로 정렬된다`() = runTest {
        val repository = FileSessionRepository(folder.root)
        repository.save(record("old", 1_000L))
        repository.save(record("new", 5_000L))

        val recent = repository.recent(limit = 10)

        assertEquals("new", recent.first().id)
        assertEquals("old", recent.last().id)
    }

    @Test
    fun `기록이 없으면 빈 목록이다`() = runTest {
        val repository = FileSessionRepository(folder.root)

        assertTrue(repository.recent(10).isEmpty())
        assertNull(repository.latest())
    }

    @Test
    fun `깨진 파일은 무시하고 빈 목록을 준다`() = runTest {
        val file = folder.newFile("sessions.json")
        file.writeText("{ broken json")

        val repository = FileSessionRepository(folder.root)

        assertTrue(repository.recent(10).isEmpty())
    }

    @Test
    fun `동시에 저장해도 기록이 유실되지 않는다`() = runTest {
        val repository = FileSessionRepository(folder.root)

        coroutineScope {
            (0 until 20).map { index ->
                async(Dispatchers.IO) {
                    repository.save(record("c$index", index * 1_000L))
                }
            }.awaitAll()
        }

        val recent = repository.recent(limit = 30)

        assertEquals(20, recent.size)
    }

    @Test
    fun `저장 개수 제한을 넘으면 오래된 기록을 버린다`() = runTest {
        val repository = FileSessionRepository(folder.root, maxRecords = 3)
        repeat(5) { index -> repository.save(record("s$index", index * 1_000L)) }

        val recent = repository.recent(limit = 10)

        assertEquals(3, recent.size)
        assertEquals("s4", recent.first().id)
    }

    @Test
    fun `새 저장소로 다시 열어도 주행 판정과 기존 기록을 함께 보존한다`() = runTest {
        val repository = FileSessionRepository(folder.root)
        assertTrue(repository.save(record("old", 1000L).copy(schemaVersion = 2)).isSuccess)
        val scored = record("driving-scored", 2000L).copy(
            mission = "SMOOTH_DRIVING", missionDetail = "급가속 1회, 급제동 0회", missionEvaluated = true,
        )
        val insufficient = record("driving-insufficient", 3000L).copy(
            mission = "SMOOTH_DRIVING", missionDetail = "브레이크 신호를 받지 못했어요", missionEvaluated = false,
        )
        assertTrue(repository.save(scored).isSuccess)
        assertTrue(repository.save(insufficient).isSuccess)
        val reopened = FileSessionRepository(folder.root).recent(10)
        assertEquals(listOf(insufficient, scored, record("old", 1000L).copy(schemaVersion = 2)), reopened)
        assertEquals("판정 보류", ReportDetail.from(reopened.first()).missionLabel)
    }

    @Test
    fun `깨진 원본은 새 기록 저장 시 덮어쓰지 않고 실패를 알린다`() = runTest {
        val file = folder.newFile("sessions.json")
        val damaged = "{ broken but recoverable original"
        file.writeText(damaged)
        val repository = FileSessionRepository(folder.root)
        assertTrue(repository.recent(10).isEmpty())
        assertTrue(repository.save(record("new", 2000L)).isFailure)
        assertEquals(damaged, file.readText())
    }
}
