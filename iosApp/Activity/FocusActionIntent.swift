import AppIntents

/// Un botón de la Live Activity: pausar, seguir, parar... con el nombre de su TimerAction. iOS lo
/// ejecuta en la app, abriéndola en segundo plano si hace falta, aunque se pulse en la extensión.
struct FocusActionIntent: LiveActivityIntent {
    static let title: LocalizedStringResource = "FlowTime"
    static let isDiscoverable = false

    /// Lo pone la app al arrancar (iOSApp): este fichero también está en la extensión, que no ve Kotlin.
    @MainActor static var handler: ((String) async -> Void)?

    @Parameter(title: "Action")
    var action: String

    init() {}

    init(action: String) {
        self.action = action
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        await Self.handler?(action)
        return .result()
    }
}
