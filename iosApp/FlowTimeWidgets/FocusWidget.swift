import SwiftUI
import WidgetKit

/// El widget de la pantalla de inicio, como el de Android: el reloj y empezar, pausar o seguir; el
/// mediano, además cómo va, la racha, parar y el objetivo de hoy. Lo que enseña lo decide Kotlin
/// (HomeWidget.kt) y la app pide redibujarlo con cada cambio.
struct FocusWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "FocusWidget", provider: Provider()) { entry in
            FocusWidgetView(state: entry.state)
                .containerBackground(Color.iconBackground, for: .widget)
        }
        .configurationDisplayName("FlowTime")
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

struct FocusWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let state: FocusWidgetState?

    var body: some View {
        if let state {
            if family == .systemMedium {
                Medium(state: state)
            } else {
                Small(state: state)
            }
        } else {
            // Hasta que se abre la app por primera vez.
            Ring().frame(width: 56, height: 56)
        }
    }
}

private struct Small: View {
    let state: FocusWidgetState

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(state.status)
                .font(.caption.weight(.medium))
                .foregroundStyle(Color.iconRing.opacity(0.75))
                .lineLimit(1)
            Spacer(minLength: 0)
            Clock(countsDown: state.countsDown, date: state.date, time: state.time)
                .font(.system(size: 32, weight: .bold).monospacedDigit())
                .foregroundStyle(Color.iconRing)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            HStack {
                Spacer()
                ActionButton(action: state.primary, label: state.primaryLabel, size: 44)
            }
        }
    }
}

private struct Medium: View {
    let state: FocusWidgetState

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(state.status).font(.subheadline.weight(.medium))
                Spacer()
                if let streak = state.streak {
                    Text(streak).font(.caption)
                }
            }
            .foregroundStyle(Color.iconRing.opacity(0.75))
            .lineLimit(1)
            HStack(spacing: 8) {
                Clock(countsDown: state.countsDown, date: state.date, time: state.time)
                    .font(.system(size: 36, weight: .bold).monospacedDigit())
                    .foregroundStyle(Color.iconRing)
                    .lineLimit(1)
                Spacer(minLength: 0)
                if let stop = state.stopLabel {
                    ActionButton(action: "STOP", label: stop, size: 44, filled: false)
                }
                ActionButton(action: state.primary, label: state.primaryLabel, size: 44)
            }
            if let goal = state.goal {
                Text(goal).font(.caption).foregroundStyle(Color.iconRing.opacity(0.75))
                GeometryReader { size in
                    ZStack(alignment: .leading) {
                        Capsule().fill(Color.iconTrack)
                        Capsule().fill(Color.iconWater).frame(width: size.size.width * state.goalFraction)
                    }
                }
                .frame(height: 6)
            }
        }
    }
}
