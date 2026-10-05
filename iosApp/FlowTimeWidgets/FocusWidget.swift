import SwiftUI
import WidgetKit

/// El widget de la pantalla de inicio, como el de Android: el reloj y empezar, pausar o seguir; el
/// mediano, además cómo va, la racha, parar y el objetivo de hoy. Lo que enseña lo decide Kotlin
/// (HomeWidget.kt) y la app pide redibujarlo con cada cambio.
struct FocusWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "FocusWidget", provider: Provider()) { entry in
            FocusWidgetView(state: entry.state)
        }
        .configurationDisplayName("FlowTime")
        .description(Text("Start, pause and stop a session, with today's goal and your streak."))
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct FocusEntry: TimelineEntry {
    let date: Date
    let state: FocusWidgetState?
}

private struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> FocusEntry {
        FocusEntry(date: .now, state: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (FocusEntry) -> Void) {
        completion(FocusEntry(date: .now, state: FocusWidgetState.load()))
    }

    // Una sola entrada: el reloj lo mueve el sistema, y la app lo redibuja cuando cambia algo.
    func getTimeline(in context: Context, completion: @escaping (Timeline<FocusEntry>) -> Void) {
        completion(Timeline(entries: [FocusEntry(date: .now, state: FocusWidgetState.load())], policy: .never))
    }
}

/// Con los colores del tema de la app, como el widget de Android; el fondo, el de ese tema.
struct FocusWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme
    let state: FocusWidgetState?

    var body: some View {
        let palette = state?.palette(scheme) ?? .icon
        Group {
            if let state {
                if family == .systemMedium {
                    Medium(state: state, palette: palette)
                } else {
                    Small(state: state, palette: palette)
                }
            } else {
                // Hasta que se abre la app por primera vez.
                Ring().frame(width: 56, height: 56)
            }
        }
        .containerBackground(Color(argb: palette.background), for: .widget)
    }
}

private struct Small: View {
    let state: FocusWidgetState
    let palette: WidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(state.status)
                .font(.caption.weight(.medium))
                .foregroundStyle(Color(argb: palette.secondaryText))
                .lineLimit(1)
            Spacer(minLength: 0)
            Clock(countsDown: state.countsDown, date: state.date, time: state.time)
                .font(.system(size: 32, weight: .bold).monospacedDigit())
                .foregroundStyle(Color(argb: palette.text))
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            HStack {
                Spacer()
                ActionButton(action: state.primary, label: state.primaryLabel, size: 44, palette: palette)
            }
        }
    }
}

private struct Medium: View {
    let state: FocusWidgetState
    let palette: WidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(state.status).font(.subheadline.weight(.medium))
                Spacer()
                if let streak = state.streak {
                    Text(streak).font(.caption)
                }
            }
            .foregroundStyle(Color(argb: palette.secondaryText))
            .lineLimit(1)
            HStack(spacing: 8) {
                Clock(countsDown: state.countsDown, date: state.date, time: state.time)
                    .font(.system(size: 36, weight: .bold).monospacedDigit())
                    .foregroundStyle(Color(argb: palette.text))
                    .lineLimit(1)
                Spacer(minLength: 0)
                if let stop = state.stopLabel {
                    ActionButton(action: "STOP", label: stop, size: 44, filled: false, palette: palette)
                }
                ActionButton(action: state.primary, label: state.primaryLabel, size: 44, palette: palette)
            }
            if let goal = state.goal {
                Text(goal).font(.caption).foregroundStyle(Color(argb: palette.secondaryText))
                GeometryReader { size in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color(argb: palette.track))
                        Capsule().fill(Color(argb: palette.primary)).frame(width: size.size.width * state.goalFraction)
                    }
                }
                .frame(height: 6)
            }
        }
    }
}
