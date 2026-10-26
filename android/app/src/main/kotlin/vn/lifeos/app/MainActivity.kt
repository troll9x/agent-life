package vn.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val shellViewModel: ShellViewModel by viewModels {
        ShellViewModel.factory((application as AgentLifeApplication).appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by shellViewModel.state.collectAsStateWithLifecycle()
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !state.darkTheme
                    isAppearanceLightNavigationBars = !state.darkTheme
                }
            }
            AgentLifeTheme(darkTheme = state.darkTheme) {
                AgentLifeScreen(
                    state = state,
                    onRetry = shellViewModel::retry,
                    onThemeChange = shellViewModel::setDarkTheme,
                    onCompleteOnboarding = shellViewModel::completeOnboarding,
                    onUpdateProfile = shellViewModel::updateProfile,
                )
            }
        }
    }
}
