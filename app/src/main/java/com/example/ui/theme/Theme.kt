package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
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

private val CyberpunkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF), // Neon Cyan
    onPrimary = Color(0xFF070913),
    secondary = Color(0xFFFF007F), // Neon Magenta
    onSecondary = Color(0xFFE1F5FE),
    background = Color(0xFF070913),
    onBackground = Color(0xFFE1F5FE),
    surface = Color(0xFF10132B),
    onSurface = Color(0xFFE1F5FE),
    error = Color(0xFFFF007F),
    onError = Color(0xFF070913)
)

private val RoyalCourtColorScheme = darkColorScheme(
    primary = Color(0xFFF1D483), // Imperial Gold
    onPrimary = Color(0xFF0B192C), // Royal Navy Dark
    secondary = Color(0xFF9B3922), // Crimson Law Accent
    onSecondary = Color(0xFFF5F7F8),
    background = Color(0xFF0B192C),
    onBackground = Color(0xFFF5F7F8),
    surface = Color(0xFF1E3E62),
    onSurface = Color(0xFFF5F7F8),
    error = Color(0xFF9B3922),
    onError = Color(0xFFF5F7F8)
)

private fun scaleTextUnit(unit: TextUnit, multiplier: Float): TextUnit {
    return if (unit == TextUnit.Unspecified) unit else unit * multiplier
}

private fun Typography.scale(multiplier: Float): Typography {
    if (multiplier == 1.0f) return this
    return Typography(
        displayLarge = displayLarge.copy(fontSize = scaleTextUnit(displayLarge.fontSize, multiplier), lineHeight = scaleTextUnit(displayLarge.lineHeight, multiplier)),
        displayMedium = displayMedium.copy(fontSize = scaleTextUnit(displayMedium.fontSize, multiplier), lineHeight = scaleTextUnit(displayMedium.lineHeight, multiplier)),
        displaySmall = displaySmall.copy(fontSize = scaleTextUnit(displaySmall.fontSize, multiplier), lineHeight = scaleTextUnit(displaySmall.lineHeight, multiplier)),
        headlineLarge = headlineLarge.copy(fontSize = scaleTextUnit(headlineLarge.fontSize, multiplier), lineHeight = scaleTextUnit(headlineLarge.lineHeight, multiplier)),
        headlineMedium = headlineMedium.copy(fontSize = scaleTextUnit(headlineMedium.fontSize, multiplier), lineHeight = scaleTextUnit(headlineMedium.lineHeight, multiplier)),
        headlineSmall = headlineSmall.copy(fontSize = scaleTextUnit(headlineSmall.fontSize, multiplier), lineHeight = scaleTextUnit(headlineSmall.lineHeight, multiplier)),
        titleLarge = titleLarge.copy(fontSize = scaleTextUnit(titleLarge.fontSize, multiplier), lineHeight = scaleTextUnit(titleLarge.lineHeight, multiplier)),
        titleMedium = titleMedium.copy(fontSize = scaleTextUnit(titleMedium.fontSize, multiplier), lineHeight = scaleTextUnit(titleMedium.lineHeight, multiplier)),
        titleSmall = titleSmall.copy(fontSize = scaleTextUnit(titleSmall.fontSize, multiplier), lineHeight = scaleTextUnit(titleSmall.lineHeight, multiplier)),
        bodyLarge = bodyLarge.copy(fontSize = scaleTextUnit(bodyLarge.fontSize, multiplier), lineHeight = scaleTextUnit(bodyLarge.lineHeight, multiplier)),
        bodyMedium = bodyMedium.copy(fontSize = scaleTextUnit(bodyMedium.fontSize, multiplier), lineHeight = scaleTextUnit(bodyMedium.lineHeight, multiplier)),
        bodySmall = bodySmall.copy(fontSize = scaleTextUnit(bodySmall.fontSize, multiplier), lineHeight = scaleTextUnit(bodySmall.lineHeight, multiplier)),
        labelLarge = labelLarge.copy(fontSize = scaleTextUnit(labelLarge.fontSize, multiplier), lineHeight = scaleTextUnit(labelLarge.lineHeight, multiplier)),
        labelMedium = labelMedium.copy(fontSize = scaleTextUnit(labelMedium.fontSize, multiplier), lineHeight = scaleTextUnit(labelMedium.lineHeight, multiplier)),
        labelSmall = labelSmall.copy(fontSize = scaleTextUnit(labelSmall.fontSize, multiplier), lineHeight = scaleTextUnit(labelSmall.lineHeight, multiplier))
    )
}

@Composable
fun ThemisTheme(
    phase: GamePhase,
    fontSizeMultiplier: Float = 1.0f,
    customTheme: String = "Auto",
    content: @Composable () -> Unit
) {
    val colorScheme = when (customTheme) {
        "Classic Terminal" -> InvestigationColorScheme
        "Warm Parchment" -> CourtroomColorScheme
        "Cyberpunk Neon" -> CyberpunkColorScheme
        "Royal Court" -> RoyalCourtColorScheme
        else -> {
            if (phase == GamePhase.INVESTIGATION) {
                InvestigationColorScheme
            } else {
                CourtroomColorScheme
            }
        }
    }

    val baseTypography = when (customTheme) {
        "Classic Terminal", "Cyberpunk Neon" -> InvestigationTypography
        "Warm Parchment", "Royal Court" -> CourtroomTypography
        else -> {
            if (phase == GamePhase.INVESTIGATION) {
                InvestigationTypography
            } else {
                CourtroomTypography
            }
        }
    }

    val typography = baseTypography.scale(fontSizeMultiplier)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
