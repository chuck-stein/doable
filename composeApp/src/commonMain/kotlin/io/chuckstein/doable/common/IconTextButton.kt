package io.chuckstein.doable.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.chuckstein.doable.tracker.IconButtonState

@Composable
fun IconTextButton(state: IconButtonState, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick, modifier) {
        DoableIcon(state.icon, Modifier.padding(start = 2.dp, end = 10.dp))
        Text(state.text.resolveText())
    }
}