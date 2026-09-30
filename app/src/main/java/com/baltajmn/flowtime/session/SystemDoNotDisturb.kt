package com.baltajmn.flowtime.session

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import androidx.annotation.RequiresApi
import com.baltajmn.flowtime.MainActivity
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DND_RULE_ID
import com.baltajmn.flowtime.data.timer.DoNotDisturb

/**
 * Desde Android 10, una regla propia llamada "FlowTime": el sistema la combina con lo que tenga
 * puesto el usuario, así que apagarla nunca le quita su No molestar. Desde Android 15 sale en
 * Ajustes > Modos. Antes, el filtro global.
 */
class SystemDoNotDisturb(
    private val context: Context,
    private val prefs: DataProvider
) : DoNotDisturb {
    private val manager = context.getSystemService(NotificationManager::class.java)

    override val granted: Boolean get() = manager.isNotificationPolicyAccessGranted

    override val hasRules: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun setRule(active: Boolean) {
        val summary = context.getString(R.string.focus_mode_rule)
        val state = if (active) Condition.STATE_TRUE else Condition.STATE_FALSE
        // Apagarla sin haberla creado nunca: no hay nada que hacer.
        val id = ruleId(create = active) ?: return
        manager.setAutomaticZenRuleState(id, Condition(CONDITION, summary, state))
    }

    override var filter: Int
        get() = manager.currentInterruptionFilter
        set(value) = manager.setInterruptionFilter(value)

    /** La regla guardada; si el usuario la ha borrado en los ajustes del sistema, una nueva. */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ruleId(create: Boolean): String? {
        val saved = prefs.getString(DND_RULE_ID)
        if (saved != null && manager.getAutomaticZenRule(saved) != null) return saved
        if (!create) return null
        val rule = AutomaticZenRule(
            context.getString(R.string.mode_flow_time),
            null,
            ComponentName(context, MainActivity::class.java),
            CONDITION,
            null,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            true
        )
        return manager.addAutomaticZenRule(rule).also { prefs.setString(DND_RULE_ID, it) }
    }

    private companion object {
        val CONDITION: Uri = Uri.parse("condition://com.baltajmn.flowtime/focus")
    }
}
