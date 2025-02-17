package io.chuckstein.doable.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import doable.composeapp.generated.resources.Res
import doable.composeapp.generated.resources.dm_serif_text
import doable.composeapp.generated.resources.song_myung
import org.jetbrains.compose.resources.Font

@Composable
fun HeaderFontFamily() = FontFamily(
    Font(Res.font.dm_serif_text)
)

@Composable
fun BodyFontFamily() = FontFamily(
    Font(Res.font.song_myung)
)

@Composable
fun DoableTypography() = with(Typography()) {
    val headerFontFamily = HeaderFontFamily()
    val bodyFontFamily = BodyFontFamily()

    copy(
        displayLarge = displayLarge.copy(fontFamily = headerFontFamily),
        displayMedium = displayMedium.copy(fontFamily = headerFontFamily),
        displaySmall = displaySmall.copy(fontFamily = headerFontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = headerFontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = headerFontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = headerFontFamily),
        titleLarge = titleLarge.copy(fontFamily = headerFontFamily),
        titleMedium = titleMedium.copy(fontFamily = headerFontFamily),
        titleSmall = titleSmall.copy(fontFamily = headerFontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = bodyFontFamily, fontSize = 14.sp),
        bodyMedium = bodyMedium.copy(fontFamily = bodyFontFamily, fontSize = 12.sp),
        bodySmall = bodySmall.copy(fontFamily = bodyFontFamily, fontSize = 10.sp),
        labelLarge = labelLarge.copy(fontFamily = bodyFontFamily, fontSize = 12.sp),
        labelMedium = labelMedium.copy(fontFamily = bodyFontFamily, fontSize = 10.sp),
        labelSmall = labelSmall.copy(fontFamily = bodyFontFamily, fontSize = 8.sp),
    )

}