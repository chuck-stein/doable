package io.chuckstein.doable.common

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import io.telereso.kmp.core.icons.resources.Circle
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

typealias Icons = io.telereso.kmp.core.icons.resources.Res.drawable

data class IconState(
    val icon: DrawableResource,
    val contentDescription: TextModel?,
    val enabled: Boolean = true
)

@Composable
fun DoableIconButton(state: IconState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    DoableIconButton(state.icon, state.contentDescription?.resolveText(), modifier, state.enabled, onClick)
}

@Composable
fun DoableIconButton(
    icon: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(onClick, modifier, enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun EmptyIconButton(modifier: Modifier = Modifier) {
    DoableIconButton(Icons.Circle, contentDescription = null, modifier.alpha(0f), enabled = false, onClick = {})
}

@Composable
fun DoableIcon(iconState: IconState, modifier: Modifier = Modifier) = with(iconState) {
    Icon(
        modifier = modifier,
        painter = painterResource(icon),
        contentDescription = contentDescription?.resolveText()
    )
}
