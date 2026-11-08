package ai.tireon.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.driving.DrivingUiState
import ai.tireon.app.ui.DrivingDisplay
import ai.tireon.app.ui.WindowLayout

/**
 * 홈 화면.
 *
 * 점수가 없으면 숫자 대신 안내 문구만 보여준다. 0점과 구분하기 위함이다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    driving: DrivingUiState,
    layout: WindowLayout,
    onStartPractice: () -> Unit,
    onStartDriving: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (layout == WindowLayout.EXPANDED) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            ScoreCard(
                state = state,
                modifier = Modifier.weight(1f),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DrivingSummaryCard(driving = driving, onRefresh = onRefresh)
                Button(
                    onClick = onStartDriving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.driving_session_start))
                }
                Button(
                    onClick = onStartPractice,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.action_start))
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScoreCard(state = state, modifier = Modifier.fillMaxWidth())
            DrivingSummaryCard(driving = driving, onRefresh = onRefresh)
            Button(
                onClick = onStartDriving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.driving_session_start))
            }
            Button(
                onClick = onStartPractice,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.action_start))
            }
        }
    }
}

@Composable
private fun ScoreCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val score = state.scoreText
            if (score != null) {
                Text(
                    text = score,
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                text = state.statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DrivingSummaryCard(driving: DrivingUiState, onRefresh: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 단위는 SDK가 제공하지 않으므로 값을 그대로 표시한다.
            Text(
                text = stringResource(
                    R.string.distance_driven,
                    DrivingDisplay.distance(driving.distanceDriven)
                        ?: stringResource(R.string.value_unavailable),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    R.string.trip_driving_time,
                    DrivingDisplay.drivingTime(driving.tripDrivingTime)
                        ?: stringResource(R.string.value_unavailable),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            driving.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(onClick = onRefresh) {
                Text(text = stringResource(R.string.action_refresh))
            }
        }
    }
}
