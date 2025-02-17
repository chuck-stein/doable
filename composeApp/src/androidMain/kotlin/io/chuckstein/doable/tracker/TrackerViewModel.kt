package io.chuckstein.doable.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.chuckstein.doable.tracker.TrackerEvent.InitializeTracker

class TrackerViewModel(private val stateEngine: TrackerStateEngine) : ViewModel() {

    init {
        processEvent(InitializeTracker)
    }

    val uiState = stateEngine.collectUiState(viewModelScope)

    fun processEvent(event: TrackerEvent) {
        stateEngine.processEvent(event, viewModelScope)
    }
}