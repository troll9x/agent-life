package vn.lifeos.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF315D64),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E9E9),
    secondary = Color(0xFF5A6450),
    background = Color(0xFFF8FAF7),
    surface = Color(0xFFF8FAF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFACD0D1),
    onPrimary = Color(0xFF17373C),
    primaryContainer = Color(0xFF24494F),
    secondary = Color(0xFFC1CFB3),
    background = Color(0xFF101918),
    surface = Color(0xFF101918),
)

@Composable
internal fun AgentLifeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
