package ai.tireon.app.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.practice.MissionDisplay
import ai.tireon.app.practice.MissionSelection
import ai.tireon.core.mission.PracticeMission

/** 연습 탭. 미션을 고르면 세션 액티비티로 들어간다. */
@Composable
internal fun PracticeEntryScreen(
    onSelectMission: (PracticeMission) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.practice_select_mission),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(MissionSelection.all(), key = { it.mission.name }) { display ->
            MissionCard(display = display, onClick = { onSelectMission(display.mission) })
        }
    }
}

@Composable
private fun MissionCard(display: MissionDisplay, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(display.nameResId),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(display.descriptionResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
