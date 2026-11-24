package ir.ravand.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.ravand.app.ui.AdminScreen
import ir.ravand.app.ui.AppDialogs
import ir.ravand.app.ui.ErrorBox
import ir.ravand.app.ui.LoadingBox
import ir.ravand.app.ui.LoginScreen
import ir.ravand.app.ui.RavandTheme
import ir.ravand.app.ui.ToastOverlay
import ir.ravand.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.stateFlow.collectAsStateWithLifecycle()
            val settings = state.dashboard?.settings
            RavandTheme(
                themeId = state.themePreview ?: settings?.theme ?: "mint",
                fontId = state.fontPreview ?: settings?.font ?: "arad",
            ) {
                Surface(Modifier.fillMaxSize()) {
                    when {
                        state.loading -> LoadingBox()
                        state.error != null -> ErrorBox(state.error ?: "") { vm.refresh() }
                        !state.logged -> LoginScreen(vm, state)
                        else -> AdminScreen(vm, state)
                    }
                    ToastOverlay(state.toast, { vm.clearToast() })
                    AppDialogs(vm, state)
                }
            }
        }
    }
}
