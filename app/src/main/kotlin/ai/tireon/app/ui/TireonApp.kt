package ai.tireon.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.tireon.app.R
import ai.tireon.app.driving.DrivingUiState
import ai.tireon.app.driving.DrivingViewModel

@Composable
fun TireonApp(
    drivingViewModel: DrivingViewModel = viewModel(
        factory = viewModelFactory(),
    ),
) {
    val state by drivingViewModel.uiState.collectAsStateWithLifecycle()
    TireonContent(
        state = state,
        onRefresh = drivingViewModel::refresh,
    )
}

@Composable
private fun TireonContent(
    state: DrivingUiState,
    onRefresh: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
            )

            // 단위는 Pleos SDK에서 제공하지 않으므로 값을 그대로 표시한다.
            Text(
                text = stringResource(
                    R.string.distance_driven,
                    DrivingDisplay.distance(state.distanceDriven)
                        ?: stringResource(R.string.value_unavailable),
                ),
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = stringResource(
                    R.string.trip_driving_time,
                    DrivingDisplay.drivingTime(state.tripDrivingTime)
                        ?: stringResource(R.string.value_unavailable),
                ),
                style = MaterialTheme.typography.titleMedium,
            )

            state.message?.let { message ->
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
