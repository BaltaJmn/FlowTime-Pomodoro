import ActivityKit
import Foundation

/// La Live Activity de la sesión. La app la pide (FocusActivities) y la extensión la dibuja
/// (FlowTimeWidgets): este fichero está en los dos targets. Los textos llegan ya traducidos de Kotlin.
struct FocusAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        /// Trabajando, descansando o en pausa.
        var title: String
        /// El modo y la etiqueta: "Pomodoro · Estudio".
        var subtitle: String
        var task: String?
        var countsDown: Bool
        /// En marcha: cuándo acaba la fase si cuenta hacia atrás, o cuándo empezó si no. En pausa, nil.
        var date: Date?
        /// El tiempo parado, en pausa.
        var time: String
        /// Los botones: el nombre de cada TimerAction (FocusActionIntent) y lo que dice.
        var actions: [String]
        var labels: [String]
    }
}
