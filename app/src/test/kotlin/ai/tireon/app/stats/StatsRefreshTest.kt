package ai.tireon.app.stats

import ai.tireon.app.report.MainDispatcherRule
import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 통계 갱신 동작.
 *
 * ViewModel은 화면 전환에도 살아있다. 생성자에서 한 번만 읽으면 주행을 마치고
 * 돌아와도 통계가 그대로다. 화면에 다시 들어올 때 읽어야 한다.
 */
class StatsRefreshTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeRepository(
        private var records: List<DrivingSessionRecord> = emptyList(),
    ) : SessionRepository {
        var readCount = 0
            private set

        fun setRecords(newRecords: List<DrivingSessionRecord>) {
            records = newRecords
        }

        override suspend fun save(record: DrivingSessionRecord): Result<Unit> = Result.success(Unit)

        override suspend fun recent(limit: Int): List<DrivingSessionRecord> {
            readCount++
            return records.take(limit)
        }

        override suspend fun latest(): DrivingSessionRecord? = records.firstOrNull()
    }

    private fun record(id: String, score: Int? = 85, startedAtMs: Long = NOW - HOUR) =
        DrivingSessionRecord(
            id = id,
            startedAtMs = startedAtMs,
            durationSec = 60,
            distanceKm = 1f,
            score = score,
            insufficientReason = if (score == null) "DURATION_TOO_SHORT" else null,
            eventCounts = emptyMap(),
        )

    @Test
    fun `초기에는 기록이 없어 통계가 비어 있다`() = runTest {
        val viewModel = StatsViewModel(FakeRepository()) { NOW }
        advanceUntilIdle()

        assertTrue(!viewModel.state.value.display.hasStats)
    }

    @Test
    fun `갱신하면 새 기록이 통계에 반영된다`() = runTest {
        val repository = FakeRepository()
        val viewModel = StatsViewModel(repository) { NOW }
        advanceUntilIdle()

        repository.setRecords(listOf(record("a"), record("b")))
        viewModel.refresh()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.display.hasStats)
        assertEquals("2회", viewModel.state.value.display.sessionText)
    }

    @Test
    fun `갱신은 매번 저장소를 읽는다`() = runTest {
        val repository = FakeRepository(listOf(record("a")))
        val viewModel = StatsViewModel(repository) { NOW }
        advanceUntilIdle()
        val afterInit = repository.readCount

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(afterInit + 2, repository.readCount)
    }

    @Test
    fun `점수 없는 기록만 있으면 기록 없음과 구분한다`() = runTest {
        val repository = FakeRepository(listOf(record("a", score = null)))
        val viewModel = StatsViewModel(repository) { NOW }
        advanceUntilIdle()

        assertEquals(
            "최근 7일 기록은 있지만 점수를 낼 수 있는 주행이 없어요",
            viewModel.state.value.display.emptyMessage,
        )
    }

    @Test
    fun `기간 밖 기록은 통계에서 빠진다`() = runTest {
        val repository = FakeRepository(
            listOf(
                record("recent", startedAtMs = NOW - HOUR),
                record("old", startedAtMs = NOW - 30 * DAY),
            ),
        )
        val viewModel = StatsViewModel(repository) { NOW }
        advanceUntilIdle()

        assertEquals("1회", viewModel.state.value.display.sessionText)
    }

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
