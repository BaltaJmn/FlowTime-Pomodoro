import Shared
import SwiftUI

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    init() {
        // Antes que la pantalla: iOS también abre la app sin ella, para un botón de la Live Activity o
        // del widget.
        MainViewControllerKt.setUp(liveActivity: FocusActivities(), homeWidget: FocusWidgetBridge())
        FocusActionIntent.handler = { action in
            try? await MainViewControllerKt.perform(action: action)
        }
    }

    var body: some Scene {
        WindowGroup {
            // Compose se ocupa de las zonas seguras (la isla, la barra de inicio), como en Android.
            ComposeView().ignoresSafeArea()
                // Coop: la vuelta del inicio de sesión y los enlaces universales de amistad y de sala.
                .onOpenURL { url in MainViewControllerKt.openUrl(url: url.absoluteString) }
        }
    }
}

/// Coop (docs/coop-push.md): el token de APNs para los avisos con la app cerrada. Sin permiso de
/// notificaciones iOS da el token igual y no enseña nada.
class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        #if DEBUG
        MainViewControllerKt.setPushToken(token: token, sandbox: true)
        #else
        MainViewControllerKt.setPushToken(token: token, sandbox: false)
        #endif
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
