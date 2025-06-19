package io.chuckstein.doable

import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.chuckstein.doable.tracker.TrackerScreen
import io.chuckstein.doable.tracker.TrackerStateEngine
import kotlinx.coroutines.CoroutineScope

fun MainViewController(stateEngine: TrackerStateEngine, scope: CoroutineScope) = ComposeUIViewController {

    val uiState by stateEngine.uiStateFlow(scope).collectAsStateWithLifecycle()

    TrackerScreen(uiState) { event ->
        stateEngine.processEvent(event, scope)
    }
}