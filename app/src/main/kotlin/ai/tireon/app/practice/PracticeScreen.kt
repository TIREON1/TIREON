package ai.tireon.app.practice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.tireon.app.R
import ai.tireon.app.ui.ActionStyle
import ai.tireon.app.ui.CenteredColumn
import ai.tireon.app.ui.IconBadge
import ai.tireon.app.ui.MissionIcons
import ai.tireon.app.ui.ScoreRing
import ai.tireon.app.ui.SectionCard
import ai.tireon.app.ui.StatusChip
import ai.tireon.app.ui.TireonActionButton
import ai.tireon.app.ui.TireonDimens
import ai.tireon.core.coach.CoachMessage
import ai.tireon.core.coach.Severity

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
    saveStatus: ai.tireon.app.report.SaveStatus = ai.tireon.app.report.SaveStatus.idle(),
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
            mission = mission,
            elapsedSec = state.elapsedSec,
            sampleCount = state.sampleCount,
            onStop = onStop,
            modifier = modifier,
        )

        else -> SummaryContent(
            mission = mission,
            summary = summary,
            saveStatus = saveStatus,
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
    CenteredColumn(modifier = modifier) {
        IconBadge(icon = MissionIcons.of(mission.mission), size = 96.dp)
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
        TireonActionButton(
            text = stringResource(R.string.action_start),
            onClick = onStart,
            icon = R.drawable.ic_play,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun MinimalContent(speedText: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TireonDimens.ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (speedText != null) {
            Text(
                text = speedText,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp, lineHeight = 124.sp),
            )
            Text(
                text = stringResource(R.string.practice_speed_unit),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.value_unavailable),
                style = MaterialTheme.typography.displayMedium,
            )
        }
    }
}

@Composable
private fun RunningContent(
    mission: MissionDisplay,
    elapsedSec: Int,
    sampleCount: Int,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredColumn(modifier = modifier) {
        StatusChip(
            text = stringResource(R.string.practice_running),
            icon = R.drawable.ic_timer,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = stringResource(mission.nameResId),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.practice_elapsed, elapsedSec),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 96.sp, lineHeight = 100.sp),
        )
        Text(
            text = stringResource(R.string.practice_sample_count, sampleCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.practice_running_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TireonActionButton(
            text = stringResource(R.string.action_stop),
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
private fun SummaryContent(
    mission: MissionDisplay,
    summary: PracticeSummary?,
    saveStatus: ai.tireon.app.report.SaveStatus,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= WIDE_SUMMARY_MIN_WIDTH
        val coach = summary?.coachMessages.orEmpty()

        if (wide && coach.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(TireonDimens.ScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
                ) {
                    ResultCard(mission = mission, summary = summary)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
                ) {
                    CoachCard(messages = coach)
                    SummaryFooter(saveStatus = saveStatus, onClose = onClose)
                }
            }
        } else {
            CenteredColumn {
                ResultCard(mission = mission, summary = summary)
                if (coach.isNotEmpty()) CoachCard(messages = coach)
                SummaryFooter(saveStatus = saveStatus, onClose = onClose)
            }
        }
    }
}

private val WIDE_SUMMARY_MIN_WIDTH = 840.dp

@Composable
private fun ResultCard(mission: MissionDisplay, summary: PracticeSummary?) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(mission.nameResId),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ScoreRing(scoreText = summary?.scoreText)
            Text(
                text = summary?.message.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            summary?.let {
                MissionResultChip(success = it.missionSuccess)
                it.missionDetail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                Text(
                    text = stringResource(R.string.practice_event_count, it.eventCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 미션 달성 여부. 판정할 수 없으면 아무것도 그리지 않는다. */
@Composable
internal fun MissionResultChip(success: Boolean?, modifier: Modifier = Modifier) {
    when (success) {
        true -> StatusChip(
            text = stringResource(R.string.mission_success),
            icon = R.drawable.ic_check_circle,
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier,
        )
        false -> StatusChip(
            text = stringResource(R.string.mission_failed),
            icon = R.drawable.ic_replay,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier,
        )
        null -> Unit
    }
}

/**
 * 코칭 문장 목록.
 *
 * 문장(해석)과 근거(측정값)를 다른 줄, 다른 색으로 나눠 둘을 섞어 읽지 않게 한다.
 */
@Composable
private fun CoachCard(messages: List<CoachMessage>) {
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.coach_title),
        titleIcon = R.drawable.ic_sparkle,
    ) {
        messages.forEach { message ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val color = when (message.severity) {
                    Severity.GOOD -> MaterialTheme.colorScheme.primary
                    Severity.WARN -> MaterialTheme.colorScheme.tertiary
                    Severity.CRITICAL -> MaterialTheme.colorScheme.error
                }
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(10.dp)
                        .background(color, CircleShape),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge)
                    message.evidence?.let { evidence ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_speed),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = evidence,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryFooter(
    saveStatus: ai.tireon.app.report.SaveStatus,
    onClose: () -> Unit,
) {
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
