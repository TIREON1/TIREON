package ai.tireon.app.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * 차속 권한을 요청하고 결과를 상태로 알려준다.
 *
 * 승인 없이는 속도가 null이 되어 급가속·급제동 분석이 불가능하다. 화면은 이 상태를
 * 보고 안내 문구를 표시한다.
 *
 * @return 승인 여부. 확인 중이면 null.
 */
@Composable
fun rememberSpeedPermissionState(): Boolean? {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasSpeedPermission()) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        if (!result) {
            Log.i(TAG, "차속 권한이 거부되었습니다. 속도 신호를 사용할 수 없습니다.")
        }
    }

    LaunchedEffect(Unit) {
        if (granted) return@LaunchedEffect

        val decision = SpeedPermission.decide(
            granted = false,
            shouldShowRationale = false,
        )
        if (decision == SpeedPermission.Decision.REQUEST) {
            launcher.launch(SpeedPermission.REQUIRED.first())
        }
    }

    return granted
}

private fun Context.hasSpeedPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, SPEED_PERMISSION) == PackageManager.PERMISSION_GRANTED

private const val TAG = "SpeedPermission"
private const val SPEED_PERMISSION = "android.car.permission.CAR_SPEED"
