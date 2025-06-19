import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    
    private let stateEngine: TrackerStateEngine = KoinHelper().stateEngine
    
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(stateEngine: stateEngine)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    
    var body: some View {
        ComposeView().ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}




