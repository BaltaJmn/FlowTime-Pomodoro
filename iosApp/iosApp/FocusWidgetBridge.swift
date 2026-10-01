import Foundation
import Shared
import WidgetKit

/// Guarda lo que decide Kotlin (HomeWidget.kt) para el widget y le pide que se vuelva a dibujar.
final class FocusWidgetBridge: NSObject, HomeWidget {
    func update(
        status: String,
        streak: String?,
        countsDown: Bool,
        date: KotlinDouble?,
        time: String,
        primary: String,
        primaryLabel: String,
        stopLabel: String?,
        goal: String?,
        goalFraction: Float
    ) {
        FocusWidgetState(
            status: status,
            streak: streak,
            countsDown: countsDown,
            date: date.map { Date(timeIntervalSince1970: $0.doubleValue) },
            time: time,
            primary: primary,
            primaryLabel: primaryLabel,
            stopLabel: stopLabel,
            goal: goal,
            goalFraction: Double(goalFraction)
        ).save()
        WidgetCenter.shared.reloadAllTimelines()
    }
}
