package ai.tireon.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.R
import ai.tireon.app.driving.DrivingUiState
import ai.tireon.app.ui.ActionStyle
import ai.tireon.app.ui.DrivingDisplay
import ai.tireon.app.ui.IconBadge
import ai.tireon.app.ui.MissionIcons
import ai.tireon.app.ui.ScoreRing
import ai.tireon.app.ui.SectionCard
import ai.tireon.app.ui.StatTile
import ai.tireon.app.ui.TireonActionButton
import ai.tireon.app.ui.TireonDimens
import ai.tireon.app.ui.WindowLayout

/**
 * 홈 화면.
 *
 * 점수가 없으면 숫자 대신 안내 문구만 보여준다. 0점과 구분하기 위함이다.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    driving: DrivingUiState,
    layout: WindowLayout,
    onStartPractice: (ai.tireon.core.mission.PracticeMission) -> Unit,
    onStartDriving: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (layout == WindowLayout.EXPANDED) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TireonDimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
            ) {
                Greeting()
                ScoreCard(state = state, modifier = Modifier.fillMaxWidth())
                VehicleCard(driving = driving, onRefresh = onRefresh)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
            ) {
                RecommendCard(state = state, onStartPractice = onStartPractice)
                StartCard(
                    icon = R.drawable.ic_car,
                    description = stringResource(R.string.home_drive_description),
                    action = stringResource(R.string.driving_session_start),
                    style = ActionStyle.PRIMARY,
                    onClick = onStartDriving,
                )
                StartCard(
                    icon = R.drawable.ic_practice,
                    description = stringResource(R.string.home_practice_description),
                    action = stringResource(R.string.action_start),
                    style = ActionStyle.SECONDARY,
                    onClick = { onStartPractice(ai.tireon.core.mission.PracticeMission.SMOOTH_START) },
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(TireonDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
        ) {
            Greeting()
            ScoreCard(state = state, modifier = Modifier.fillMaxWidth())
            RecommendCard(state = state, onStartPractice = onStartPractice)
            TireonActionButton(
                text = stringResource(R.string.driving_session_start),
                onClick = onStartDriving,
                icon = R.drawable.ic_car,
                modifier = Modifier.fillMaxWidth(),
            )
            TireonActionButton(
                text = stringResource(R.string.action_start),
                onClick = { onStartPractice(ai.tireon.core.mission.PracticeMission.SMOOTH_START) },
                icon = R.drawable.ic_practice,
                style = ActionStyle.SECONDARY,
                modifier = Modifier.fillMaxWidth(),
            )
            VehicleCard(driving = driving, onRefresh = onRefresh)
        }
    }
}

@Composable
private fun Greeting() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.home_greeting),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

/** 추천 미션이 있을 때만 보여준다. */
@Composable
private fun RecommendCard(
    state: HomeUiState,
    onStartPractice: (ai.tireon.core.mission.PracticeMission) -> Unit,
) {
    val mission = state.recommendedMission ?: return

    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.home_recommend_title),
        titleIcon = R.drawable.ic_sparkle,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(icon = MissionIcons.of(mission), size = 80.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(ai.tireon.app.practice.MissionSelection.of(mission).nameResId),
                    style = MaterialTheme.typography.titleLarge,
                )
                state.recommendReason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.home_recommend_badge),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TireonActionButton(
            text = stringResource(R.string.action_start),
            onClick = { onStartPractice(mission) },
            icon = R.drawable.ic_play,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScoreCard(state: HomeUiState, modifier: Modifier = Modifier) {
    SectionCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScoreRing(scoreText = state.scoreText)
            Text(
                text = state.statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StartCard(
    icon: Int,
    description: String,
    action: String,
    style: ActionStyle,
    onClick: () -> Unit,
) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(icon = icon)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        }
        TireonActionButton(
            text = action,
            onClick = onClick,
            icon = R.drawable.ic_play,
            style = style,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VehicleCard(driving: DrivingUiState, onRefresh: () -> Unit) {
    val unavailable = stringResource(R.string.value_unavailable)
    SectionCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.settings_vehicle_section),
        titleIcon = R.drawable.ic_car,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 단위는 SDK가 제공하지 않으므로 값을 그대로 표시한다.
            StatTile(
                label = stringResource(R.string.distance_driven),
                value = DrivingDisplay.distance(driving.distanceDriven) ?: unavailable,
                icon = R.drawable.ic_place,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.trip_driving_time),
                value = DrivingDisplay.drivingTime(driving.tripDrivingTime) ?: unavailable,
                icon = R.drawable.ic_timer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.action_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        driving.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
