package com.baltajmn.flowtime.data

import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DAILY_REMINDER
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.TIMER_SESSION
import com.baltajmn.flowtime.core.persistence.sharedpreferences.getObject
import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.reminder.Reminder
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import org.junit.Assert.assertEquals
import org.junit.Test

/** La 2.1.0 guardaba la sesión y el recordatorio con Gson: al actualizar, tienen que seguir ahí. */
class GsonFormatTest {

    private val prefs = FakeDataProvider()

    @Test
    fun `la sesion en curso sigue al actualizar`() {
        // Gson no escribe los campos nulos (aquí, taskId).
        prefs.setString(
            TIMER_SESSION,
            """{"mode":"FLOW_TIME","phase":"WORK","running":true,"countedMillis":1500,""" +
                """"anchorWall":1759300000000,"anchorElapsed":5000,"anchorBoot":3,"durationMillis":0,""" +
                """"workStartedAt":1759300000000,"tagId":2,"taskTitle":"Informe"}"""
        )

        assertEquals(
            FocusState(
                mode = TimerMode.FLOW_TIME,
                phase = Phase.WORK,
                running = true,
                countedMillis = 1500,
                anchorWall = 1759300000000,
                anchorElapsed = 5000,
                anchorBoot = 3,
                workStartedAt = 1759300000000,
                tagId = 2,
                taskTitle = "Informe"
            ),
            prefs.getObject<FocusState>(TIMER_SESSION)
        )
    }

    @Test
    fun `el recordatorio sigue al actualizar`() {
        prefs.setString(DAILY_REMINDER, """{"enabled":true,"minuteOfDay":540,"days":31}""")

        assertEquals(Reminder(enabled = true, minuteOfDay = 9 * 60), prefs.getObject<Reminder>(DAILY_REMINDER))
    }
}
