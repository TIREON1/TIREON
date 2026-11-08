package ai.tireon.app.practice

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R

/**
 * 연습 세션 화면.
 *
 * 제한이 걸린 상태에서는 숫자 하나만 표시하고 터치 요소를 두지 않는다.
 */
@Composable
fun PracticeScreen(
    mission: MissionDisplay,
    state: PracticeSessionState,
    summary: PracticeSummary?,
    speedText: String?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.phase == PracticePhase.READY -> ReadyContent(
            mission = mission,
            onStart = onStart,
            modifier = modifier,
        )

        state.phase == PracticePhase.RUNNING && state.showMinimalUi -> MinimalContent(
            speedText = speedText,
            modifier = modifier,
        )

        state.phase == PracticePhase.RUNNING -> RunningContent(
            elapsedSec = state.elapsedSec,
            sampleCount = state.sampleCount,
            onStop = onStop,
            modifier = modifier,
        )

        else -> SummaryContent(
            summary = summary,
            onClose = onClose,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReadyContent(
    mission: MissionDisplay,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(mission.nameResId),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(mission.descriptionResId),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.action_start))
        }
    }
}

@Composable
private fun MinimalContent(speedText: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = speedText ?: stringResource(R.string.value_unavailable),
            style = MaterialTheme.typography.displayLarge,
        )
    }
}

@Composable
private fun RunningContent(
    elapsedSec: Int,
    sampleCount: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.practice_elapsed, elapsedSec),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.practice_sample_count, sampleCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.action_stop))
        }
    }
}

@Composable
private fun SummaryContent(
    summary: PracticeSummary?,
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
                val score = summary?.scoreText
                if (score != null) {
                    Text(text = score, style = MaterialTheme.typography.displayLarge)
                }
                Text(
                    text = summary?.message.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                summary?.let {
                    val missionLabel = when (it.missionSuccess) {
                        true -> stringResource(R.string.mission_success)
                        false -> stringResource(R.string.mission_failed)
                        null -> null
                    }
                    if (missionLabel != null) {
                        Text(
                            text = missionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (it.missionSuccess == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    it.missionDetail?.let { detail ->
                        Text(
                            text = detail,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        text = stringResource(R.string.practice_event_count, it.eventCount),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.action_close))
        }
    }
}
