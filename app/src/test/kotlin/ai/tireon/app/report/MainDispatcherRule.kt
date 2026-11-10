package ai.tireon.app.report

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `viewModelScope`가 쓰는 Main 디스패처를 테스트 디스패처로 바꾼다.
 *
 * ViewModel 안의 코루틴은 Main에서 돌아간다. JVM 단위 테스트에는 Main이 없으므로
 * 규칙으로 바꿔줘야 `init`과 `refresh`의 코루틴이 실행된다.
 */
class MainDispatcherRule(
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
