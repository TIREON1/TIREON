package ai.tireon.app.report

import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 세션 기록 저장을 화면 수명과 분리한다.
 *
 * 결과 화면을 닫으면 ViewModel 스코프가 취소된다. 저장을 그 스코프에서 돌리면
 * 기록이 사라질 수 있어 앱 수명 스코프에서 끝까지 저장한다.
 */
class SessionSaver(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val callbackDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {

    fun save(
        repository: SessionRepository,
        record: DrivingSessionRecord,
        onSaved: (Result<Unit>) -> Unit = {},
    ) {
        scope.launch {
            val result = runCatching { repository.save(record) }
                .getOrElse { Result.failure(it) }
            withContext(callbackDispatcher) { onSaved(result) }
        }
    }

    companion object {
        /** 실제 앱에서 쓰는 인스턴스. 테스트에서 클래스 초기화만으로 Main 디스패처를 찾지 않게 지연 생성한다. */
        val shared: SessionSaver by lazy { SessionSaver() }
    }
}
