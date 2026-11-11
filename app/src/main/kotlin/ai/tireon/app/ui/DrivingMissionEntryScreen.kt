package ai.tireon.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = MISSION_CARD_MIN_WIDTH),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TireonDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(
                title = stringResource(R.string.driving_mission_select_title),
                subtitle = stringResource(R.string.driving_mission_select_description),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        item(key = "skip") {
            MissionOptionCard(
                icon = MissionIcons.of(DrivingMissionSelection.forSkip()),
                name = stringResource(DrivingMissionSelection.skipNameResId()),
                description = stringResource(DrivingMissionSelection.skipDescriptionResId()),
                onClick = { onSelect(DrivingMissionSelection.forSkip()) },
                highlighted = true,
            )
        }

        items(DrivingMissionSelection.all(), key = { it.mission.name }) { display ->
            MissionOptionCard(
                icon = MissionIcons.of(display.mission),
                name = stringResource(display.nameResId),
                description = stringResource(display.descriptionResId),
                onClick = { onSelect(display.mission) },
            )
        }
    }
}
