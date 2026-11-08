package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 미션 결과가 저장되고 다시 읽히는지 실제 파일 시스템에서 확인한다.
 */
@RunWith(AndroidJUnit4::class)
class MissionResultStorageTest {

    private lateinit var directory: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.filesDir, "mission-result-test")
        directory.deleteRecursively()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun record(
        id: String,
        mission: String?,
        success: Boolean?,
        detail: String?,
    ) = DrivingSessionRecord(
        id = id,
        startedAtMs = 1_000L,
        durationSec = 60,
        distanceKm = 1f,
        score = 90,
        insufficientReason = null,
        eventCounts = emptyMap(),
        mission = mission,
        missionSuccess = success,
        missionDetail = detail,
    )

    @Test
    fun 미션_성공이_저장되고_유지된다() = runBlocking {
        val repository = FileSessionRepository(directory)

        repository.save(record("a", "SMOOTH_START", true, "급가속 없이 출발했어요"))
        val loaded = repository.latest()

        assertEquals("SMOOTH_START", loaded?.mission)
        assertEquals(true, loaded?.missionSuccess)
        assertEquals("급가속 없이 출발했어요", loaded?.missionDetail)
    }

    @Test
    fun 미션_실패가_저장되고_유지된다() = runBlocking {
        val repository = FileSessionRepository(directory)

        repository.save(record("b", "SMOOTH_STOP", false, "급제동이 있었어요 (2 번)"))
        val loaded = repository.latest()

        assertEquals("SMOOTH_STOP", loaded?.mission)
        assertEquals(false, loaded?.missionSuccess)
    }

    @Test
    fun 판정_불가는_달성_여부_없이_저장된다() = runBlocking {
        val repository = FileSessionRepository(directory)

        repository.save(record("c", "STEADY_SPEED", null, "속도 신호를 받지 못했어요"))
        val loaded = repository.latest()

        assertEquals("STEADY_SPEED", loaded?.mission)
        assertNull(loaded?.missionSuccess)
        assertEquals("속도 신호를 받지 못했어요", loaded?.missionDetail)
    }

    @Test
    fun 미션이_없는_기록도_저장된다() = runBlocking {
        val repository = FileSessionRepository(directory)

        repository.save(record("d", null, null, null))
        val loaded = repository.latest()

        assertNull(loaded?.mission)
        assertNull(loaded?.missionSuccess)
        assertNull(loaded?.missionDetail)
    }
}
