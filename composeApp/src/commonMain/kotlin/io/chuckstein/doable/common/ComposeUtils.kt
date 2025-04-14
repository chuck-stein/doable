package io.chuckstein.doable.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity

@Composable
fun isKeyboardVisible(): Boolean {
    val density = LocalDensity.current
    val keyboard = WindowInsets.ime
    var openKeyboardBottomInset by remember { mutableStateOf(0) }
    val currentKeyboardBottomInset by derivedStateOf { keyboard.getBottom(density) }
    val keyboardIsVisible = currentKeyboardBottomInset > openKeyboardBottomInset / 2

    if (currentKeyboardBottomInset > openKeyboardBottomInset) {
        LaunchedEffect(Unit) {
            openKeyboardBottomInset = currentKeyboardBottomInset
        }
    }
    return keyboardIsVisible
}