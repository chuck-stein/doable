import Foundation
import Combine
import ComposeApp

@MainActor
class TrackerViewModel: ObservableObject {

    let stateEngine: TrackerStateEngine = KoinHelper().stateEngine
    let viewModelScope = ViewModelScope()
    
    @Published var uiState: TrackerUiState

    init() {
        self.uiState = stateEngine.currentUiState()
        
        processEvent(TrackerEventInitializeTracker())

        // this and the @Published uiState is not actually used anymore
        stateEngine.onUiStateChange(scope: viewModelScope) { [weak self] newState in
            self?.uiState = newState
        }
    }

    func processEvent(_ event: TrackerEvent) {
        stateEngine.processEvent(event: event, scope: viewModelScope)
    }

    deinit {
        viewModelScope.cancel()
    }
}
