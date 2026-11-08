package ai.tireon.app.home

import ai.tireon.core.analysis.InsufficientReason
import ai.tireon.core.report.DrivingSessionRecord
import ai.tireon.core.report.SessionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 홈 화면 상태를 제공한다.
 *
 * 저장된 최신 세션에서 점수를 가져온다. 기록이 없으면 기록 없음 상태다.
 */
class HomeViewModel(
    private val repository: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState.empty())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val recent = repository.recent(RECOMMEND_WINDOW)
            val base = recent.firstOrNull()?.toUiState() ?: HomeUiState.empty()

            _state.value = base.copy(
                recommendedMission = MissionRecommender.recommend(recent),
                recommendReason = MissionRecommender.reasonFor(recent),
            )
        }
    }

    private fun DrivingSessionRecord.toUiState(): HomeUiState {
        val recordedScore = score
        if (recordedScore != null) {
            return HomeUiState(
                scoreText = recordedScore.toString(),
                statusText = "최근 운전 점수",
                hasScore = true,
            )
        }

        val reason = insufficientReason
            ?.let { name -> runCatching { InsufficientReason.valueOf(name) }.getOrNull() }
            ?: InsufficientReason.NOT_ENOUGH_SAMPLES
        return HomeUiState.from(ai.tireon.core.analysis.ScoreResult.Insufficient(reason))
    }

    private companion object {
        /** 추천에 사용할 최근 기록 수. */
        const val RECOMMEND_WINDOW = 10
    }

    class Factory(private val repository: SessionRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}
