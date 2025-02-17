package io.chuckstein.doable

import androidx.compose.ui.window.ComposeUIViewController
import io.chuckstein.doable.tracker.TrackerScreen

fun MainViewController() = ComposeUIViewController { TrackerScreen() }