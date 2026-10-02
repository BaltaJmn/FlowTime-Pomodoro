package com.baltajmn.flowtime.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.baltajmn.flowtime.data.task.TaskRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusMode
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.reminder.DailyReminder
import com.baltajmn.flowtime.widget.FocusWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.baltajmn.flowtime.data.goal.today

/**
 * Todo lo que llega con la app posiblemente cerrada: los botones de la notificación, la alarma de
 * fin de fase, el recordatorio y los cambios de hora. Al reiniciar el móvil no hay receptor (ver el
 * manifiesto): lo pone al día App en cuanto se abre el proceso. El motor se reconstruye solo; aquí se
 * aplica la acción o se le pone al día, y se dejan listas la notificación y la siguiente alarma.
 */
class SessionReceiver :
    BroadcastReceiver(),
    KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == SessionNotification.ACTION_COMPLETE_TASK) return completeTask(intent)
        if (intent.action == DailyReminder.ACTION) return later { get<DailyReminder>().fire() }
        val engine = get<FocusEngine>()
        val action = TimerAction.entries.firstOrNull { it.name == intent.action }
        if (action != null) engine.perform(action) else engine.sync()
        // También lo hace App al cambiar el estado, pero aquí no puede esperar: tras volver de
        // onReceive el sistema puede cerrar el proceso.
        get<SessionNotification>().update()
        get<PhaseAlarm>().schedule()
        get<FocusMode>().apply(engine.state.value)
        FocusTileService.refresh(context)
        // Con otra hora o zona horaria, el recordatorio cae a otra hora: se recoloca.
        if (action == null) get<DailyReminder>().schedule()
        if (action == TimerAction.START) get<DailyReminder>().dismiss()
        later { FocusWidget.update(context) }
    }

    /** Lo que es suspend (Room, Glance) tras volver de onReceive, sin que el sistema cierre el proceso. */
    private fun later(block: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                block()
            } finally {
                pending.finish()
            }
        }
    }

    /** "Hecha" en el aviso de fin de fase (#40): la base de datos no se toca en el hilo principal. */
    private fun completeTask(intent: Intent) {
        val id = intent.getLongExtra(SessionNotification.EXTRA_TASK_ID, -1).takeIf { it > 0 } ?: return
        later {
            get<TaskRepository>().setDone(id, done = true, today = today())
            val engine = get<FocusEngine>()
            if (engine.state.value.taskId == id) engine.setTask(null)
            get<SessionNotification>().run {
                dismissAlert()
                update()
            }
        }
    }
}
