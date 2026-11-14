package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 세션 기록을 단일 JSON 파일로 저장한다.
 *
 * 쓰기는 임시 파일에 먼저 하고 이름을 바꾼다. 중간에 실패해도 기존 파일이
 * 깨지지 않게 하기 위함이다.
 *
 * 읽기에 실패하면 빈 목록을 반환한다. 파일이 깨졌다고 앱이 죽으면 안 된다.
 *
 * @param maxRecords 보관할 최대 기록 수. 넘으면 오래된 것부터 버린다.
 */
class FileSessionRepository(
    private val directory: File,
    private val maxRecords: Int = DEFAULT_MAX_RECORDS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SessionRepository {

    private val file = File(directory, FILE_NAME)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val serializer = ListSerializer(DrivingSessionRecord.serializer())

    /** 읽기·쓰기가 같은 파일을 동시에 건드리지 않게 한다. */
    private val mutex = Mutex()

    override suspend fun save(record: DrivingSessionRecord): Result<Unit> = mutex.withLock {
        withContext(ioDispatcher) {
            try {
                val current = readAll(failOnError = true)
                val updated = (current + record)
                    .distinctBy { it.id }
                    .sortedByDescending { it.startedAtMs }
                    .take(maxRecords)
                writeAll(updated)
                Result.success(Unit)
            } catch (e: CancellationException) {
                // 코루틴 취소는 저장 실패가 아니다. 삼키면 상위 취소가 사라진다.
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "세션 기록 저장 실패", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun recent(limit: Int): List<DrivingSessionRecord> = mutex.withLock {
        withContext(ioDispatcher) {
            readAll().sortedByDescending { it.startedAtMs }.take(limit)
        }
    }

    override suspend fun latest(): DrivingSessionRecord? = recent(limit = 1).firstOrNull()

    private fun readAll(failOnError: Boolean = false): List<DrivingSessionRecord> {
        if (!file.isFile) return emptyList()
        return try {
            val text = file.readText()
            if (text.isBlank()) emptyList() else json.decodeFromString(serializer, text)
        } catch (e: Exception) {
            // 읽지 못한 원본을 빈 목록으로 덮어쓰면 복구 가능한 기록까지 사라진다.
            if (failOnError) throw e
            Log.e(TAG, "세션 기록을 읽지 못했습니다. 빈 목록으로 시작합니다", e)
            emptyList()
        }
    }

    private fun writeAll(records: List<DrivingSessionRecord>) {
        directory.mkdirs()
        val temp = File(directory, "$FILE_NAME.tmp")
        temp.writeText(json.encodeToString(serializer, records))

        // 원본을 지우기 전에 교체한다. 이동에 실패해도 기존 기록은 남는다.
        try {
            moveIntoPlace(temp)
        } catch (e: Exception) {
            temp.delete()
            throw e
        }
    }

    private fun moveIntoPlace(temp: File) {
        try {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (e: AtomicMoveNotSupportedException) {
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
    }

    private companion object {
        const val TAG = "FileSessionRepository"
        const val FILE_NAME = "sessions.json"
        const val DEFAULT_MAX_RECORDS = 200
    }
}
