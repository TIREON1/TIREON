package ai.tireon.app.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.practice.MissionResultChip
import ai.tireon.app.ui.IconBadge
import ai.tireon.app.ui.ScoreTrendChart
import ai.tireon.app.ui.ScreenHeader
import ai.tireon.app.ui.SectionCard
import ai.tireon.app.ui.TireonDimens
import ai.tireon.app.ui.color
import ai.tireon.app.ui.scoreToneOf
import ai.tireon.core.report.DrivingSessionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 리포트 화면. 기록이 없으면 안내만 보여준다.
 *
 * 넓은 화면에서는 왼쪽에 통계, 오른쪽에 기록 목록을 둔다. 좁은 화면에서는
 * 하나의 목록으로 이어 붙인다.
 */
@Composable
fun ReportScreen(
    state: ReportUiState,
    statsDisplay: ai.tireon.app.stats.StatsDisplay? = null,
    onSelectRecord: (DrivingSessionRecord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (state.loading) return

    if (state.records.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(TireonDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(icon = R.drawable.ic_report, size = 112.dp)
            Text(
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.report_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // 기록은 최신순이다. 그래프는 왼쪽에서 오른쪽으로 시간이 흐르게 뒤집는다.
    val trend = state.records.mapNotNull { it.score }.take(TREND_SIZE).reversed()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= TWO_PANE_MIN_WIDTH) {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(TireonDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
                ) {
                    summaryItems(state = state, statsDisplay = statsDisplay, trend = trend)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(TireonDimens.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    recordItems(state = state, onSelectRecord = onSelectRecord)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(TireonDimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
            ) {
                summaryItems(state = state, statsDisplay = statsDisplay, trend = trend)
                recordItems(state = state, onSelectRecord = onSelectRecord)
            }
        }
    }
}

private const val TREND_SIZE = 10
private val TWO_PANE_MIN_WIDTH = 840.dp

private fun LazyListScope.summaryItems(
    state: ReportUiState,
    statsDisplay: ai.tireon.app.stats.StatsDisplay?,
    trend: List<Int>,
) {
    item(key = "header") {
        ScreenHeader(title = stringResource(R.string.report_title))
    }

    statsDisplay?.takeIf { it.hasStats }?.let { display ->
        item(key = "stats") {
            ai.tireon.app.stats.StatsSection(display = display)
        }
    }

    // 막대 하나로는 추이를 말할 수 없으므로 두 번 이상 점수가 났을 때만 그린다.
    if (trend.size >= 2) {
        item(key = "trend") {
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.report_trend_title),
                titleIcon = R.drawable.ic_trending_up,
            ) {
                ScoreTrendChart(scores = trend)
            }
        }
    }

    item(key = "mission_stats") {
        MissionStatsSection(display = state.missionStats)
    }
}

private fun LazyListScope.recordItems(
    state: ReportUiState,
    onSelectRecord: (DrivingSessionRecord) -> Unit,
) {
    item(key = "records_header") {
        Text(
            text = stringResource(R.string.report_records_title),
            style = MaterialTheme.typography.titleLarge,
        )
    }
    items(state.records, key = { it.id }) { record ->
        RecordCard(record = record, onClick = { onSelectRecord(record) })
    }
}

@Composable
private fun RecordCard(record: DrivingSessionRecord, onClick: () -> Unit) {
    SectionCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 점수 구간 색 띠. 숫자를 함께 쓰므로 색만으로 뜻을 전하지 않는다.
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(72.dp)
                    .clip(CircleShape)
                    .background(scoreToneOf(record.score).color()),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val kind = SessionKind.of(record)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(kind.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = kind.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDate(record.startedAtMs),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val score = record.score
                if (score != null) {
                    Text(
                        text = stringResource(R.string.report_score, score),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.report_no_score),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                record.missionDetail?.let { detail ->
                    MissionResultChip(success = record.missionSuccess)
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val totalEvents = record.eventCounts.values.sum()
                Text(
                    text = stringResource(R.string.practice_event_count, totalEvents),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun formatDate(timestampMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN)
    return formatter.format(Date(timestampMs))
}
