import Shared
import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Antes que la pantalla: iOS también abre la app sin ella, para un botón de la Live Activity.
        MainViewControllerKt.setUp(liveActivity: FocusActivities())
        FocusActionIntent.handler = { action in
            try? await MainViewControllerKt.perform(action: action)
        }
    }

    var body: some Scene {
        WindowGroup {
            // Compose se ocupa de las zonas seguras (la isla, la barra de inicio), como en Android.
            ComposeView().ignoresSafeArea()
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
