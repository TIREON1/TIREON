package ai.tireon.app.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import ai.tireon.app.R

/**
 * 주행 세션 화면.
 *
 * 수집 중에는 아무것도 그리지 않는다. 실도로 주행 중 화면 노출은 안전 규정
 * 위반이고, 보여줄 정보도 없다.
 */
@Composable
fun DrivingSessionScreen(
    state: DrivingSessionState,
    result: DrivingSessionResult?,
    saveStatus: ai.tireon.app.report.SaveStatus = ai.tireon.app.report.SaveStatus.idle(),
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.hideUi) return

    when (state.phase) {
        DrivingSessionPhase.READY -> ReadyContent(onStart = onStart, modifier = modifier)
        DrivingSessionPhase.FINISHED -> ResultContent(
            result = result,
            saveStatus = saveStatus,
            onClose = onClose,
            modifier = modifier,
        )

        DrivingSessionPhase.COLLECTING -> Unit
    }
}

@Composable
private fun ReadyContent(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.driving_session_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.driving_session_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.driving_session_start))
        }
    }
}

@Composable
private fun ResultContent(
    result: DrivingSessionResult?,
    saveStatus: ai.tireon.app.report.SaveStatus,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val score = result?.scoreText
                if (score != null) {
                    Text(text = score, style = MaterialTheme.typography.displayLarge)
                }
                Text(
                    text = result?.message.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                result?.missionDetail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                result?.let {
                    Text(
                        text = stringResource(R.string.practice_event_count, it.eventCount),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        saveStatus.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.action_close))
        }
    }
}
