package com.baltajmn.flowtime.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baltajmn.flowtime.data.task.TaskRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.TimerAction
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Todo lo que llega con la app posiblemente cerrada: los botones de la notificación, la alarma de
 * fin de fase, el reinicio del móvil y los cambios de hora. El motor se reconstruye solo; aquí se
 * aplica la acción o se le pone al día, y se dejan listas la notificación y la siguiente alarma.
 */
class SessionReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SessionNotification.ACTION_COMPLETE_TASK) return completeTask(intent)
        val engine = get<FocusEngine>()
        val action = TimerAction.entries.firstOrNull { it.name == intent.action }
        if (action != null) engine.perform(action) else engine.sync()
        // También lo hace App al cambiar el estado, pero aquí no puede esperar: tras volver de
        // onReceive el sistema puede cerrar el proceso.
        get<SessionNotification>().update()
        get<PhaseAlarm>().schedule()
    }

    /** "Hecha" en el aviso de fin de fase (#40): la base de datos no se toca en el hilo principal. */
    private fun completeTask(intent: Intent) {
        val id = intent.getLongExtra(SessionNotification.EXTRA_TASK_ID, -1).takeIf { it > 0 } ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                get<TaskRepository>().setDone(id, done = true, today = LocalDate.now())
                val engine = get<FocusEngine>()
                if (engine.state.value.taskId == id) engine.setTask(null)
                get<SessionNotification>().run {
                    dismissAlert()
                    update()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
