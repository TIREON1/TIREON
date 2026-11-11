package ai.tireon.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = MISSION_CARD_MIN_WIDTH),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TireonDimens.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(
                title = stringResource(R.string.practice_select_mission),
                subtitle = stringResource(R.string.practice_entry_subtitle),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(MissionSelection.all(), key = { it.mission.name }) { display ->
            PracticeMissionCard(display = display, onClick = { onSelectMission(display.mission) })
        }
    }
}

internal val MISSION_CARD_MIN_WIDTH = 320.dp

@Composable
private fun PracticeMissionCard(display: MissionDisplay, onClick: () -> Unit) {
    MissionOptionCard(
        icon = MissionIcons.of(display.mission),
        name = stringResource(display.nameResId),
        description = stringResource(display.descriptionResId),
        onClick = onClick,
    )
}

/** 미션 선택 카드. 카드 전체가 터치 영역이다. */
@Composable
internal fun MissionOptionCard(
    @DrawableRes icon: Int,
    name: String,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        color = if (highlighted) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(
                icon = icon,
                size = 56.dp,
                tint = if (highlighted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                container = if (highlighted) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = description,
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
