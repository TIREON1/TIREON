package ai.tireon.app.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.ui.EventBars
import ai.tireon.app.ui.ScoreRing
import ai.tireon.app.ui.ScreenHeader
import ai.tireon.app.ui.SectionCard
import ai.tireon.app.ui.StatTile
import ai.tireon.app.ui.TireonDimens

/**
 * 기간별 통계 화면.
 *
 * 점수 추이와 이벤트 발생 횟수를 보여준다. "운전이 좋아지고 있는지"를 한눈에
 * 확인할 수 있어야 한다.
 *
 * 단독 화면으로 쓸 때만 이 함수를 쓴다. 다른 스크롤 안에 넣을 때는 [StatsSection]을
 * 써야 한다. 세로 스크롤을 중첩하면 측정에 실패해 예외가 난다.
 */
@Composable
fun StatsScreen(
    display: StatsDisplay,
    modifier: Modifier = Modifier,
) {
    if (!display.hasStats) {
        EmptyStats(
            message = display.emptyMessage,
            modifier = modifier.fillMaxSize().padding(24.dp),
            titleStyle = MaterialTheme.typography.headlineLarge,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TireonDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.stats_title))

        ScoreCard(display = display)
        SummaryCard(display = display)
        EventsCard(display = display)
    }
}

/**
 * 리포트처럼 이미 스크롤되는 화면에 넣는 통계 구역.
 *
 * 자체 스크롤과 고정 크기를 갖지 않는다. 부모가 높이를 정하고 스크롤도 맡는다.
 * 여기에 `fillMaxSize`나 `verticalScroll`을 넣으면 부모가 무한 높이를 줬을 때
 * 예외가 난다.
 */
@Composable
fun StatsSection(
    display: StatsDisplay,
    modifier: Modifier = Modifier,
) {
    if (!display.hasStats) return

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.titleLarge,
        )

        ScoreCard(display = display)
        SummaryCard(display = display)
        EventsCard(display = display)
    }
}

@Composable
private fun EmptyStats(
    message: String,
    modifier: Modifier,
    titleStyle: androidx.compose.ui.text.TextStyle,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = titleStyle,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ScoreCard(display: StatsDisplay) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ScoreRing(scoreText = display.averageText, size = 128.dp, strokeWidth = 10.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.stats_average_score),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                display.changeText?.let { change ->
                    Text(
                        text = change,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(display: StatsDisplay) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatTile(
            label = stringResource(R.string.stats_session_count),
            value = display.sessionText ?: "-",
            icon = R.drawable.ic_car,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.stats_total_distance),
            value = display.distanceText ?: "-",
            icon = R.drawable.ic_place,
            modifier = Modifier.weight(1f),
        )
        StatTile(
            label = stringResource(R.string.stats_total_events),
            value = display.eventText ?: "-",
            icon = R.drawable.ic_warning,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun EventsCard(display: StatsDisplay) {
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.stats_event_breakdown),
    ) {
        if (display.topEvents.isEmpty()) {
            Text(
                text = stringResource(R.string.stats_no_events),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            EventBars(
                lines = display.topEvents.map { it.label to it.count },
                countText = { "${it}회" },
            )
        }
    }
}
