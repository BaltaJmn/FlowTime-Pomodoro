import Foundation

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

    private static let defaults = UserDefaults(suiteName: "group.com.baltajmn.flowtime")
    private static let key = "widget"

    static func load() -> FocusWidgetState? {
        defaults?.data(forKey: key).flatMap { try? JSONDecoder().decode(FocusWidgetState.self, from: $0) }
    }

    func save() {
        Self.defaults?.set(try? JSONEncoder().encode(self), forKey: Self.key)
    }
}
