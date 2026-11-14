package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.viewmodel.AppThemeMode

data class IndoFuturismPalette(
    val isDark: Boolean,
    val isEInk: Boolean,
    val baseBg: Color,
    val cardSurface: Color,
    val elevatedSurface: Color,
    val highlightSurface: Color,
    val secondaryIndigo: Color,
    val secondaryCopper: Color,
    val secondaryTerracotta: Color,
    val secondarySlate: Color,
    val goldHairline: Color,
    val watermarkTint: Color,
    val accentSaffron: Color,
    val accentCyan: Color,
    val accentGold: Color,
    val accentVermilion: Color
)

val LocalIndoPalette = staticCompositionLocalOf {
    IndoFuturismPalette(
        isDark = false,
        isEInk = false,
        baseBg = SandstoneBase,
        cardSurface = AlabasterSurface,
        elevatedSurface = HandloomVariant,
        highlightSurface = ParchmentElevated,
        secondaryIndigo = IndigoStructureLight,
        secondaryCopper = CopperStructureLight,
        secondaryTerracotta = TerracottaStructureLight,
        secondarySlate = SlateStructureLight,
        goldHairline = MoltenGoldHairlineLight,
        watermarkTint = Color(0xFF78350F).copy(alpha = 0.05f),
        accentSaffron = SaffronAmberDeep,
        accentCyan = OxidizedTeal,
        accentGold = Color(0xFFB48811),
        accentVermilion = VermilionAccent
    )
}

private val IndoDarkColorScheme = darkColorScheme(
    primary = SaffronAmber,
    onPrimary = ObsidianBase,
    primaryContainer = TerracottaStructureDark,
    onPrimaryContainer = TextIvoryDark,

    secondary = ElectricCyan,
    onSecondary = ObsidianBase,
    secondaryContainer = CopperStructureDark,
    onSecondaryContainer = TextIvoryDark,

    tertiary = VermilionAccent,
    onTertiary = TextIvoryDark,
    tertiaryContainer = IndigoStructureDark,
    onTertiaryContainer = TextIvoryDark,

    background = ObsidianBase,
    onBackground = TextIvoryDark,

    surface = BasaltSurface,
    onSurface = TextIvoryDark,
    surfaceVariant = BasaltElevated,
    onSurfaceVariant = TextSlateDark,
    outline = MoltenGoldHairline,
    outlineVariant = Color(0xFF2B3242),

    error = VermilionAccent,
    onError = TextIvoryDark
)

private val IndoLightColorScheme = lightColorScheme(
    primary = SaffronAmberDeep,
    onPrimary = AlabasterSurface,
    primaryContainer = TerracottaStructureLight,
    onPrimaryContainer = TextInkLight,

    secondary = OxidizedTeal,
    onSecondary = AlabasterSurface,
    secondaryContainer = CopperStructureLight,
    onSecondaryContainer = TextInkLight,

    tertiary = VermilionAccent,
    onTertiary = AlabasterSurface,
    tertiaryContainer = IndigoStructureLight,
    onTertiaryContainer = TextInkLight,

    background = SandstoneBase,
    onBackground = TextInkLight,

    surface = AlabasterSurface,
    onSurface = TextInkLight,
    surfaceVariant = HandloomVariant,
    onSurfaceVariant = TextStoneLight,
    outline = MoltenGoldHairlineLight,
    outlineVariant = ParchmentElevated,

    error = VermilionAccent,
    onError = AlabasterSurface
)

private val EInkColorScheme = lightColorScheme(
    primary = EInkInkPrimary,
    onPrimary = EInkSurface,
    primaryContainer = EInkStoneVariant,
    onPrimaryContainer = EInkInkPrimary,

    secondary = EInkInkSecondary,
    onSecondary = EInkSurface,
    secondaryContainer = EInkStoneVariant,
    onSecondaryContainer = EInkInkPrimary,

    tertiary = EInkInkSecondary,
    onTertiary = EInkSurface,
    tertiaryContainer = EInkStoneVariant,
    onTertiaryContainer = EInkInkPrimary,

    background = EInkPaperBase,
    onBackground = EInkInkPrimary,

    surface = EInkSurface,
    onSurface = EInkInkPrimary,
    surfaceVariant = EInkStoneVariant,
    onSurfaceVariant = EInkInkSecondary,
    outline = Color(0x33171717),
    outlineVariant = EInkStoneVariant,

    error = EInkInkPrimary,
    onError = EInkSurface
)

@Composable
fun EduPulseTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        AppThemeMode.DARK -> IndoDarkColorScheme
        AppThemeMode.LIGHT -> IndoLightColorScheme
        AppThemeMode.EINK -> EInkColorScheme
    }

    val indoPalette = when (themeMode) {
        AppThemeMode.DARK -> IndoFuturismPalette(
            isDark = true,
            isEInk = false,
            baseBg = ObsidianBase,
            cardSurface = BasaltSurface,
            elevatedSurface = BasaltElevated,
            highlightSurface = BasaltHighlight,
            secondaryIndigo = IndigoStructureDark,
            secondaryCopper = CopperStructureDark,
            secondaryTerracotta = TerracottaStructureDark,
            secondarySlate = SlateStructureDark,
            goldHairline = MoltenGoldHairline,
            watermarkTint = MoltenGold.copy(alpha = 0.055f),
            accentSaffron = SaffronAmber,
            accentCyan = ElectricCyan,
            accentGold = MoltenGold,
            accentVermilion = VermilionAccent
        )
        AppThemeMode.LIGHT -> IndoFuturismPalette(
            isDark = false,
            isEInk = false,
            baseBg = SandstoneBase,
            cardSurface = AlabasterSurface,
            elevatedSurface = HandloomVariant,
            highlightSurface = ParchmentElevated,
            secondaryIndigo = IndigoStructureLight,
            secondaryCopper = CopperStructureLight,
            secondaryTerracotta = TerracottaStructureLight,
            secondarySlate = SlateStructureLight,
            goldHairline = MoltenGoldHairlineLight,
            watermarkTint = Color(0xFF78350F).copy(alpha = 0.055f),
            accentSaffron = SaffronAmberDeep,
            accentCyan = OxidizedTeal,
            accentGold = Color(0xFFB48811),
            accentVermilion = VermilionAccent
        )
        AppThemeMode.EINK -> IndoFuturismPalette(
            isDark = false,
            isEInk = true,
            baseBg = EInkPaperBase,
            cardSurface = EInkSurface,
            elevatedSurface = EInkStoneVariant,
            highlightSurface = EInkHighlight,
            secondaryIndigo = EInkStoneVariant,
            secondaryCopper = EInkStoneVariant,
            secondaryTerracotta = EInkStoneVariant,
            secondarySlate = EInkStoneVariant,
            goldHairline = Color(0x33171717),
            watermarkTint = Color.Transparent,
            accentSaffron = EInkInkPrimary,
            accentCyan = EInkInkSecondary,
            accentGold = EInkInkPrimary,
            accentVermilion = EInkInkPrimary
        )
    }

    CompositionLocalProvider(LocalIndoPalette provides indoPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EduPulseTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        content = content
    )
}
