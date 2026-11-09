package ai.tireon.app.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R

/**
 * 미션별 성적 목록.
 *
 * 리포트 화면 안에 한 구역으로 들어간다. 미션 기록이 없으면 아무것도 그리지 않는다.
 * 빈 카드를 보여주면 사용자는 자기 기록이 사라진 줄 안다.
 */
@Composable
fun MissionStatsSection(
    display: MissionStatsDisplay,
    modifier: Modifier = Modifier,
) {
    if (!display.hasStats) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.mission_stats_title),
            style = MaterialTheme.typography.titleMedium,
        )

        display.hardestText?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                display.rows.forEach { row ->
                    MissionStatsRow(row = row)
                }
            }
        }
    }
}

@Composable
private fun MissionStatsRow(row: MissionStatsDisplay.MissionRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "${row.name} ${row.rateText} ${row.countText}" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = row.countText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
            )
        }
        Text(
            text = row.rateText,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
