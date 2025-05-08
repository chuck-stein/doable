package io.chuckstein.doable.common

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Presentation-layer representation of color that will be displayed on the UI.
 * Call [resolve] to convert to a [Color].
 */
sealed interface ColorModel {
    data class Value(val color: Color) : ColorModel
    data class FromTheme(val resolveColor: ColorScheme.() -> Color) : ColorModel
}

@Composable
fun ColorModel.resolve(): Color = when (this) {
    is ColorModel.Value -> color
    is ColorModel.FromTheme -> MaterialTheme.colorScheme.resolveColor()
}