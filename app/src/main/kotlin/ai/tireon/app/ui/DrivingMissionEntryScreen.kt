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
import ai.tireon.app.driving.DrivingMissionDisplay
import ai.tireon.app.driving.DrivingMissionSelection
import ai.tireon.core.mission.DrivingMission

/**
 * 주행 미션 선택 화면.
 *
 * 주행이 시작되면 화면이 가려지므로 시작 전에 고른다. 미션 없이 주행하는
 * 선택지를 맨 위에 둔다. 미션을 강요하지 않는다는 뜻을 먼저 보여주기 위함이다.
 */
@Composable
internal fun DrivingMissionEntryScreen(
    onSelect: (DrivingMission?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.driving_mission_select_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.driving_mission_select_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "skip") {
            MissionOption(
                name = stringResource(DrivingMissionSelection.skipNameResId()),
                description = stringResource(DrivingMissionSelection.skipDescriptionResId()),
                onClick = { onSelect(DrivingMissionSelection.forSkip()) },
            )
        }

        items(DrivingMissionSelection.all(), key = { it.mission.name }) { display ->
            DrivingMissionOption(display = display, onClick = { onSelect(display.mission) })
        }
    }
}

@Composable
private fun DrivingMissionOption(
    display: DrivingMissionDisplay,
    onClick: () -> Unit,
) {
    MissionOption(
        name = stringResource(display.nameResId),
        description = stringResource(display.descriptionResId),
        onClick = onClick,
    )
}

@Composable
private fun MissionOption(
    name: String,
    description: String,
    onClick: () -> Unit,
) {
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
                text = name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
