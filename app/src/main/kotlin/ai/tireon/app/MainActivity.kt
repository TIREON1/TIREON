package ai.tireon.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.tireon.app.permission.SpeedPermission
import ai.tireon.app.permission.rememberSpeedPermissionState
import ai.tireon.app.ui.TireonApp
import ai.tireon.app.ui.theme.TireonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TireonTheme {
                TireonRoot()
            }
        }
    }
}

/**
 * 권한 상태에 따라 앱 또는 안내를 보여준다.
 *
 * 차속 권한이 없으면 속도가 null이 되어 핵심 분석이 불가능하다. 조용히 진행하면
 * 사용자는 왜 점수가 안 나오는지 알 수 없으므로 안내를 먼저 보여준다.
 */
@Composable
private fun TireonRoot() {
    val speedGranted = rememberSpeedPermissionState()

    when (speedGranted) {
        true -> TireonApp()
        false -> PermissionRequired()
        null -> Unit
    }
}

@Composable
private fun PermissionRequired() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "권한이 필요해요",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = SpeedPermission.DENIED_MESSAGE,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "설정에서 차량 속도 권한을 허용한 뒤 앱을 다시 실행해 주세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
