import Foundation
import SwiftUI

/// Lo que enseña el widget (HomeWidget.kt), guardado por la app en el App Group que comparte con la
/// extensión. Este fichero está en los dos targets.
struct FocusWidgetState: Codable {
    /// El modo, y con sesión cómo va: "Pomodoro · Trabajando".
    var status: String
    var streak: String?
    var countsDown: Bool
    /// En marcha: cuándo acaba la fase si cuenta hacia atrás, o cuándo empezó si no.
    var date: Date?
    /// Parado: el tiempo fijo; sin sesión, lo que dura el trabajo del modo.
    var time: String
    /// START, RESUME o PAUSE (FocusActionIntent).
    var primary: String
    var primaryLabel: String
    /// Con sesión, el botón de parar.
    var stopLabel: String?
    var goal: String?
    var goalFraction: Double
    /// Los colores del tema de la app, con el iPhone en claro y en oscuro. Sin ellos (lo guardado por
    /// una versión anterior), los del icono.
    var light: WidgetPalette?
    var dark: WidgetPalette?

    func palette(_ scheme: ColorScheme) -> WidgetPalette {
        (scheme == .dark ? dark : light) ?? .icon
    }

    private static let defaults = UserDefaults(suiteName: "group.com.baltajmn.flowtime")
    private static let key = "widget"

    static func load() -> FocusWidgetState? {
        defaults?.data(forKey: key).flatMap { try? JSONDecoder().decode(FocusWidgetState.self, from: $0) }
    }

    func save() {
        Self.defaults?.set(try? JSONEncoder().encode(self), forKey: Self.key)
    }
}

/// Los colores del widget en ARGB, los mismos papeles que GlanceTheme en Android (WidgetColors en
/// HomeWidget.kt).
struct WidgetPalette: Codable {
    var background: UInt32
    /// El reloj.
    var text: UInt32
    /// El modo, la racha y el objetivo.
    var secondaryText: UInt32
    /// Empezar, pausar o seguir, y la barra del objetivo.
    var primary: UInt32
    var onPrimary: UInt32
    /// Parar.
    var secondary: UInt32
    var onSecondary: UInt32
    /// El fondo de la barra del objetivo.
    var track: UInt32

    /// Los de design/icon.svg, como antes de que el widget siguiera al tema.
    static let icon = WidgetPalette(
        background: 0xFF15506A,
        text: 0xFFEAF6FB,
        secondaryText: 0xBFEAF6FB,
        primary: 0xFF8ECAE6,
        onPrimary: 0xFF15506A,
        secondary: 0xFF2B6782,
        onSecondary: 0xFF8ECAE6,
        track: 0xFF2B6782
    )
}

extension Color {
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double(argb >> 24) / 255
        )
    }
}
