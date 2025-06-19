package io.chuckstein.doable

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import io.chuckstein.doable.tracker.TrackerEvent.InitializeTracker
import io.chuckstein.doable.tracker.TrackerScreen
import io.chuckstein.doable.tracker.TrackerStateEngine

fun MainViewController(stateEngine: TrackerStateEngine) = ComposeUIViewController {
    val scope = rememberCoroutineScope()
    val uiState by stateEngine.uiStateFlow(scope).collectAsState()

    LaunchedEffect(Unit) {
        stateEngine.processEvent(InitializeTracker, scope)
    }

    TrackerScreen(uiState) { event ->
        stateEngine.processEvent(event, scope)
    }
}