package com.nuvio.tv.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import com.nuvio.tv.R

/**
 * The Ink theme (this fork only): ink's look (~/src/ink/CLAUDE.md, "The look"; tokens from ink's
 * Theme.kt) as one more choice in Settings > Appearance. Arc's own themes are untouched; Arc's files
 * only hook in here, so the patch stays small across Arc's daily updates.
 *
 * Black, warm white type, 1px hairlines at 12%. Pictures are the only colour. Two faces: Archivo
 * Black for titles, Silkscreen for small words (never under 12 sp). Long paragraphs keep Inter
 * (the one bend of "two fonts", for reading from the couch). Selection is brightness: 70% for
 * anything read, 35% only for what's off. One curve, ease-out-expo; no bounce.
 */
object InkTheme {
    val ink = Color(0xFFF1F0EC)
    val black = Color.Black
    val plate = Color(0xFF141414)
    val mid = ink.copy(alpha = .70f)   // secondary, anything read
    val dim = ink.copy(alpha = .35f)   // off / unselected only
    val line = ink.copy(alpha = .12f)  // hairlines

    val palette = ThemeColorPalette(
        secondary = ink,
        secondaryVariant = mid,
        onSecondary = black,
        onSecondaryVariant = black,
        focusRing = ink,
        focusBackground = plate,
        background = black,
        backgroundElevated = plate,
        backgroundCard = plate,
        surface = plate,
        surfaceVariant = plate,
        panel = black,
        overlay = Color(0xD9000000),
        field = plate,
        menu = plate,
        modal = black,
        playerOverlay = Color(0xCC000000),
    )

    // Every weight maps to the one face, so Compose never fakes a bold onto them.
    private fun only(res: Int) = FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.Black)
            .map { Font(res, it) }
    )
    // Archivo Black is always UPPERCASE in ink: this copy draws lowercase as capitals
    // (fork/make_caps_font.py), so headings need no per-screen uppercase().
    val Archivo = only(R.font.archivo_black_caps)
    val Pixel = only(R.font.silkscreen)

    private val tight = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

    private fun headline(s: TextStyle) = s.copy(
        fontFamily = Archivo, fontWeight = FontWeight.Normal,
        letterSpacing = (-0.02).em, lineHeight = 1.0.em, lineHeightStyle = tight,
    )

    private fun pixel(s: TextStyle) = s.copy(
        fontFamily = Pixel, fontWeight = FontWeight.Normal,
        fontSize = readable(s.fontSize), letterSpacing = 0.sp, lineHeight = 1.4.em,
    )

    /** Silkscreen's pixels blur together under 12 sp: 12 at least, the sizes just over it a step up. */
    private fun readable(size: TextUnit): TextUnit = when {
        !size.isSp -> 12.sp
        size.value < 12f -> 12.sp
        size.value < 14f -> (size.value + 1f).sp
        else -> size
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    fun typography(): Typography {
        val base = buildNuvioTypography(InterFamily)
        return base.copy(
            displayLarge = headline(base.displayLarge),
            displayMedium = headline(base.displayMedium),
            displaySmall = headline(base.displaySmall),
            headlineLarge = headline(base.headlineLarge),
            headlineMedium = headline(base.headlineMedium),
            headlineSmall = headline(base.headlineSmall),
            titleLarge = headline(base.titleLarge),
            titleMedium = headline(base.titleMedium),
            titleSmall = pixel(base.titleSmall),
            labelLarge = pixel(base.labelLarge),
            labelMedium = pixel(base.labelMedium),
            labelSmall = pixel(base.labelSmall),
            // body* stay Inter: paragraphs.
        )
    }

    /** ink's one curve (MOTION.md): ease-out-expo, enter 220-280 ms, exit 140-150 ms. */
    val expo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    val motion = NuvioMotionTokens(
        durations = NuvioMotionDurations(
            instant = 0,
            quick = 145,
            fast = 220,
            medium = 240,
            slow = 280,
            overlay = 240,
            sidebarLabelIn = 220,
            sidebarLabelOut = 145,
            sidebarPanelIn = 240,
            sidebarPanelOut = 145,
            sidebarBloomOut = 145,
            sidebarEnter = 240,
            sidebarExit = 145,
            hero = 280,
            shimmer = 1200,
        ),
        easings = NuvioMotionEasings(standard = expo, emphasized = expo, decelerate = expo, accelerate = expo),
        focusScale = 1f,
        selectedScale = 1f,
        pressedScale = 1f,
        reducedMotionScale = 1f,
    )

    /** Set by NuvioTheme; NuvioMotion is a global object, so it reads this to pick ink's motion. */
    @Volatile
    var active: Boolean = false

    /**
     * The playing title's accent (from its backdrop), for the player's progress line: ink keeps the
     * picture's colour only there. Set by the player chrome, read by Arc's ProgressBar.
     */
    val progressAccent = androidx.compose.runtime.mutableStateOf(ink)
}
