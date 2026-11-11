package ai.tireon.app.driving

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.ui.ActionStyle
import ai.tireon.app.ui.CenteredColumn
import ai.tireon.app.ui.IconBadge
import ai.tireon.app.ui.MissionIcons
import ai.tireon.app.ui.ScoreRing
import ai.tireon.app.ui.SectionCard
import ai.tireon.app.ui.StatusChip
import ai.tireon.app.ui.TireonActionButton

/**
 * 주행 세션 화면.
 *
 * 수집 중 제한이 걸려 있으면 아무것도 그리지 않는다. 실도로 주행 중 화면 노출은
 * 안전 규정 위반이고, 보여줄 정보도 없다. 정차해 제한이 풀리면 종료 버튼만 보여준다.
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

        DrivingSessionPhase.COLLECTING -> CollectingContent(
            sampleCount = state.sampleCount,
            onStop = onStop,
            modifier = modifier,
        )
    }
}

@Composable
private fun ReadyContent(onStart: () -> Unit, modifier: Modifier = Modifier) {
    CenteredColumn(modifier = modifier) {
        IconBadge(
            icon = R.drawable.ic_car,
            size = 96.dp,
            tint = MaterialTheme.colorScheme.secondary,
            container = MaterialTheme.colorScheme.secondaryContainer,
        )
        Text(
            text = stringResource(R.string.driving_session_title),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = stringResource(R.string.driving_session_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TireonActionButton(
            text = stringResource(R.string.driving_session_start),
            onClick = onStart,
            icon = R.drawable.ic_play,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun CollectingContent(
    sampleCount: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredColumn(modifier = modifier) {
        StatusChip(
            text = stringResource(R.string.driving_session_recording),
            icon = R.drawable.ic_timer,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(R.string.driving_session_recording_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.driving_session_recording_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.practice_sample_count, sampleCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TireonActionButton(
            text = stringResource(R.string.driving_session_stop),
            onClick = onStop,
            icon = R.drawable.ic_stop,
            style = ActionStyle.DANGER,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun ResultContent(
    result: DrivingSessionResult?,
    saveStatus: ai.tireon.app.report.SaveStatus,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredColumn(modifier = modifier) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ScoreRing(scoreText = result?.scoreText)
                Text(
                    text = result?.message.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                result?.mission?.let { mission ->
                    StatusChip(
                        text = stringResource(
                            R.string.driving_mission_label,
                            stringResource(DrivingMissionSelection.of(mission).nameResId),
                        ),
                        icon = MissionIcons.of(mission),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                result?.missionDetail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                result?.let {
                    Text(
                        text = stringResource(R.string.practice_event_count, it.eventCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        TireonActionButton(
            text = stringResource(R.string.action_close),
            onClick = onClose,
            style = ActionStyle.SECONDARY,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
