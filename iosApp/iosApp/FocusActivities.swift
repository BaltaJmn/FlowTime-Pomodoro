import ActivityKit
import Foundation
import Shared

/// Pide, actualiza y termina la Live Activity de la sesión: Kotlin decide qué enseña
/// (SessionActivity.kt) y ActivityKit solo existe en Swift.
final class FocusActivities: NSObject, LiveActivity {
    func show(
        title: String,
        subtitle: String,
        task: String?,
        countsDown: Bool,
        date: KotlinDouble?,
        time: String,
        actions: [String],
        labels: [String]
    ) {
        let date = date.map { Date(timeIntervalSince1970: $0.doubleValue) }
        let state = FocusAttributes.ContentState(
            title: title, subtitle: subtitle, task: task, countsDown: countsDown, date: date, time: time,
            actions: actions, labels: labels
        )
        // Pasado el final de la fase, el sistema la marca como antigua hasta que la app la actualiza.
        let content = ActivityContent(state: state, staleDate: countsDown ? date : nil)
        if let activity = Activity<FocusAttributes>.activities.first {
            Task { await activity.update(content) }
        } else {
            // Falla si el usuario las ha apagado en Ajustes: la app sigue igual.
            _ = try? Activity.request(attributes: FocusAttributes(), content: content)
        }
    }

    func end() {
        for activity in Activity<FocusAttributes>.activities {
            Task { await activity.end(nil, dismissalPolicy: .immediate) }
        }
    }
}
