package ai.tireon.app.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
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
import ai.tireon.app.ui.InfoRow
import ai.tireon.app.ui.ScreenHeader
import ai.tireon.app.ui.SectionCard
import ai.tireon.app.ui.TireonDimens

/** 설정 화면. 앱 정보, 차량 정보, 안전 고지를 보여준다. */
@Composable
fun SettingsScreen(
    info: SettingsInfo,
    modifier: Modifier = Modifier,
    onShowOnboarding: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(TireonDimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
    ) {
        ScreenHeader(title = stringResource(R.string.settings_title))

        Column(
            modifier = Modifier.widthIn(max = 840.dp),
            verticalArrangement = Arrangement.spacedBy(TireonDimens.Gap),
        ) {
            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_vehicle_section),
                titleIcon = R.drawable.ic_car,
            ) {
                InfoRow(
                    label = stringResource(R.string.settings_connection),
                    value = info.connectionText,
                )
                InfoRow(
                    label = stringResource(R.string.settings_vin),
                    value = info.vinText,
                )
            }

            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_app_section),
                titleIcon = R.drawable.ic_info,
            ) {
                InfoRow(
                    label = stringResource(R.string.settings_app_version),
                    value = info.appVersion,
                )
            }

            if (onShowOnboarding != null) {
                SectionCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onShowOnboarding,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_guide_section),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(R.string.settings_onboarding),
                                style = MaterialTheme.typography.titleMedium,
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

            SectionCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.settings_safety_section),
                titleIcon = R.drawable.ic_shield,
            ) {
                SettingsInfo.safetyNotices().forEach { notice ->
                    Text(
                        text = "• $notice",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
