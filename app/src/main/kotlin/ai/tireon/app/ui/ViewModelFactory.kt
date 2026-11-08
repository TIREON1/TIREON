package ai.tireon.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import ai.tireon.app.driving.DrivingViewModel

/** 화면 생명주기에 묶인 [DrivingViewModel] 팩토리를 만든다. */
@Composable
fun viewModelFactory(): ViewModelProvider.Factory {
    val context = LocalContext.current
    return remember(context) { DrivingViewModel.Factory(context) }
}
