package ai.tireon.app.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.ui.EventBars
import ai.tireon.app.ui.ScoreRing
import ai.tireon.app.ui.SectionCard
import ai.tireon.app.ui.StatTile
import ai.tireon.app.ui.StatusChip
import ai.tireon.app.ui.TireonDimens
import ai.tireon.app.ui.theme.LocalStatusColors

/**
 * 세션 상세 화면.
 *
 * 점수가 왜 그렇게 나왔는지 근거를 보여준다. 사용자가 개선점을 파악하려면
 * 이벤트별 횟수와 미션 결과가 필요하다.
 */
@Composable
fun ReportDetailScreen(
    detail: ReportDetail,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= WIDE_DETAIL_MIN_WIDTH
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TireonDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
        ) {
            TextButton(onClick = onBack) {
                Text(
                    text = stringResource(R.string.report_detail_back),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(TireonDimens.Gap)) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
                    ) {
                        ScoreSection(detail = detail)
                        MetaSection(detail = detail)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
                    ) {
                        EventsSection(detail = detail)
                        detail.missionLabel?.let { MissionSection(label = it, detail = detail) }
                    }
                }
            } else {
                ScoreSection(detail = detail)
                MetaSection(detail = detail)
                EventsSection(detail = detail)
                detail.missionLabel?.let { MissionSection(label = it, detail = detail) }
            }
        }
    }
}

private val WIDE_DETAIL_MIN_WIDTH = 840.dp

@Composable
private fun ScoreSection(detail: ReportDetail) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            detail.kind?.let { kind ->
                StatusChip(
                    text = kind.label(),
                    icon = kind.iconRes,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ScoreRing(scoreText = detail.scoreText)
            Text(
                text = detail.scoreReason ?: stringResource(R.string.report_detail_score_caption),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MetaSection(detail: ReportDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            label = stringResource(R.string.report_detail_date),
            value = detail.dateText,
            icon = R.drawable.ic_event,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.report_detail_duration),
                value = detail.durationText,
                icon = R.drawable.ic_timer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.report_detail_distance),
                value = detail.distanceText,
                icon = R.drawable.ic_place,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EventsSection(detail: ReportDetail) {
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.report_detail_events),
        titleIcon = R.drawable.ic_warning,
    ) {
        if (detail.events.isEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = LocalStatusColors.current.good,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.report_detail_no_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            EventBars(
                lines = detail.events.map { it.label to it.count },
                countText = { stringResource(R.string.report_detail_event_count, it) },
            )
        }
    }
}

@Composable
private fun MissionSection(label: String, detail: ReportDetail) {
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.report_detail_mission),
        titleIcon = R.drawable.ic_practice,
    ) {
        StatusChip(
            text = label,
            icon = if (detail.missionSuccess == false) R.drawable.ic_replay else R.drawable.ic_check_circle,
            color = when (detail.missionSuccess) {
                true -> LocalStatusColors.current.good
                false -> MaterialTheme.colorScheme.tertiary
                null -> MaterialTheme.colorScheme.outline
            },
        )
        detail.missionDetail?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
