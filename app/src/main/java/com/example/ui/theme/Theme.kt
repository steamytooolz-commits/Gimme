package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.data.model.GamePhase

private val InvestigationColorScheme = darkColorScheme(
    primary = AmberAccent,
    onPrimary = DarkTerminalBg,
    secondary = CrimsonAccent,
    onSecondary = TerminalOnBg,
    background = DarkTerminalBg,
    onBackground = TerminalOnBg,
    surface = DarkTerminalSurface,
    onSurface = TerminalOnBg,
    error = CrimsonAccent,
    onError = TerminalOnBg
)

private val CourtroomColorScheme = lightColorScheme(
    primary = WarmWoodBrown,
    onPrimary = ParchmentBg,
    secondary = AntiqueBrassGold,
    onSecondary = LegalOnBg,
    background = ParchmentBg,
    onBackground = LegalOnBg,
    surface = CourtSurface,
    onSurface = LegalOnBg,
    error = LegalRed,
    onError = ParchmentBg
)

@Composable
fun ThemisTheme(
    phase: GamePhase,
    content: @Composable () -> Unit
) {
    val colorScheme = if (phase == GamePhase.INVESTIGATION) {
        InvestigationColorScheme
    } else {
        CourtroomColorScheme
    }

    val typography = if (phase == GamePhase.INVESTIGATION) {
        InvestigationTypography
    } else {
        CourtroomTypography
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
