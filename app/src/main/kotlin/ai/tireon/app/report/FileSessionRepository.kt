package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

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

    override suspend fun save(record: DrivingSessionRecord): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val current = readAll()
                val updated = (current + record)
                    .distinctBy { it.id }
                    .sortedByDescending { it.startedAtMs }
                    .take(maxRecords)
                writeAll(updated)
            }.onFailure { Log.e(TAG, "세션 기록 저장 실패", it) }
        }

    override suspend fun recent(limit: Int): List<DrivingSessionRecord> =
        withContext(ioDispatcher) {
            readAll().sortedByDescending { it.startedAtMs }.take(limit)
        }

    override suspend fun latest(): DrivingSessionRecord? = recent(limit = 1).firstOrNull()

    private fun readAll(): List<DrivingSessionRecord> {
        if (!file.isFile) return emptyList()
        return try {
            val text = file.readText()
            if (text.isBlank()) emptyList() else json.decodeFromString(serializer, text)
        } catch (e: Exception) {
            Log.e(TAG, "세션 기록을 읽지 못했습니다. 빈 목록으로 시작합니다", e)
            emptyList()
        }
    }

    private fun writeAll(records: List<DrivingSessionRecord>) {
        directory.mkdirs()
        val temp = File(directory, "$FILE_NAME.tmp")
        temp.writeText(json.encodeToString(serializer, records))
        if (!temp.renameTo(file)) {
            file.delete()
            if (!temp.renameTo(file)) {
                throw IllegalStateException("세션 기록 파일을 교체하지 못했습니다")
            }
        }
    }

    private companion object {
        const val TAG = "FileSessionRepository"
        const val FILE_NAME = "sessions.json"
        const val DEFAULT_MAX_RECORDS = 200
    }
}
