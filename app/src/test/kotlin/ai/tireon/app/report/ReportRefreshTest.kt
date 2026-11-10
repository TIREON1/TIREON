package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * 리포트 갱신 동작.
 *
 * 주행을 마치고 앱으로 돌아오면 새 기록이 생긴다. 그때 목록을 다시 읽되,
 * 사용자가 보던 상세 화면을 빼앗지 않아야 한다.
 */
class ReportRefreshTest {

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

    private fun record(id: String, score: Int = 85) = DrivingSessionRecord(
        id = id,
        startedAtMs = 1_700_000_000_000L,
        durationSec = 60,
        distanceKm = 1f,
        score = score,
        insufficientReason = null,
        eventCounts = emptyMap(),
    )

    @Test
    fun `갱신하면 새 기록이 목록에 들어온다`() = runTest {
        val repository = FakeRepository(listOf(record("old")))
        val viewModel = ReportViewModel(repository)
        advanceUntilIdle()

        repository.setRecords(listOf(record("new"), record("old")))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.records.size)
    }

    @Test
    fun `갱신해도 보던 상세 화면은 유지된다`() = runTest {
        val repository = FakeRepository(listOf(record("a"), record("b")))
        val viewModel = ReportViewModel(repository)
        advanceUntilIdle()
        viewModel.select(viewModel.state.value.records.first())

        viewModel.refresh()
        advanceUntilIdle()

        val selected = viewModel.state.value.selected
        assertNotNull(selected)
        assertEquals("a", selected.id)
    }

    @Test
    fun `보던 기록이 사라졌으면 목록으로 돌아간다`() = runTest {
        val repository = FakeRepository(listOf(record("a")))
        val viewModel = ReportViewModel(repository)
        advanceUntilIdle()
        viewModel.select(viewModel.state.value.records.first())

        repository.setRecords(listOf(record("b")))
        viewModel.refresh()
        advanceUntilIdle()

        assertNull(viewModel.state.value.selected)
    }

    @Test
    fun `갱신은 매번 저장소를 읽는다`() = runTest {
        val repository = FakeRepository(listOf(record("a")))
        val viewModel = ReportViewModel(repository)
        advanceUntilIdle()
        val afterInit = repository.readCount

        viewModel.refresh()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(afterInit + 2, repository.readCount)
    }
}
