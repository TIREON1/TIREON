package ai.tireon.app.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import ai.tireon.core.report.DrivingSessionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 리포트 화면. 기록이 없으면 안내만 보여준다. */
@Composable
fun ReportScreen(
    state: ReportUiState,
    onSelectRecord: (DrivingSessionRecord) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (state.loading) return

    if (state.records.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.report_title),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = stringResource(R.string.report_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.records, key = { it.id }) { record ->
            RecordCard(record = record, onClick = { onSelectRecord(record) })
        }
    }
}

@Composable
private fun RecordCard(record: DrivingSessionRecord, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = formatDate(record.startedAtMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val score = record.score
            if (score != null) {
                Text(
                    text = stringResource(R.string.report_score, score),
                    style = MaterialTheme.typography.headlineMedium,
                )
            } else {
                Text(
                    text = stringResource(R.string.report_no_score),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            record.missionDetail?.let { detail ->
                val missionLabel = when (record.missionSuccess) {
                    true -> stringResource(R.string.mission_success)
                    false -> stringResource(R.string.mission_failed)
                    null -> null
                }
                if (missionLabel != null) {
                    Text(
                        text = missionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (record.missionSuccess == true) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
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
            )
        }
    }
}

private fun formatDate(timestampMs: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREAN)
    return formatter.format(Date(timestampMs))
}
