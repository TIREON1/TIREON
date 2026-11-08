package ai.tireon.app.home

import androidx.lifecycle.ViewModel
import ai.tireon.core.analysis.ScoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 홈 화면 상태를 제공한다.
 *
 * 점수 저장소가 생기기 전까지는 기록 없음 상태를 유지한다. 저장소가 붙으면
 * [updateScore]로 실제 결과를 넣고, 화면 코드는 바꾸지 않는다.
 */
class HomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState.empty())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun updateScore(result: ScoreResult) {
        _state.value = HomeUiState.from(result)
    }

    fun clear() {
        _state.value = HomeUiState.empty()
    }
}
