import Shared
import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            // Compose se ocupa de las zonas seguras (la isla, la barra de inicio), como en Android.
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(liveActivity: FocusActivities())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
