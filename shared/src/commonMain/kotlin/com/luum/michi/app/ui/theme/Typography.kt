@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package com.luum.michi.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import org.jetbrains.compose.resources.ResourceItem

/**
 * App typography: bundled font catalog ([AppFont], so Android and iOS render
 * identical), with glass-flavoured tweaks — semibold titles with tightened
 * tracking, medium labels slightly opened. Every style carries the family
 * explicitly (M3 [Typography] has no defaultFontFamily), so call sites keep
 * using `typography.*` and inherit the look for free.
 */

private const val MD: String = "composeResources/com.luum.michi.app.resources/"

private fun font(name: String): FontResource = FontResource(
    "font:$name",
    setOf(ResourceItem(setOf(), "${MD}font/$name.ttf", -1, -1)),
)

/**
 * Hand-written mirror of the generated `Res.font.*` accessors (same ids,
 * same packaged paths) — under `androidMultiplatformLibrary` the generated
 * accessors never reach the Android compilation (icons avoid this entirely:
 * they are Valkyrie-generated `ImageVector` code, no resource system).
 * Swapping back is mechanical if wiring is fixed.
 */
internal object LocalFonts {
    val regular: FontResource by lazy { font("plus_jakarta_sans_regular") }
    val medium: FontResource by lazy { font("plus_jakarta_sans_medium") }
    val semibold: FontResource by lazy { font("plus_jakarta_sans_semibold") }
    val bold: FontResource by lazy { font("plus_jakarta_sans_bold") }
    val manropeRegular: FontResource by lazy { font("manrope_regular") }
    val manropeMedium: FontResource by lazy { font("manrope_medium") }
    val manropeSemibold: FontResource by lazy { font("manrope_semibold") }
    val manropeBold: FontResource by lazy { font("manrope_bold") }
    val nunitoSansRegular: FontResource by lazy { font("nunito_sans_regular") }
    val nunitoSansMedium: FontResource by lazy { font("nunito_sans_medium") }
    val nunitoSansSemibold: FontResource by lazy { font("nunito_sans_semibold") }
    val nunitoSansBold: FontResource by lazy { font("nunito_sans_bold") }
}

/** Bundled font catalog. Display names travel raw (proper nouns, like palette names): not copy. */
internal enum class AppFont {
    JAKARTA,
    MANROPE,
    NUNITO_SANS,
}

internal val AppFont.displayName: String
    get() = when (this) {
        AppFont.JAKARTA -> "Plus Jakarta Sans"
        AppFont.MANROPE -> "Manrope"
        AppFont.NUNITO_SANS -> "Nunito Sans"
    }

@Composable
internal fun jakartaFontFamily(): FontFamily = FontFamily(
    Font(LocalFonts.regular, FontWeight.Normal),
    Font(LocalFonts.medium, FontWeight.Medium),
    Font(LocalFonts.semibold, FontWeight.SemiBold),
    Font(LocalFonts.bold, FontWeight.Bold),
)

@Composable
internal fun manropeFontFamily(): FontFamily = FontFamily(
    Font(LocalFonts.manropeRegular, FontWeight.Normal),
    Font(LocalFonts.manropeMedium, FontWeight.Medium),
    Font(LocalFonts.manropeSemibold, FontWeight.SemiBold),
    Font(LocalFonts.manropeBold, FontWeight.Bold),
)

@Composable
internal fun nunitoSansFontFamily(): FontFamily = FontFamily(
    Font(LocalFonts.nunitoSansRegular, FontWeight.Normal),
    Font(LocalFonts.nunitoSansMedium, FontWeight.Medium),
    Font(LocalFonts.nunitoSansSemibold, FontWeight.SemiBold),
    Font(LocalFonts.nunitoSansBold, FontWeight.Bold),
)

@Composable
internal fun fontFamilyFor(font: AppFont): FontFamily = when (font) {
    AppFont.JAKARTA -> jakartaFontFamily()
    AppFont.MANROPE -> manropeFontFamily()
    AppFont.NUNITO_SANS -> nunitoSansFontFamily()
}

private fun TextStyle.jakarta(
    family: FontFamily,
    weight: FontWeight? = null,
    tracking: TextUnit? = null,
): TextStyle = copy(
    fontFamily = family,
    fontWeight = weight ?: fontWeight,
    letterSpacing = tracking ?: letterSpacing,
)

@Composable
internal fun appTypography(font: AppFont = AppFont.JAKARTA): Typography {
    val family = fontFamilyFor(font)
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.jakarta(family),
        displayMedium = base.displayMedium.jakarta(family),
        displaySmall = base.displaySmall.jakarta(family),
        headlineLarge = base.headlineLarge.jakarta(family),
        headlineMedium = base.headlineMedium.jakarta(
            family,
            weight = FontWeight.SemiBold,
            tracking = (-0.25).sp,
        ),
        headlineSmall = base.headlineSmall.jakarta(
            family,
            weight = FontWeight.SemiBold,
            tracking = (-0.2).sp,
        ),
        titleLarge = base.titleLarge.jakarta(
            family,
            weight = FontWeight.SemiBold,
            tracking = (-0.2).sp,
        ),
        titleMedium = base.titleMedium.jakarta(family, weight = FontWeight.Medium),
        titleSmall = base.titleSmall.jakarta(family, weight = FontWeight.Medium),
        bodyLarge = base.bodyLarge.jakarta(family),
        bodyMedium = base.bodyMedium.jakarta(family),
        bodySmall = base.bodySmall.jakarta(family),
        labelLarge = base.labelLarge.jakarta(
            family,
            weight = FontWeight.Medium,
            tracking = 0.4.sp,
        ),
        labelMedium = base.labelMedium.jakarta(
            family,
            weight = FontWeight.Medium,
            tracking = 0.5.sp,
        ),
        labelSmall = base.labelSmall.jakarta(
            family,
            weight = FontWeight.Medium,
            tracking = 0.5.sp,
        ),
    )
}
