import ActivityKit
import SwiftUI
import WidgetKit

@main
struct FlowTimeWidgets: WidgetBundle {
    var body: some Widget {
        FocusLiveActivity()
        FocusWidget()
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
                    Clock(context.state)
                        .multilineTextAlignment(.trailing)
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
                Clock(context.state)
                    .multilineTextAlignment(.trailing)
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
                Clock(state)
                    .multilineTextAlignment(.trailing)
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
                ActionButton(action: action, label: label)
            }
        }
    }
}

/// Un botón redondo con una acción de la sesión. Lo hace la app (FocusActionIntent), sin abrirla.
struct ActionButton: View {
    let action: String
    let label: String
    var size: CGFloat = 32
    var filled = true

    var body: some View {
        Button(intent: FocusActionIntent(action: action)) {
            Image(systemName: symbol)
                .font(.system(size: size * 0.44, weight: .bold))
                .frame(width: size, height: size)
        }
        .buttonStyle(.plain)
        .foregroundStyle(filled ? Color.iconBackground : Color.iconWater)
        .background(filled ? Color.iconWater : Color.iconTrack, in: Circle())
        .accessibilityLabel(label)
    }

    private var symbol: String {
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
struct Clock: View {
    let countsDown: Bool
    let date: Date?
    let time: String

    var body: some View {
        if let date {
            if countsDown {
                Text(timerInterval: min(date, .now)...date, countsDown: true)
            } else {
                Text(date, style: .timer)
            }
        } else {
            Text(time)
        }
    }
}

extension Clock {
    init(_ state: FocusAttributes.ContentState) {
        self.init(countsDown: state.countsDown, date: state.date, time: state.time)
    }
}

/// El anillo del icono, sin la ola.
struct Ring: View {
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
extension Color {
    static let iconBackground = Color(red: 0x15 / 255, green: 0x50 / 255, blue: 0x6A / 255)
    static let iconTrack = Color(red: 0x2B / 255, green: 0x67 / 255, blue: 0x82 / 255)
    static let iconRing = Color(red: 0xEA / 255, green: 0xF6 / 255, blue: 0xFB / 255)
    static let iconWater = Color(red: 0x8E / 255, green: 0xCA / 255, blue: 0xE6 / 255)
}
