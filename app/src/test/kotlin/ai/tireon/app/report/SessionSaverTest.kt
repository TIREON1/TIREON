package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SessionSaverTest {

    private class FakeRepository(
        private val result: Result<Unit>,
    ) : SessionRepository {
        override suspend fun save(record: DrivingSessionRecord): Result<Unit> = result
        override suspend fun recent(limit: Int) = emptyList<DrivingSessionRecord>()
        override suspend fun latest(): DrivingSessionRecord? = null
    }

    private class ThrowingRepository : SessionRepository {
        override suspend fun save(record: DrivingSessionRecord): Result<Unit> =
            throw IllegalStateException("disk full")

        override suspend fun recent(limit: Int) = emptyList<DrivingSessionRecord>()
        override suspend fun latest(): DrivingSessionRecord? = null
    }

    private fun record() = DrivingSessionRecord(
        id = "save-test",
        startedAtMs = 0L,
        durationSec = 60,
        distanceKm = 1f,
        score = 80,
        insufficientReason = null,
        eventCounts = emptyMap(),
    )

    @Test
    fun `저장 결과를 콜백으로 전달한다`() = runTest {
        val saver = SessionSaver(this, StandardTestDispatcher(testScheduler))
        var saved: Result<Unit>? = null

        saver.save(FakeRepository(Result.success(Unit)), record()) { saved = it }
        advanceUntilIdle()

        assertTrue(saved?.isSuccess == true)
    }

    @Test
    fun `저장 중 예외가 나도 실패 결과로 전달한다`() = runTest {
        val saver = SessionSaver(this, StandardTestDispatcher(testScheduler))
        var saved: Result<Unit>? = null

        saver.save(
            ThrowingRepository(),
            record(),
        ) { saved = it }
        advanceUntilIdle()

        assertTrue(saved?.isFailure == true)
    }
}
