import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    
    let viewModel: TrackerViewModel
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            stateEngine: viewModel.stateEngine,
            scope: viewModel.viewModelScope
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    
    @StateObject private var viewModel = TrackerViewModel()
    
    var body: some View {
        ComposeView(viewModel: viewModel)
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}




