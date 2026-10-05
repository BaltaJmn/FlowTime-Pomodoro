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
        goalFraction: Float,
        light: WidgetColors,
        dark: WidgetColors
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
            goalFraction: Double(goalFraction),
            light: WidgetPalette(light),
            dark: WidgetPalette(dark)
        ).save()
        WidgetCenter.shared.reloadAllTimelines()
    }
}

private extension WidgetPalette {
    /// Los Int de Kotlin llegan como Int32 con signo: el mismo ARGB, leído sin él.
    init(_ colors: WidgetColors) {
        self.init(
            background: UInt32(bitPattern: colors.background),
            text: UInt32(bitPattern: colors.text),
            secondaryText: UInt32(bitPattern: colors.secondaryText),
            primary: UInt32(bitPattern: colors.primary),
            onPrimary: UInt32(bitPattern: colors.onPrimary),
            secondary: UInt32(bitPattern: colors.secondary),
            onSecondary: UInt32(bitPattern: colors.onSecondary),
            track: UInt32(bitPattern: colors.track)
        )
    }
}
