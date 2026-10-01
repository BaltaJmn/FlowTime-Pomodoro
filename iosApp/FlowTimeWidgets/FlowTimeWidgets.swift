import ActivityKit
import SwiftUI
import WidgetKit

@main
struct FlowTimeWidgets: WidgetBundle {
    var body: some Widget {
        FocusLiveActivity()
    }
}

/// La sesión en la pantalla bloqueada y en la Dynamic Island, con los colores del icono.
struct FocusLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: FocusAttributes.self) { context in
            LockScreen(state: context.state)
                .activityBackgroundTint(.iconBackground)
                .activitySystemActionForegroundColor(.iconRing)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Ring().frame(width: 36, height: 36)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Clock(state: context.state)
                        .font(.title.monospacedDigit().weight(.semibold))
                        .foregroundStyle(Color.iconWater)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Labels(state: context.state)
                        Buttons(state: context.state)
                    }
                }
            } compactLeading: {
                Ring().frame(width: 20, height: 20)
            } compactTrailing: {
                Clock(state: context.state)
                    .monospacedDigit()
                    .foregroundStyle(Color.iconWater)
                    .frame(maxWidth: 56)
            } minimal: {
                Ring().frame(width: 20, height: 20)
            }
        }
    }
}

private struct LockScreen: View {
    let state: FocusAttributes.ContentState

    var body: some View {
        HStack(spacing: 16) {
            Ring().frame(width: 44, height: 44)
            Labels(state: state)
            Spacer(minLength: 0)
            VStack(alignment: .trailing, spacing: 8) {
                Clock(state: state)
                    .font(.system(size: 36, weight: .semibold).monospacedDigit())
                    .foregroundStyle(Color.iconWater)
                    .frame(maxWidth: 140, alignment: .trailing)
                Buttons(state: state)
            }
        }
        .padding()
    }
}

/// Trabajando, el modo con la etiqueta, y la tarea si hay.
private struct Labels: View {
    let state: FocusAttributes.ContentState

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(state.title).font(.headline).foregroundStyle(Color.iconRing)
            Text(state.subtitle).font(.subheadline).foregroundStyle(Color.iconRing.opacity(0.75))
            if let task = state.task {
                Text(task).font(.subheadline).foregroundStyle(Color.iconRing.opacity(0.75)).lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

/// Lo que se puede hacer con la sesión, como en la notificación de Android. Cada botón lo hace la app
/// (FocusActionIntent), sin abrirla.
private struct Buttons: View {
    let state: FocusAttributes.ContentState

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Array(zip(state.actions, state.labels)), id: \.0) { action, label in
                Button(intent: FocusActionIntent(action: action)) {
                    Image(systemName: symbol(action))
                        .font(.system(size: 14, weight: .bold))
                        .frame(width: 32, height: 32)
                }
                .buttonStyle(.plain)
                .foregroundStyle(Color.iconBackground)
                .background(Color.iconWater, in: Circle())
                .accessibilityLabel(label)
            }
        }
    }

    private func symbol(_ action: String) -> String {
        switch action {
        case "PAUSE": "pause.fill"
        case "STOP": "stop.fill"
        case "BREAK": "cup.and.saucer.fill"
        case "SKIP_BREAK": "forward.end.fill"
        default: "play.fill"
        }
    }
}

/// Hacia atrás hasta el final de la fase (se para en 0:00), hacia delante desde que empezó, o el tiempo
/// parado en pausa. Lo mueve el sistema: la app no tiene que actualizarlo cada segundo.
private struct Clock: View {
    let state: FocusAttributes.ContentState

    var body: some View {
        if let date = state.date {
            if state.countsDown {
                Text(timerInterval: min(date, .now)...date, countsDown: true)
                    .multilineTextAlignment(.trailing)
            } else {
                Text(date, style: .timer).multilineTextAlignment(.trailing)
            }
        } else {
            Text(state.time)
        }
    }
}

/// El anillo del icono, sin la ola.
private struct Ring: View {
    var body: some View {
        ZStack {
            Circle().stroke(Color.iconTrack, lineWidth: 4)
            Circle()
                .trim(from: 0, to: 0.83)
                .stroke(Color.iconRing, style: StrokeStyle(lineWidth: 4, lineCap: .round))
                .rotationEffect(.degrees(-90))
        }
        .padding(2)
    }
}

// Los de design/icon.svg.
private extension Color {
    static let iconBackground = Color(red: 0x15 / 255, green: 0x50 / 255, blue: 0x6A / 255)
    static let iconTrack = Color(red: 0x2B / 255, green: 0x67 / 255, blue: 0x82 / 255)
    static let iconRing = Color(red: 0xEA / 255, green: 0xF6 / 255, blue: 0xFB / 255)
    static let iconWater = Color(red: 0x8E / 255, green: 0xCA / 255, blue: 0xE6 / 255)
}
