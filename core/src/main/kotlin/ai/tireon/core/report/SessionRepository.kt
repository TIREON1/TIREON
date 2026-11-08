package ai.tireon.core.report

/**
 * 세션 기록 저장소.
 *
 * 저장 실패를 호출 측이 알 수 있도록 [save]는 [Result]를 반환한다. 조용히
 * 삼키면 사용자가 기록이 사라진 것을 모른다.
 */
interface SessionRepository {

    suspend fun save(record: DrivingSessionRecord): Result<Unit>

    /** 최신 순으로 [limit]개를 반환한다. */
    suspend fun recent(limit: Int): List<DrivingSessionRecord>

    suspend fun latest(): DrivingSessionRecord?
}
